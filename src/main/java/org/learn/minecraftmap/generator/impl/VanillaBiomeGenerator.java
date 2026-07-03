package org.learn.minecraftmap.generator.impl;

import com.sun.jna.Pointer;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.learn.minecraftmap.domain.BiomeColorMap;
import org.learn.minecraftmap.domain.BiomeInfo;
import org.learn.minecraftmap.generator.BiomeGenerator;
import org.learn.minecraftmap.jna.CubiomesNative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.learn.minecraftmap.generator.CustomDatapackManager;
import org.springframework.stereotype.Component;


@Component
public class VanillaBiomeGenerator implements BiomeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(VanillaBiomeGenerator.class);

    private final GenericObjectPool<Pointer> generatorPool;
    private final CustomDatapackManager datapackManager;

    public VanillaBiomeGenerator(GenericObjectPool<Pointer> generatorPool, CustomDatapackManager datapackManager) {
        this.generatorPool = generatorPool;
        this.datapackManager = datapackManager;
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

    @Override
    public BiomeInfo getBiome(long seed, String mcVersion, int dimension, int x, int z) {
        logger.debug("Sampling single biome: seed={}, version={}, dimension={}, x={}, z={}", seed, mcVersion, dimension, x, z);
        int versionCode = mapVersion(mcVersion);
        Pointer g = null;
        try {
            g = generatorPool.borrowObject();
            CubiomesNative.setupGenerator(g, versionCode, 0);
            if (datapackManager.getCustomTree() != null) {
                CubiomesNative.setGeneratorCustomTree(g, datapackManager.getCustomTree());
            }
            CubiomesNative.applySeed(g, dimension, seed);
            
            // Cubiomes: getBiomeAt(g, scale, x, y, z)
            int biomeId = CubiomesNative.getBiomeAt(g, 1, x, 64, z);
            String name = BiomeColorMap.getBiomeName(biomeId);
            String color = BiomeColorMap.getHexColor(biomeId);
            return new BiomeInfo(biomeId, name, color);
        } catch (Exception e) {
            logger.error("Failed to sample single biome", e);
            throw new RuntimeException("Failed to sample single biome", e);
        } finally {
            if (g != null) {
                generatorPool.returnObject(g);
            }
        }
    }

    @Override
    public BiomeInfo[][] getBiomeTile(long seed, String mcVersion, int dimension, int tx, int ty, int tileSize) {
        long start = System.currentTimeMillis();
        logger.info("Generating biome tile grid (fallback): seed={}, version={}, dimension={}, tx={}, ty={}", seed, mcVersion, dimension, tx, ty);
        
        BiomeInfo[][] tile = new BiomeInfo[tileSize][tileSize];
        int versionCode = mapVersion(mcVersion);

        // Run row generation in parallel across ForkJoin worker threads
        java.util.stream.IntStream.range(0, tileSize).parallel().forEach(pz -> {
            Pointer g = null;
            try {
                g = generatorPool.borrowObject();
                CubiomesNative.setupGenerator(g, versionCode, 0);
                if (datapackManager.getCustomTree() != null) {
                    CubiomesNative.setGeneratorCustomTree(g, datapackManager.getCustomTree());
                }
                CubiomesNative.applySeed(g, dimension, seed);

                int blockZ = ty * tileSize + pz;
                for (int px = 0; px < tileSize; px++) {
                    int blockX = tx * tileSize + px;
                    int biomeId = CubiomesNative.getBiomeAt(g, 4, blockX >> 2, 16, blockZ >> 2);
                    String name = BiomeColorMap.getBiomeName(biomeId);
                    String color = BiomeColorMap.getHexColor(biomeId);
                    tile[pz][px] = new BiomeInfo(biomeId, name, color);
                }
            } catch (Exception e) {
                logger.error("Failed to query biome tile row at pz=" + pz, e);
            } finally {
                if (g != null) {
                    generatorPool.returnObject(g);
                }
            }
        });
        
        logger.info("Generated biome tile grid in {} ms", System.currentTimeMillis() - start);
        return tile;
    }
}
