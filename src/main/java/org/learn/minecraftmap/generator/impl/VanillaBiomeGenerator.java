package org.learn.minecraftmap.generator.impl;

import com.sun.jna.Pointer;
import org.learn.minecraftmap.domain.BiomeColorMap;
import org.learn.minecraftmap.domain.BiomeInfo;
import org.learn.minecraftmap.generator.BiomeGenerator;
import org.learn.minecraftmap.jna.CubiomesLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class VanillaBiomeGenerator implements BiomeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(VanillaBiomeGenerator.class);

    // Thread-local cache of native generators to prevent concurrent access (since cubiomes Generator is thread-unsafe)
    private final ThreadLocal<Map<VersionedSeed, Pointer>> threadLocalCache = ThreadLocal.withInitial(HashMap::new);

    private Pointer getGenerator(long seed, String mcVersion, int dimension) {
        VersionedSeed key = new VersionedSeed(seed, mcVersion, dimension);
        Map<VersionedSeed, Pointer> cache = threadLocalCache.get();
        Pointer g = cache.get(key);
        if (g != null) {
            return g;
        }

        long start = System.currentTimeMillis();
        // Allocate 27592 bytes for the C Generator struct
        Pointer newG = new com.sun.jna.Memory(27592);
        int versionCode = mapVersion(mcVersion);
        CubiomesLibrary.INSTANCE.setupGenerator(newG, versionCode, 0);
        CubiomesLibrary.INSTANCE.applySeed(newG, dimension, seed);
        
        logger.info("Initialized native generator for thread={} in {} ms (seed={}, dimension={})", 
            Thread.currentThread().getName(), System.currentTimeMillis() - start, seed, dimension);
        
        cache.put(key, newG);
        return newG;
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
    public BiomeInfo getBiome(long seed, String mcVersion, int dimension, int x, int z) {
        logger.debug("Sampling single biome: seed={}, version={}, dimension={}, x={}, z={}", seed, mcVersion, dimension, x, z);
        Pointer g = getGenerator(seed, mcVersion, dimension);
        // Cubiomes: getBiomeAt(g, scale, x, y, z)
        // scale = 1 for 1:1 block coordinates (this automatically handles the 1:4 Voronoi zoom resolution internally)
        int biomeId = CubiomesLibrary.INSTANCE.getBiomeAt(g, 1, x, 64, z);
        
        String name = BiomeColorMap.getBiomeName(biomeId);
        String color = BiomeColorMap.getHexColor(biomeId);
        logger.debug("Sampled biome at ({}, {}): id={}, name={}", x, z, biomeId, name);
        return new BiomeInfo(biomeId, name, color);
    }

    @Override
    public BiomeInfo[][] getBiomeTile(long seed, String mcVersion, int dimension, int zoom, int tx, int ty, int tileSize) {
        long start = System.currentTimeMillis();
        logger.info("Generating biome tile grid: seed={}, version={}, dimension={}, zoom={}, tx={}, ty={}", seed, mcVersion, dimension, zoom, tx, ty);
        
        BiomeInfo[][] tile = new BiomeInfo[tileSize][tileSize];
        // Calculate the coordinate scale mapping based on the zoom factor.
        double scale = Math.pow(2.0, 8.0 - zoom);

        // Run row generation in parallel across ForkJoin worker threads
        java.util.stream.IntStream.range(0, tileSize).parallel().forEach(pz -> {
            Pointer g = getGenerator(seed, mcVersion, dimension);
            for (int px = 0; px < tileSize; px++) {
                int blockX = (int) Math.round((tx * tileSize + px) * scale);
                int blockZ = (int) Math.round((ty * tileSize + pz) * scale);
                
                // Query at biome scale (4) to bypass expensive Voronoi scaling.
                int biomeId = CubiomesLibrary.INSTANCE.getBiomeAt(g, 4, blockX >> 2, 16, blockZ >> 2);
                String name = BiomeColorMap.getBiomeName(biomeId);
                String color = BiomeColorMap.getHexColor(biomeId);
                tile[pz][px] = new BiomeInfo(biomeId, name, color);
            }
        });
        
        logger.info("Generated biome tile grid in {} ms", System.currentTimeMillis() - start);
        return tile;
    }

    // Composite key representation for the cache
    private static final class VersionedSeed {
        private final long seed;
        private final String version;
        private final int dimension;

        public VersionedSeed(long seed, String version, int dimension) {
            this.seed = seed;
            this.version = version;
            this.dimension = dimension;
        }

        public long seed() {
            return seed;
        }

        public String version() {
            return version;
        }

        public int dimension() {
            return dimension;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VersionedSeed that = (VersionedSeed) o;
            return seed == that.seed && dimension == that.dimension && Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(seed, version, dimension);
        }
    }
}
