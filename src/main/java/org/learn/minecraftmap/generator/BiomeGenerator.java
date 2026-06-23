package org.learn.minecraftmap.generator;

import org.learn.minecraftmap.domain.BiomeInfo;

public interface BiomeGenerator {
    /**
     * Samples a biome at specific block coordinates (x, z) for a given seed and Minecraft version.
     *
     * @param seed the world seed
     * @param mcVersion the Minecraft version string (e.g. "1.20", "26.2")
     * @param dimension the dimension ID (e.g. 0 for Overworld, -1 for Nether, 1 for End)
     * @param x block coordinate x
     * @param z block coordinate z
     * @return BiomeInfo detailing the biome at the coordinate
     */
    BiomeInfo getBiome(long seed, String mcVersion, int dimension, int x, int z);

    /**
     * Generates a 2D grid of BiomeInfo for a specific map tile and Minecraft version.
     *
     * @param seed the world seed
     * @param mcVersion the Minecraft version string (e.g. "1.20", "26.2")
     * @param dimension the dimension ID (e.g. 0 for Overworld, -1 for Nether, 1 for End)
     * @param tx the tile coordinate x
     * @param ty the tile coordinate y
     * @param tileSize the size of the tile (typically 256)
     * @return 2D array of BiomeInfo representing the tile's pixels
     */
    BiomeInfo[][] getBiomeTile(long seed, String mcVersion, int dimension, int tx, int ty, int tileSize);
}
