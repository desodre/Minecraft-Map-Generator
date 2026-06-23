package org.learn.minecraftmap.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sun.jna.Pointer;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.learn.minecraftmap.domain.BiomeColorMap;
import org.learn.minecraftmap.domain.BiomeInfo;
import org.learn.minecraftmap.generator.BiomeGenerator;
import org.learn.minecraftmap.jna.CubiomesNative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class BiomeMapService {

    private static final Logger logger = LoggerFactory.getLogger(BiomeMapService.class);

    private final BiomeGenerator biomeGenerator;
    private final GenericObjectPool<Pointer> generatorPool;

    // Caffeine Cache: Stores processed PNG tile bytes with 5 minutes expiration
    private final Cache<TileKey, byte[]> tileCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(2000) // Cache up to 2000 tiles
            .build();

    // Lock striping map to prevent concurrent threads from generating the same Metatile simultaneously
    private final ConcurrentHashMap<MetaTileKey, Object> metaTileLocks = new ConcurrentHashMap<>();

    public BiomeMapService(BiomeGenerator biomeGenerator, GenericObjectPool<Pointer> generatorPool) {
        this.biomeGenerator = biomeGenerator;
        this.generatorPool = generatorPool;
    }

    /**
     * Retrieves the biome details at a specific coordinate and version.
     */
    public BiomeInfo getBiome(long seed, String mcVersion, int dimension, int x, int z) {
        return biomeGenerator.getBiome(seed, mcVersion, dimension, x, z);
    }

    /**
     * Generates or fetches from cache a 256x256 PNG map tile image.
     */
    public byte[] generateTileImage(long seed, String mcVersion, int dimension, int tx, int ty) throws IOException {
        TileKey key = new TileKey(seed, mcVersion, dimension, tx, ty);
        
        // 1. Check Caffeine Cache
        byte[] cachedBytes = tileCache.getIfPresent(key);
        if (cachedBytes != null) {
            logger.debug("Tile cache HIT for tx={}, ty={}", tx, ty);
            return cachedBytes;
        }

        // 2. Metatiling Math (aligning grid to 4x4 Metatiles)
        int metaTx = Math.floorDiv(tx, 4) * 4;
        int metaTy = Math.floorDiv(ty, 4) * 4;
        MetaTileKey metaKey = new MetaTileKey(seed, mcVersion, dimension, metaTx, metaTy);

        // 3. Lock-striped metatile generation
        Object lock = metaTileLocks.computeIfAbsent(metaKey, k -> new Object());
        synchronized (lock) {
            // Double-check cache inside synchronized block
            cachedBytes = tileCache.getIfPresent(key);
            if (cachedBytes != null) {
                return cachedBytes;
            }

            // Generate all 16 tiles inside the 4x4 Metatile (1024x1024 blocks)
            generateMetaTile(seed, mcVersion, dimension, metaTx, metaTy);
        }
        metaTileLocks.remove(metaKey, lock);

        byte[] finalBytes = tileCache.getIfPresent(key);
        if (finalBytes == null) {
            throw new IOException("Failed to generate tile image for tx=" + tx + ", ty=" + ty);
        }
        return finalBytes;
    }

    /**
     * Generates a 4x4 metatile grid (1024x1024 blocks matrix) using pooled pointers and JNA Direct Mapping,
     * slices it into 16 images of 256x256 blocks, encodes them to PNG, and caches them.
     */
    private void generateMetaTile(long seed, String mcVersion, int dimension, int metaTx, int metaTy) throws IOException {
        long start = System.currentTimeMillis();
        logger.info("Generating metatile at metaTx={}, metaTy={} (seed={}, version={}, dimension={})", 
                metaTx, metaTy, seed, mcVersion, dimension);

        int queryGridSize = 256; // 1024 blocks / 4 = 256 biome queries at scale 4
        int[][] biomes = new int[queryGridSize][queryGridSize];
        int versionCode = mapVersion(mcVersion);

        // Query JNA Direct Mapping inside ForkJoin Pool threads
        java.util.stream.IntStream.range(0, queryGridSize).parallel().forEach(gy -> {
            Pointer g = null;
            try {
                g = generatorPool.borrowObject();
                CubiomesNative.setupGenerator(g, versionCode, 0);
                CubiomesNative.applySeed(g, dimension, seed);

                int blockZ_scale4 = (metaTy * 64) + gy;
                for (int gx = 0; gx < queryGridSize; gx++) {
                    int blockX_scale4 = (metaTx * 64) + gx;
                    biomes[gy][gx] = CubiomesNative.getBiomeAt(g, 4, blockX_scale4, 16, blockZ_scale4);
                }
            } catch (Exception e) {
                logger.error("Failed to query metatile row at gy=" + gy, e);
            } finally {
                if (g != null) {
                    generatorPool.returnObject(g);
                }
            }
        });

        long gridTime = System.currentTimeMillis();

        // Slice the metatile matrix into 16 buffered images, encode as PNG, and cache
        for (int dy = 0; dy < 4; dy++) {
            for (int dx = 0; dx < 4; dx++) {
                int tileTx = metaTx + dx;
                int tileTy = metaTy + dy;

                BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);

                for (int py = 0; py < 256; py++) {
                    int gy = (dy * 64) + (py >> 2);
                    for (int px = 0; px < 256; px++) {
                        int gx = (dx * 64) + (px >> 2);
                        int biomeId = biomes[gy][gx];
                        Color color = BiomeColorMap.getColor(biomeId);
                        image.setRGB(px, py, color.getRGB());
                    }
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                byte[] pngBytes = baos.toByteArray();

                TileKey tileKey = new TileKey(seed, mcVersion, dimension, tileTx, tileTy);
                tileCache.put(tileKey, pngBytes);
            }
        }

        logger.info("Metatile generated and cached in {} ms (native C time={} ms, slice & encode time={} ms)",
                System.currentTimeMillis() - start,
                gridTime - start,
                System.currentTimeMillis() - gridTime);
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

    // Composite keys for caches
    private static record TileKey(long seed, String version, int dimension, int tx, int ty) {}
    private static record MetaTileKey(long seed, String version, int dimension, int metaTx, int metaTy) {}
}
