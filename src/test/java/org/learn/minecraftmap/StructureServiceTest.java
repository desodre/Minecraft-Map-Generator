package org.learn.minecraftmap;

import com.sun.jna.Pointer;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.learn.minecraftmap.domain.StructureInfo;
import org.learn.minecraftmap.generator.pool.CubiomesGeneratorFactory;
import org.learn.minecraftmap.service.StructureService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StructureServiceTest {

    private static GenericObjectPool<Pointer> pool;
    private static StructureService service;

    @BeforeAll
    public static void setup() {
        CubiomesGeneratorFactory factory = new CubiomesGeneratorFactory();
        GenericObjectPoolConfig<Pointer> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(2);
        pool = new GenericObjectPool<>(factory, config);
        service = new StructureService(pool);
    }

    @AfterAll
    public static void teardown() {
        if (pool != null) {
            pool.close();
        }
    }

    @Test
    public void testGetGridStructures() {
        long seed = 12345L;
        // Bounding box containing known viable village coordinates found in previous Python verification:
        // Viable village at region (-10, 2), block: (-5408, 1408) for seed 12345, version 1.21
        int minX = -5500;
        int maxX = -5300;
        int minZ = 1300;
        int maxZ = 1500;

        List<StructureInfo> structures = service.getStructures(
                seed, "1.21", 0, minX, minZ, maxX, maxZ, List.of("village")
        );

        assertNotNull(structures);
        assertFalse(structures.isEmpty(), "Should find a viable village in this area");
        
        StructureInfo village = structures.get(0);
        assertEquals("village", village.getType());
        assertEquals(-5408, village.getX());
        assertEquals(1408, village.getZ());
        assertNotNull(village.getBiome());
        
        System.out.println("Test found village: type=" + village.getType() + 
                ", biome=" + village.getBiome() + ", pos=(" + village.getX() + ", " + village.getZ() + ")");
    }

    @Test
    public void testGetStrongholds() {
        long seed = 12345L;
        // Stronghold 1 exact coordinates from Python: (-1676, 1988)
        int minX = -1700;
        int maxX = -1650;
        int minZ = 1950;
        int maxZ = 2050;

        List<StructureInfo> structures = service.getStructures(
                seed, "1.21", 0, minX, minZ, maxX, maxZ, List.of("stronghold")
        );

        assertNotNull(structures);
        assertFalse(structures.isEmpty(), "Should find Stronghold 1 in this area");

        StructureInfo stronghold = structures.get(0);
        assertEquals("stronghold", stronghold.getType());
        assertEquals(-1676, stronghold.getX());
        assertEquals(1988, stronghold.getZ());
        assertNotNull(stronghold.getBiome());

        System.out.println("Test found stronghold: type=" + stronghold.getType() + 
                ", biome=" + stronghold.getBiome() + ", pos=(" + stronghold.getX() + ", " + stronghold.getZ() + ")");
    }

    @Test
    public void testStrongholdPruningPerformance() {
        long seed = 12345L;
        // Search far away from spawn (distance > 26000), should skip stronghold search instantly
        int minX = 30000;
        int maxX = 31000;
        int minZ = 30000;
        int maxZ = 31000;

        long start = System.currentTimeMillis();
        List<StructureInfo> structures = service.getStructures(
                seed, "1.21", 0, minX, minZ, maxX, maxZ, List.of("stronghold")
        );
        long duration = System.currentTimeMillis() - start;

        assertNotNull(structures);
        assertTrue(structures.isEmpty(), "No strongholds should exist past 26k radius");
        assertTrue(duration < 10, "Search should be near-instantaneous (pruned)");
        
        System.out.println("Search past 26k blocks pruned in " + duration + " ms");
    }
}
