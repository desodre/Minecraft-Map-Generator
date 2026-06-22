package org.learn.minecraftmap.domain;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class BiomeColorMap {
    private static final Map<Integer, Color> ID_TO_COLOR = new HashMap<>();
    private static final Map<String, Color> NAME_TO_COLOR = new HashMap<>();
    private static final Map<Integer, String> ID_TO_NAME = new HashMap<>();
    private static final Color DEFAULT_COLOR = new Color(141, 179, 96); // Plains green default

    static {
        // Oceans
        register(0, "ocean", new Color(0, 0, 112));
        register(24, "deep_ocean", new Color(0, 0, 48));
        register(44, "warm_ocean", new Color(0, 0, 160));
        register(45, "lukewarm_ocean", new Color(0, 127, 255));
        register(46, "cold_ocean", new Color(32, 32, 112));
        register(47, "deep_warm_ocean", new Color(0, 0, 96));
        register(48, "deep_lukewarm_ocean", new Color(0, 64, 128));
        register(49, "deep_cold_ocean", new Color(32, 32, 96));
        register(50, "deep_frozen_ocean", new Color(32, 32, 64));
        register(10, "frozen_ocean", new Color(112, 112, 214));

        // Plains & Meadows
        register(1, "plains", new Color(141, 179, 96));
        register(129, "sunflower_plains", new Color(181, 219, 136));
        register(185, "meadow", new Color(44, 140, 68));

        // Deserts & Beaches
        register(2, "desert", new Color(250, 226, 162));
        register(130, "desert_lakes", new Color(255, 235, 180));
        register(16, "beach", new Color(250, 222, 85));
        register(25, "stony_shore", new Color(162, 162, 162));
        register(26, "snowy_beach", new Color(250, 240, 192));

        // Forests
        register(4, "forest", new Color(5, 102, 33));
        register(18, "forest_hills", new Color(5, 102, 33));
        register(132, "flower_forest", new Color(45, 142, 73));
        register(27, "birch_forest", new Color(48, 116, 68));
        register(28, "birch_forest_hills", new Color(48, 116, 68));
        register(155, "old_growth_birch_forest", new Color(30, 90, 50));
        register(29, "dark_forest", new Color(64, 81, 26));
        register(157, "dark_forest_hills", new Color(64, 81, 26));
        register(191, "cherry_grove", new Color(255, 181, 216));

        // Taigas & Snowy Plains
        register(5, "taiga", new Color(11, 77, 44));
        register(19, "taiga_hills", new Color(11, 77, 44));
        register(133, "taiga_mountains", new Color(11, 77, 44));
        register(30, "snowy_taiga", new Color(49, 85, 74));
        register(31, "snowy_taiga_hills", new Color(49, 85, 74));
        register(158, "snowy_taiga_mountains", new Color(49, 85, 74));
        register(32, "old_growth_pine_taiga", new Color(89, 102, 81));
        register(33, "old_growth_spruce_taiga", new Color(45, 93, 48));
        register(12, "snowy_plains", new Color(255, 255, 255));
        register(140, "ice_spikes", new Color(180, 220, 255));

        // Swamps
        register(6, "swamp", new Color(7, 249, 178));
        register(134, "swamp_hills", new Color(7, 249, 178));
        register(192, "mangrove_swamp", new Color(56, 58, 21));

        // Rivers
        register(7, "river", new Color(0, 0, 255));
        register(11, "frozen_river", new Color(160, 160, 255));

        // Mountains & Peaks
        register(3, "windswept_hills", new Color(96, 96, 96));
        register(34, "wooded_mountains", new Color(96, 96, 96));
        register(131, "windswept_gravelly_hills", new Color(136, 136, 136));
        register(162, "modified_gravelly_mountains", new Color(136, 136, 136));
        register(186, "grove", new Color(91, 135, 114));
        register(187, "snowy_slopes", new Color(242, 242, 242));
        register(188, "jagged_peaks", new Color(255, 255, 255));
        register(189, "frozen_peaks", new Color(240, 240, 255));
        register(190, "stony_peaks", new Color(140, 140, 140));

        // Jungles
        register(21, "jungle", new Color(34, 182, 0));
        register(22, "jungle_hills", new Color(34, 182, 0));
        register(23, "sparse_jungle", new Color(98, 182, 0));
        register(149, "modified_jungle", new Color(34, 182, 0));
        register(151, "modified_jungle_edge", new Color(98, 182, 0));
        register(168, "bamboo_jungle", new Color(118, 142, 20));
        register(169, "bamboo_jungle_hills", new Color(118, 142, 20));

        // Savanna
        register(35, "savanna", new Color(189, 177, 90));
        register(36, "savanna_plateau", new Color(167, 157, 82));
        register(163, "shattered_savanna", new Color(229, 217, 130));
        register(164, "shattered_savanna_plateau", new Color(207, 197, 122));

        // Badlands
        register(37, "badlands", new Color(217, 69, 21));
        register(38, "wooded_badlands", new Color(176, 151, 101));
        register(39, "badlands_plateau", new Color(176, 101, 101));
        register(165, "eroded_badlands", new Color(255, 109, 61));
        register(166, "modified_badlands_plateau", new Color(206, 131, 131));

        // Caves
        register(182, "dripstone_caves", new Color(66, 55, 44));
        register(183, "lush_caves", new Color(58, 89, 45));
        register(184, "deep_dark", new Color(3, 22, 28));

        // Nether
        register(8, "nether_wastes", new Color(139, 34, 34));
        register(178, "soul_sand_valley", new Color(94, 77, 65));
        register(179, "crimson_forest", new Color(152, 26, 26));
        register(180, "warped_forest", new Color(26, 112, 104));
        register(181, "basalt_deltas", new Color(66, 61, 61));

        // End
        register(9, "the_end", new Color(56, 56, 24));
    }

    private static void register(int id, String name, Color color) {
        ID_TO_COLOR.put(id, color);
        NAME_TO_COLOR.put(name.toLowerCase(), color);
        NAME_TO_COLOR.put("minecraft:" + name.toLowerCase(), color);
        ID_TO_NAME.put(id, name);
    }

    public static String getBiomeName(int id) {
        return ID_TO_NAME.getOrDefault(id, "unknown");
    }

    public static Color getColor(int id) {
        return ID_TO_COLOR.getOrDefault(id, DEFAULT_COLOR);
    }

    public static Color getColor(String name) {
        if (name == null) return DEFAULT_COLOR;
        return NAME_TO_COLOR.getOrDefault(name.toLowerCase(), DEFAULT_COLOR);
    }

    public static String getHexColor(int id) {
        Color c = getColor(id);
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static String getHexColor(String name) {
        Color c = getColor(name);
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
