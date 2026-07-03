package org.learn.minecraftmap;

import com.sun.jna.Pointer;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Test;
import org.learn.minecraftmap.domain.BiomeInfo;
import org.learn.minecraftmap.generator.impl.VanillaBiomeGenerator;
import org.learn.minecraftmap.generator.pool.CubiomesGeneratorFactory;
import org.learn.minecraftmap.generator.CustomDatapackManager;
import org.learn.minecraftmap.service.BiomeMapService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class BiomeMapServiceTest {

    private static final GenericObjectPool<Pointer> pool = createTestPool();
    private static final CustomDatapackManager vanillaDatapackManager = createVanillaDatapackManager();
    private final VanillaBiomeGenerator generator = new VanillaBiomeGenerator(pool, vanillaDatapackManager);
    private final BiomeMapService service = new BiomeMapService(generator, pool);

    private static CustomDatapackManager createVanillaDatapackManager() {
        CustomDatapackManager manager = new CustomDatapackManager();
        manager.init();
        return manager;
    }

    private static GenericObjectPool<Pointer> createTestPool() {
        CubiomesGeneratorFactory factory = new CubiomesGeneratorFactory();
        GenericObjectPoolConfig<Pointer> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(2);
        return new GenericObjectPool<>(factory, config);
    }

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
        int tx = 0;
        int ty = 0;

        // Verify tile image works for version 26.2 (dimension 0 = Overworld)
        byte[] pngBytes = service.generateTileImage(seed, "26.2", 0, tx, ty);
        assertNotNull(pngBytes);
        assertTrue(pngBytes.length > 0);

        // Check PNG signature: first 4 bytes must be 0x89, 'P', 'N', 'G'
        assertEquals((byte) 0x89, pngBytes[0]);
        assertEquals((byte) 'P', pngBytes[1]);
        assertEquals((byte) 'N', pngBytes[2]);
        assertEquals((byte) 'G', pngBytes[3]);
    }

    @Test
    public void testNative1to1ScalingMath() {
        // Test that 1:1 scale is used (1 pixel = 1 block).
        long seed = 11111L;
        
        // Tile tx 0, ty 0. Pixel px 0, pz 0 is block x 0, z 0.
        // Pixel px 10, pz 20 is block x 10, z 20.
        BiomeInfo[][] grid = generator.getBiomeTile(seed, "1.20", 0, 0, 0, 256);
        
        BiomeInfo singleAt0_0 = generator.getBiome(seed, "1.20", 0, 0, 0);
        assertEquals(singleAt0_0.getId(), grid[0][0].getId());

        BiomeInfo singleAt10_20 = generator.getBiome(seed, "1.20", 0, 10, 20);
        assertEquals(singleAt10_20.getId(), grid[20][10].getId()); // pz is row index (y/z), px is col index (x)
    }

    @Test
    public void testGetCustomBiomeFromDatapack() {
        CustomDatapackManager datapackManager = new CustomDatapackManager();
        datapackManager.setDatapackPath("/home/desodre/Projects/cubiomes/test_overworld.json");
        datapackManager.init();

        try {
            assertNotNull(datapackManager.getCustomTree(), "Custom tree should be loaded");

            // Create a generator specifically configured with this custom tree
            VanillaBiomeGenerator customGenerator = new VanillaBiomeGenerator(pool, datapackManager);
            
            // Validate JNA function getCustomBiomeIdByName directly
            int customId = org.learn.minecraftmap.jna.CubiomesNative.getCustomBiomeIdByName("biomesoplenty:lavender_fields");
            assertTrue(customId >= 200, "Should have registered lavender fields dynamically with ID >= 200");

            // Validate BiomeColorMap integration
            String customName = org.learn.minecraftmap.domain.BiomeColorMap.getBiomeName(customId);
            assertEquals("biomesoplenty:lavender_fields", customName);

            String customColor = org.learn.minecraftmap.domain.BiomeColorMap.getHexColor(customId);
            assertNotNull(customColor);
            assertNotEquals("#8db360", customColor, "Should have generated a custom color hash instead of default plains green");

            System.out.println("Custom Biome Registered: ID=" + customId + ", Name=" + customName + ", Color=" + customColor);

            // Test sampling with seed that generates this custom biome via Kd-Tree
            // In test_overworld.json:
            // biomesoplenty:lavender_fields -> temp=[0.3, 0.7], hum=[0.5, 0.9], cont=0.0
            // When custom tree is active, sampling should successfully output customIds or vanilla IDs matching nearest neighbors
            BiomeInfo sampled = customGenerator.getBiome(12345L, "1.20", 0, 0, 0);
            assertNotNull(sampled);
            System.out.println("Sampled biome with custom datapack active at (0,0): " + sampled.getName() + " (ID: " + sampled.getId() + ")");
        } finally {
            datapackManager.cleanup();
        }
    }
}
