package org.learn.minecraftmap.jna;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA Direct Mapping interface to libcubiomes.
 * Direct mapping provides significantly higher performance than interface proxying
 * by bypassing JNA reflection overhead on native method calls.
 */
public class CubiomesNative {
    static {
        Native.register("cubiomes");
    }

    @Structure.FieldOrder({"x", "z"})
    public static class Pos extends Structure {
        public int x;
        public int z;

        public static class ByValue extends Pos implements Structure.ByValue {}
    }

    @Structure.FieldOrder({"salt", "regionSize", "chunkRange", "structType", "dim", "rarity"})
    public static class StructureConfig extends Structure {
        public int salt;
        public byte regionSize;
        public byte chunkRange;
        public byte structType;
        public byte dim;
        public float rarity;
    }

    @Structure.FieldOrder({
        "pos", "nextapprox", "index", "ringnum", "ringmax", "ringidx", "angle", "dist", "rnds", "mc"
    })
    public static class StrongholdIter extends Structure {
        public Pos pos = new Pos();
        public Pos nextapprox = new Pos();
        public int index;
        public int ringnum;
        public int ringmax;
        public int ringidx;
        public double angle;
        public double dist;
        public long rnds;
        public int mc;
    }

    /**
     * Initializes the Generator structure for a specific Minecraft version.
     * void setupGenerator(Generator *g, int mc, uint32_t flags);
     */
    public static native void setupGenerator(Pointer g, int mc, int flags);

    /**
     * Configures the generator for a specific dimension and world seed.
     * void applySeed(Generator *g, int dim, uint64_t seed);
     */
    public static native void applySeed(Pointer g, int dim, long seed);

    /**
     * Retrieves the biome ID at a given coordinate.
     * int getBiomeAt(const Generator *g, int scale, int x, int y, int z);
     * scale can be 1 (block coordinates) or 4 (biome coordinates).
     */
    public static native int getBiomeAt(Pointer g, int scale, int x, int y, int z);

    /**
     * Retrieves version-specific structure configuration.
     * int getStructureConfig(int structureType, int mc, StructureConfig *sconf);
     */
    public static native int getStructureConfig(int structureType, int mc, StructureConfig sconf);

    /**
     * Calculates the potential coordinates where a structure may generate in a region.
     * int getStructurePos(int structureType, int mc, uint64_t seed, int regX, int regZ, Pos *pos);
     */
    public static native int getStructurePos(int structureType, int mc, long seed, int regX, int regZ, Pos pos);

    /**
     * Checks if a structure can generate at the given position.
     * int isViableStructurePos(int structureType, const Generator *g, int x, int z, int checkBiomes);
     */
    public static native int isViableStructurePos(int structureType, Pointer g, int x, int z, int checkBiomes);

    /**
     * Initializes stronghold iteration ring-based system.
     * Pos initFirstStronghold(StrongholdIter *sh, int mc, uint64_t seed);
     */
    public static native Pos.ByValue initFirstStronghold(StrongholdIter sh, int mc, long seed);

    /**
     * Iterates to the next stronghold.
     * int nextStronghold(StrongholdIter *si, const Generator *g);
     */
    public static native int nextStronghold(StrongholdIter si, Pointer g);

    // Minecraft Version Constants from biomes.h (enum MCVersion)
    public static final int MC_1_18 = 22;
    public static final int MC_1_19 = 24;
    public static final int MC_1_20 = 25;
    public static final int MC_1_21 = 28;
    public static final int MC_NEWEST = 28;

    // Dimension Constants from biomes.h (enum Dimension)
    public static final int DIM_NETHER = -1;
    public static final int DIM_OVERWORLD = 0;
    public static final int DIM_END = 1;
}
