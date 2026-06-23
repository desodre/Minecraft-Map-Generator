package org.learn.minecraftmap;

import org.junit.jupiter.api.Test;
import org.learn.minecraftmap.domain.BiomeInfo;
import org.learn.minecraftmap.generator.impl.VanillaBiomeGenerator;
import org.learn.minecraftmap.service.BiomeMapService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class BiomeMapServiceTest {

    private final VanillaBiomeGenerator generator = new VanillaBiomeGenerator();
    private final BiomeMapService service = new BiomeMapService(generator);

    @Test
    public void testGetBiome() {
        long seed = 123456L;
        // Sample biome at a specific coordinate for 1.20 (dimension 0 = Overworld)
        BiomeInfo biome = service.getBiome(seed, "1.20", 0, 0, 0);
        assertNotNull(biome);
        assertNotNull(biome.getName());
        assertNotEquals("unknown", biome.getName());
        assertTrue(biome.getId() >= 0);
        assertNotNull(biome.getHexColor());
        assertTrue(biome.getHexColor().startsWith("#"));
        
        System.out.println("Sample biome at (0,0) for seed " + seed + " in 1.20 is: " + biome.getName() + " (ID: " + biome.getId() + ", Color: " + biome.getHexColor() + ")");
    }

    @Test
    public void testGetBiomeForNewVersion26_2() {
        long seed = 123456L;
        // Sample biome at a specific coordinate for 26.2 (mapped to 1.21, dimension 0 = Overworld)
        BiomeInfo biome = service.getBiome(seed, "26.2", 0, 0, 0);
        assertNotNull(biome);
        assertNotNull(biome.getName());
        assertNotEquals("unknown", biome.getName());
        assertTrue(biome.getId() >= 0);
        assertNotNull(biome.getHexColor());
        
        System.out.println("Sample biome at (0,0) for seed " + seed + " in 26.2 is: " + biome.getName() + " (ID: " + biome.getId() + ", Color: " + biome.getHexColor() + ")");
    }

    @Test
    public void testGenerateTileImage() throws IOException {
        long seed = 987654321L;
        int zoom = 8;
        int tx = 0;
        int ty = 0;

        // Verify tile image works for version 26.2 (dimension 0 = Overworld)
        byte[] pngBytes = service.generateTileImage(seed, "26.2", 0, zoom, tx, ty);
        assertNotNull(pngBytes);
        assertTrue(pngBytes.length > 0);

        // Check PNG signature: first 4 bytes must be 0x89, 'P', 'N', 'G'
        assertEquals((byte) 0x89, pngBytes[0]);
        assertEquals((byte) 'P', pngBytes[1]);
        assertEquals((byte) 'N', pngBytes[2]);
        assertEquals((byte) 'G', pngBytes[3]);
    }

    @Test
    public void testZoomScalingMath() {
        // Test that zooming in/out scales coordinates correctly
        long seed = 11111L;
        
        // Zoom 8 is 1:1 scale. So tile tx 0, ty 0, pixel px 0, pz 0 is block x 0, z 0
        BiomeInfo[][] gridZoom8 = generator.getBiomeTile(seed, "1.20", 0, 8, 0, 0, 256);
        BiomeInfo singleZoom8 = generator.getBiome(seed, "1.20", 0, 0, 0);
        assertEquals(singleZoom8.getId(), gridZoom8[0][0].getId());

        // Zoom 7 is 1:2 scale. So pixel px 1, pz 1 in tile tx 0, ty 0 is block x 2, z 2
        BiomeInfo[][] gridZoom7 = generator.getBiomeTile(seed, "1.20", 0, 7, 0, 0, 256);
        BiomeInfo singleZoom7_pixel1 = generator.getBiome(seed, "1.20", 0, 2, 2);
        assertEquals(singleZoom7_pixel1.getId(), gridZoom7[1][1].getId());
    }
}
