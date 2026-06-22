package org.learn.minecraftmap.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface CubiomesLibrary extends Library {
    // JNA will load libcubiomes.so automatically from resources (linux-x86-64/)
    CubiomesLibrary INSTANCE = Native.load("cubiomes", CubiomesLibrary.class);

    // C Signatures from generator.h
    
    /**
     * Initializes the Generator structure for a specific Minecraft version.
     * void setupGenerator(Generator *g, int mc, uint32_t flags);
     */
    void setupGenerator(Pointer g, int mc, int flags);

    /**
     * Configures the generator for a specific dimension and world seed.
     * void applySeed(Generator *g, int dim, uint64_t seed);
     */
    void applySeed(Pointer g, int dim, long seed);

    /**
     * Retrieves the biome ID at a given coordinate.
     * int getBiomeAt(const Generator *g, int scale, int x, int y, int z);
     * scale can be 1 (block coordinates) or 4 (biome coordinates).
     */
    int getBiomeAt(Pointer g, int scale, int x, int y, int z);

    // Minecraft Version Constants from biomes.h (enum MCVersion)
    int MC_1_18 = 22;
    int MC_1_19 = 24;
    int MC_1_20 = 25;
    int MC_1_21 = 28;
    int MC_NEWEST = 28;

    // Dimension Constants from biomes.h (enum Dimension)
    int DIM_OVERWORLD = 0;
}
