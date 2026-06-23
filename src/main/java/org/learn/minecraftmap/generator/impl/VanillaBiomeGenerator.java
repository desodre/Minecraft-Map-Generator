package org.learn.minecraftmap.generator.impl;

import com.sun.jna.Pointer;
import org.learn.minecraftmap.domain.BiomeColorMap;
import org.learn.minecraftmap.domain.BiomeInfo;
import org.learn.minecraftmap.generator.BiomeGenerator;
import org.learn.minecraftmap.jna.CubiomesLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class VanillaBiomeGenerator implements BiomeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(VanillaBiomeGenerator.class);

    // Cache of Generator pointers per VersionedSeed
    private final ConcurrentMap<VersionedSeed, Pointer> generatorCache = new ConcurrentHashMap<>();

    private Pointer getGenerator(long seed, String mcVersion) {
        VersionedSeed key = new VersionedSeed(seed, mcVersion);
        Pointer g = generatorCache.get(key);
        if (g != null) {
            logger.debug("Cache hit for Cubiomes generator: seed={}, version={}", seed, mcVersion);
            return g;
        }

        logger.info("Cache miss. Initializing new native Cubiomes generator for seed={}, version={}", seed, mcVersion);
        return generatorCache.computeIfAbsent(key, k -> {
            long start = System.currentTimeMillis();
            // Allocate 27592 bytes for the C Generator struct
            Pointer newG = new com.sun.jna.Memory(27592);
            int versionCode = mapVersion(k.version());
            CubiomesLibrary.INSTANCE.setupGenerator(newG, versionCode, 0);
            CubiomesLibrary.INSTANCE.applySeed(newG, CubiomesLibrary.DIM_OVERWORLD, k.seed());
            logger.info("Initialized native generator in {} ms for seed={}", System.currentTimeMillis() - start, k.seed());
            return newG;
        });
    }

    private int mapVersion(String versionStr) {
        if (versionStr == null) {
            return CubiomesLibrary.MC_1_20;
        }
        return switch (versionStr.trim()) {
            case "26.2", "26.1", "1.21" -> CubiomesLibrary.MC_1_21;
            case "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6" -> CubiomesLibrary.MC_1_20;
            default -> CubiomesLibrary.MC_1_20;
        };
    }

    @Override
    public BiomeInfo getBiome(long seed, String mcVersion, int x, int z) {
        logger.debug("Sampling single biome: seed={}, version={}, x={}, z={}", seed, mcVersion, x, z);
        Pointer g = getGenerator(seed, mcVersion);
        // Cubiomes: getBiomeAt(g, scale, x, y, z)
        // scale = 1 for 1:1 block coordinates (this automatically handles the 1:4 Voronoi zoom resolution internally)
        int biomeId = CubiomesLibrary.INSTANCE.getBiomeAt(g, 1, x, 64, z);
        
        String name = BiomeColorMap.getBiomeName(biomeId);
        String color = BiomeColorMap.getHexColor(biomeId);
        logger.debug("Sampled biome at ({}, {}): id={}, name={}", x, z, biomeId, name);
        return new BiomeInfo(biomeId, name, color);
    }

    @Override
    public BiomeInfo[][] getBiomeTile(long seed, String mcVersion, int zoom, int tx, int ty, int tileSize) {
        long start = System.currentTimeMillis();
        logger.info("Generating biome tile grid: seed={}, version={}, zoom={}, tx={}, ty={}", seed, mcVersion, zoom, tx, ty);
        
        BiomeInfo[][] tile = new BiomeInfo[tileSize][tileSize];
        Pointer g = getGenerator(seed, mcVersion);
        
        // Calculate the coordinate scale mapping based on the zoom factor.
        // At zoom 8, each pixel corresponds to exactly 1 block (1:1).
        // For zoom levels smaller than 8, each pixel represents more blocks (zoomed out).
        // For zoom levels greater than 8, each pixel represents a sub-block region (zoomed in).
        double scale = Math.pow(2.0, 8.0 - zoom);

        for (int pz = 0; pz < tileSize; pz++) {
            for (int px = 0; px < tileSize; px++) {
                int blockX = (int) Math.round((tx * tileSize + px) * scale);
                int blockZ = (int) Math.round((ty * tileSize + pz) * scale);
                
                int biomeId = CubiomesLibrary.INSTANCE.getBiomeAt(g, 1, blockX, 64, blockZ);
                String name = BiomeColorMap.getBiomeName(biomeId);
                String color = BiomeColorMap.getHexColor(biomeId);
                tile[pz][px] = new BiomeInfo(biomeId, name, color);
            }
        }
        
        logger.info("Generated biome tile grid in {} ms", System.currentTimeMillis() - start);
        return tile;
    }

    // Composite key representation for the cache
    private static final class VersionedSeed {
        private final long seed;
        private final String version;

        public VersionedSeed(long seed, String version) {
            this.seed = seed;
            this.version = version;
        }

        public long seed() {
            return seed;
        }

        public String version() {
            return version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VersionedSeed that = (VersionedSeed) o;
            return seed == that.seed && Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(seed, version);
        }
    }
}
