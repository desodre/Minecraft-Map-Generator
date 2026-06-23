package org.learn.minecraftmap.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.learn.minecraftmap.domain.StructureInfo;
import org.learn.minecraftmap.service.StructureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
@Tag(name = "Minecraft Structures Finder", description = "Endpoints for finding points of interest (structures) within bounding boxes.")
public class StructureController {

    private final StructureService structureService;

    public StructureController(StructureService structureService) {
        this.structureService = structureService;
    }

    @GetMapping("/structures")
    @Operation(
        summary = "Find structures in Bounding Box",
        description = "Returns a list of viable structures of the specified types within the specified coordinate boundaries (minX, minZ, maxX, maxZ)."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved structures."
    )
    public ResponseEntity<List<StructureInfo>> getStructures(
        @Parameter(description = "The Minecraft world seed.", required = true, example = "12345")
        @RequestParam long seed,

        @Parameter(description = "The Minecraft version to check (e.g. 1.20, 1.21). Defaults to 1.20.", required = false, example = "1.21")
        @RequestParam(required = false, defaultValue = "1.20") String version,

        @Parameter(description = "The Minecraft dimension ID (0 for Overworld, -1 for Nether, 1 for End).", required = false, example = "0")
        @RequestParam(required = false, defaultValue = "0") int dimension,

        @Parameter(description = "Minimum X block coordinate of the bounding box.", required = true, example = "-1000")
        @RequestParam int minX,

        @Parameter(description = "Minimum Z block coordinate of the bounding box.", required = true, example = "-1000")
        @RequestParam int minZ,

        @Parameter(description = "Maximum X block coordinate of the bounding box.", required = true, example = "1000")
        @RequestParam int maxX,

        @Parameter(description = "Maximum Z block coordinate of the bounding box.", required = true, example = "1000")
        @RequestParam int maxZ,

        @Parameter(description = "List of structure types to search for (e.g. village, monument, stronghold, fortress, bastion, end_city).", required = true)
        @RequestParam List<String> types
    ) {
        List<StructureInfo> structures = structureService.getStructures(
                seed, version, dimension, minX, minZ, maxX, maxZ, types
        );
        return ResponseEntity.ok(structures);
    }
}
