package org.learn.minecraftmap.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.learn.minecraftmap.domain.BiomeInfo;
import org.learn.minecraftmap.service.BiomeMapService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
@Tag(name = "Minecraft Map Generator", description = "Endpoints for sampling biomes and generating map tiles from world seeds.")
public class BiomeMapController {

    private final BiomeMapService biomeMapService;

    public BiomeMapController(BiomeMapService biomeMapService) {
        this.biomeMapService = biomeMapService;
    }

    /**
     * Samples the biome at a specific pixel/coordinate.
     */
    @GetMapping("/biome")
    @Operation(
        summary = "Sample single-pixel biome",
        description = "Returns the biome ID, name, and hex color code at the exact block coordinate (x, z) for the specified seed and version."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Successfully sampled the biome.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BiomeInfo.class))
    )
    public ResponseEntity<BiomeInfo> getBiome(
        @Parameter(description = "The Minecraft world seed.", required = true, example = "12345")
        @RequestParam long seed,

        @Parameter(description = "The Minecraft version to generate (e.g. 1.20, 1.20.2, 1.20.6, 1.21, 26.1, 26.2). Defaults to 1.20.", required = false, example = "26.2")
        @RequestParam(required = false, defaultValue = "1.20") String version,

        @Parameter(description = "The Minecraft dimension ID (0 for Overworld, -1 for Nether, 1 for End).", required = false, example = "0")
        @RequestParam(required = false, defaultValue = "0") int dimension,
        
        @Parameter(description = "The block coordinate X.", required = true, example = "100")
        @RequestParam int x,
        
        @Parameter(description = "The block coordinate Z.", required = true, example = "100")
        @RequestParam int z
    ) {
        BiomeInfo biomeInfo = biomeMapService.getBiome(seed, version, dimension, x, z);
        return ResponseEntity.ok(biomeInfo);
    }

    /**
     * Renders a 256x256 map tile in PNG format.
     */
    @GetMapping("/map/tile")
    @Operation(
        summary = "Render map tile (PNG)",
        description = "Generates a 256x256 pixel PNG image representing a map tile of biomes for the given seed, version, zoom level, and tile coordinates (tx, ty)."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Successfully generated the PNG tile.",
        content = @Content(mediaType = "image/png")
    )
    @ApiResponse(
        responseCode = "500",
        description = "Failed to render or write the PNG image."
    )
    public ResponseEntity<byte[]> getMapTile(
        @Parameter(description = "The Minecraft world seed.", required = true, example = "12345")
        @RequestParam long seed,

        @Parameter(description = "The Minecraft version to generate (e.g. 1.20, 1.20.2, 1.20.6, 1.21, 26.1, 26.2). Defaults to 1.20.", required = false, example = "26.2")
        @RequestParam(required = false, defaultValue = "1.20") String version,

        @Parameter(description = "The Minecraft dimension ID (0 for Overworld, -1 for Nether, 1 for End).", required = false, example = "0")
        @RequestParam(required = false, defaultValue = "0") int dimension,
        
        @Parameter(description = "The map zoom level (deprecated, ignored).", required = false)
        @RequestParam(required = false) Integer zoom,
        
        @Parameter(description = "The horizontal tile coordinate.", required = true, example = "0")
        @RequestParam int tx,
        
        @Parameter(description = "The vertical tile coordinate.", required = true, example = "0")
        @RequestParam int ty
    ) {
        try {
            byte[] imageBytes = biomeMapService.generateTileImage(seed, version, dimension, tx, ty);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            // Disable caching to ensure browser fetches fresh Cubiomes tiles during testing
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
