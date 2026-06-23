package org.learn.minecraftmap.service;

import com.sun.jna.Pointer;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.learn.minecraftmap.domain.BiomeColorMap;
import org.learn.minecraftmap.domain.StructureInfo;
import org.learn.minecraftmap.jna.CubiomesNative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StructureService {

    private static final Logger logger = LoggerFactory.getLogger(StructureService.class);

    private final GenericObjectPool<Pointer> generatorPool;

    // Mapping of client structure type names to Cubiomes structure ID
    private static final Map<String, Integer> STRUCTURE_NAME_TO_ID = Map.of(
            "village", 5,
            "monument", 8,
            "fortress", 18,
            "bastion", 19,
            "end_city", 20
    );

    public StructureService(GenericObjectPool<Pointer> generatorPool) {
        this.generatorPool = generatorPool;
    }

    /**
     * Finds viable structures of requested types within a Minecraft bounding box.
     */
    public List<StructureInfo> getStructures(
            long seed,
            String mcVersion,
            int dimension,
            int minX,
            int minZ,
            int maxX,
            int maxZ,
            List<String> types
    ) {
        long startTime = System.currentTimeMillis();
        List<StructureInfo> results = new ArrayList<>();
        int versionCode = mapVersion(mcVersion);

        Pointer g = null;
        try {
            g = generatorPool.borrowObject();
            CubiomesNative.setupGenerator(g, versionCode, 0);
            CubiomesNative.applySeed(g, dimension, seed);

            // 1. Process Grid-Based Structures
            for (String typeName : types) {
                String normalizedType = typeName.trim().toLowerCase();
                Integer structureId = STRUCTURE_NAME_TO_ID.get(normalizedType);
                if (structureId == null) {
                    continue; // Skip unknown or stronghold type (handled separately)
                }

                // Check version-specific configuration
                CubiomesNative.StructureConfig sconf = new CubiomesNative.StructureConfig();
                if (CubiomesNative.getStructureConfig(structureId, versionCode, sconf) == 0) {
                    logger.debug("Structure {} not supported in Minecraft version {}", normalizedType, mcVersion);
                    continue;
                }

                // Verify dimension compatibility
                if (sconf.dim != dimension) {
                    logger.debug("Structure {} dimension mismatch (expected {}, got {})", normalizedType, sconf.dim, dimension);
                    continue;
                }

                int regionSizeBlocks = sconf.regionSize * 16;
                int minRegX = Math.floorDiv(minX, regionSizeBlocks);
                int maxRegX = Math.floorDiv(maxX, regionSizeBlocks);
                int minRegZ = Math.floorDiv(minZ, regionSizeBlocks);
                int maxRegZ = Math.floorDiv(maxZ, regionSizeBlocks);

                for (int rx = minRegX; rx <= maxRegX; rx++) {
                    for (int rz = minRegZ; rz <= maxRegZ; rz++) {
                        CubiomesNative.Pos pos = new CubiomesNative.Pos();
                        if (CubiomesNative.getStructurePos(structureId, versionCode, seed, rx, rz, pos) != 0) {
                            if (pos.x >= minX && pos.x <= maxX && pos.z >= minZ && pos.z <= maxZ) {
                                if (CubiomesNative.isViableStructurePos(structureId, g, pos.x, pos.z, 1) != 0) {
                                    int biomeId = CubiomesNative.getBiomeAt(g, 1, pos.x, 64, pos.z);
                                    String biomeName = BiomeColorMap.getBiomeName(biomeId);
                                    results.add(new StructureInfo(normalizedType, biomeName, pos.x, pos.z));
                                }
                            }
                        }
                    }
                }
            }

            // 2. Process Strongholds (Only generates in Overworld, dimension 0)
            if (dimension == 0 && types.stream().anyMatch(t -> t.trim().equalsIgnoreCase("stronghold"))) {
                int maxBoxDist = Math.max(
                        Math.max(Math.abs(minX), Math.abs(maxX)),
                        Math.max(Math.abs(minZ), Math.abs(maxZ))
                );

                double minBoxDist = getMinDistanceToOrigin(minX, minZ, maxX, maxZ);

                // Strongholds do not generate past radius 26000 in Java edition
                if (minBoxDist <= 26000) {
                    CubiomesNative.StrongholdIter sh = new CubiomesNative.StrongholdIter();
                    CubiomesNative.initFirstStronghold(sh, versionCode, seed);

                    for (int i = 0; i < 128; i++) {
                        // Strongholds are generated in rings at increasing distances.
                        // sh.dist represents the next ring's distance from the origin in chunks.
                        double approxDistBlocks = sh.dist * 16.0;
                        if (approxDistBlocks > maxBoxDist + 1000) {
                            logger.debug("Pruning stronghold search at iteration {} (approxDist={} blocks > maxBoxDist+1000={})",
                                    i, approxDistBlocks, maxBoxDist + 1000);
                            break;
                        }

                        if (CubiomesNative.nextStronghold(sh, g) <= 0) {
                            break;
                        }

                        if (sh.pos.x >= minX && sh.pos.x <= maxX && sh.pos.z >= minZ && sh.pos.z <= maxZ) {
                            int biomeId = CubiomesNative.getBiomeAt(g, 1, sh.pos.x, 64, sh.pos.z);
                            String biomeName = BiomeColorMap.getBiomeName(biomeId);
                            results.add(new StructureInfo("stronghold", biomeName, sh.pos.x, sh.pos.z));
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Failed to query structures", e);
            throw new RuntimeException("Failed to query structures", e);
        } finally {
            if (g != null) {
                generatorPool.returnObject(g);
            }
        }

        logger.info("Found {} structures in Bounding Box [{}, {}] to [{}, {}] in {} ms",
                results.size(), minX, minZ, maxX, maxZ, System.currentTimeMillis() - startTime);

        return results;
    }

    private double getMinDistanceToOrigin(double minX, double minZ, double maxX, double maxZ) {
        double dx = 0;
        if (maxX < 0) dx = -maxX;
        else if (minX > 0) dx = minX;

        double dz = 0;
        if (maxZ < 0) dz = -maxZ;
        else if (minZ > 0) dz = minZ;

        return Math.sqrt(dx * dx + dz * dz);
    }

    private int mapVersion(String versionStr) {
        if (versionStr == null) {
            return CubiomesNative.MC_1_20;
        }
        return switch (versionStr.trim()) {
            case "26.2", "26.1", "1.21" -> CubiomesNative.MC_1_21;
            case "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6" -> CubiomesNative.MC_1_20;
            default -> CubiomesNative.MC_1_20;
        };
    }
}
