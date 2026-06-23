package org.learn.minecraftmap.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Information about a Minecraft structure, including its type, biome, and block coordinates.")
public class StructureInfo {

    @Schema(description = "The type of structure (e.g. village, monument, stronghold).", example = "village")
    private final String type;

    @Schema(description = "The biome name at the structure position.", example = "plains")
    private final String biome;

    @Schema(description = "The block coordinate X.", example = "150")
    private final int x;

    @Schema(description = "The block coordinate Z.", example = "-200")
    private final int z;

    public StructureInfo(String type, String biome, int x, int z) {
        this.type = type;
        this.biome = biome;
        this.x = x;
        this.z = z;
    }

    public String getType() {
        return type;
    }

    public String getBiome() {
        return biome;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }
}
