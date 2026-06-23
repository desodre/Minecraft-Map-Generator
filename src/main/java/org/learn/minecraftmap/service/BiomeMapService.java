package org.learn.minecraftmap.service;

import org.learn.minecraftmap.domain.BiomeColorMap;
import org.learn.minecraftmap.domain.BiomeInfo;
import org.learn.minecraftmap.generator.BiomeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class BiomeMapService {

    private static final Logger logger = LoggerFactory.getLogger(BiomeMapService.class);

    private final BiomeGenerator biomeGenerator;

    public BiomeMapService(BiomeGenerator biomeGenerator) {
        this.biomeGenerator = biomeGenerator;
    }

    /**
     * Retrieves the biome details at a specific coordinate and version.
     *
     * @param seed the world seed
     * @param mcVersion the Minecraft version string (e.g. "1.20", "26.2")
     * @param x block coordinate x
     * @param z block coordinate z
     * @return BiomeInfo DTO
     */
    public BiomeInfo getBiome(long seed, String mcVersion, int x, int z) {
        return biomeGenerator.getBiome(seed, mcVersion, x, z);
    }

    /**
     * Generates a 256x256 PNG map tile image bytes for the given tile parameters.
     *
     * @param seed the world seed
     * @param mcVersion the Minecraft version string (e.g. "1.20", "26.2")
     * @param zoom the zoom level
     * @param tx the tile coordinate x
     * @param ty the tile coordinate y
     * @return PNG image bytes
     * @throws IOException if image rendering fails
     */
    public byte[] generateTileImage(long seed, String mcVersion, int zoom, int tx, int ty) throws IOException {
        long start = System.currentTimeMillis();
        logger.info("Starting PNG tile rendering for seed={}, version={}, zoom={}, tx={}, ty={}", seed, mcVersion, zoom, tx, ty);

        int tileSize = 256;
        BiomeInfo[][] biomeGrid = biomeGenerator.getBiomeTile(seed, mcVersion, zoom, tx, ty, tileSize);

        long gridTime = System.currentTimeMillis();
        BufferedImage image = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_RGB);

        for (int z = 0; z < tileSize; z++) {
            for (int x = 0; x < tileSize; x++) {
                BiomeInfo biomeInfo = biomeGrid[z][x];
                Color color = BiomeColorMap.getColor(biomeInfo.getId());
                image.setRGB(x, z, color.getRGB());
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        logger.info("PNG tile rendered: size={} bytes, total time={} ms (grid gen={} ms, draw & encode={} ms)",
                imageBytes.length,
                System.currentTimeMillis() - start,
                gridTime - start,
                System.currentTimeMillis() - gridTime);

        return imageBytes;
    }
}
