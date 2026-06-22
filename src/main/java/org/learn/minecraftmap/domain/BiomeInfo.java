package org.learn.minecraftmap.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Information about a Minecraft biome, including its ID, name, and color representation.")
public class BiomeInfo {
    
    @Schema(description = "The unique numeric ID of the Minecraft biome.", example = "1")
    private final int id;
    
    @Schema(description = "The registry name of the biome.", example = "plains")
    private final String name;
    
    @Schema(description = "The HEX color code representing this biome on the map.", example = "#8db360")
    private final String hexColor;

    public BiomeInfo(int id, String name, String hexColor) {
        this.id = id;
        this.name = name;
        this.hexColor = hexColor;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHexColor() {
        return hexColor;
    }
}
