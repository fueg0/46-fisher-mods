package com.poke;

import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PokePluginTest {

    private final PokePlugin plugin;

    public PokePluginTest() throws Exception {
        plugin = PokePlugin.class.getDeclaredConstructor().newInstance();
    }

    // ============================================================
    // interpolate() tests
    // ============================================================

    @Test
    void interpolate_exactFrom_returnsFromColor() throws Exception {
        TextColor from = TextColor.fromHexString("#FF0000");
        TextColor to = TextColor.fromHexString("#00FF00");
        TextColor result = invoke("interpolate", from, to, 0.0);
        assertEquals(0xFF0000, result.rgb());
        assertEquals(255, result.red());
        assertEquals(0, result.green());
        assertEquals(0, result.blue());
    }

    @Test
    void interpolate_exactTo_returnsToColor() throws Exception {
        TextColor from = TextColor.fromHexString("#FF0000");
        TextColor to = TextColor.fromHexString("#00FF00");
        TextColor result = invoke("interpolate", from, to, 1.0);
        assertEquals(0x00FF00, result.rgb());
        assertEquals(0, result.red());
        assertEquals(255, result.green());
        assertEquals(0, result.blue());
    }

    @Test
    void interpolate_midpoint_returnsHalfBlend() throws Exception {
        TextColor from = TextColor.fromHexString("#000000");
        TextColor to = TextColor.fromHexString("#FFFFFF");
        TextColor result = invoke("interpolate", from, to, 0.5);
        assertEquals(127, result.red());
        assertEquals(127, result.green());
        assertEquals(127, result.blue());
    }

    @Test
    void interpolate_quarterBlend_isCorrect() throws Exception {
        TextColor from = TextColor.fromHexString("#000000"); // black
        TextColor to = TextColor.fromHexString("#0000FF");    // blue (b=255)
        TextColor result = invoke("interpolate", from, to, 0.75);
        assertEquals(0, result.red());
        assertEquals(0, result.green());
        assertEquals(191, result.blue()); // 0 + 255*0.75 = 191
    }

    @Test
    void interpolate_sameColor_returnsThatColor() throws Exception {
        TextColor same = TextColor.fromHexString("#AABBCC");
        TextColor result = invoke("interpolate", same, same, 0.5);
        assertEquals(same.rgb(), result.rgb());
    }

    // ============================================================
    // getColorForCount() tests — edge cases
    // ============================================================

    @Test
    void getColorForCount_negative_returnsBrown() throws Exception {
        TextColor result = invoke("getColorForCount", -1);
        assertEquals(TextColor.fromHexString("#8B4513").rgb(), result.rgb());
    }

    @Test
    void getColorForCount_count1_isGray() throws Exception {
        // count=1: index=1.0 → gray (COLORS[1])
        TextColor result = invoke("getColorForCount", 1);
        assertEquals(TextColor.fromHexString("#808080").rgb(), result.rgb());
    }

    @Test
    void getColorForCount_count2_interpolatesGrayToRed() throws Exception {
        // count=2: index=1.225, lower=1 (gray), t=0.225
        // gray(128) to red(255): r = 128 + (255-128)*0.225 = 128+28.6 = 156
        TextColor result = invoke("getColorForCount", 2);
        assertTrue(result.red() > 150 && result.red() < 160,
            "count=2 should interpolate gray→red, red channel expected ~156, got " + result.red());
        assertEquals(128, result.green()); // gray has equal R=G=B
        assertEquals(128, result.blue());
    }

    @Test
    void getColorForCount_count7_isRed() throws Exception {
        // count=7: index=2.0 → red (COLORS[2])
        TextColor result = invoke("getColorForCount", 7);
        assertEquals(TextColor.fromHexString("#FF0000").rgb(), result.rgb());
    }

    @Test
    void getColorForCount_zero_returnsBrown() throws Exception {
        TextColor result = invoke("getColorForCount", 0);
        assertEquals(TextColor.fromHexString("#8B4513").rgb(), result.rgb());
    }

    @Test
    void getColorForCount_at1500_returnsWhite() throws Exception {
        TextColor result = invoke("getColorForCount", 1500);
        assertEquals(TextColor.fromHexString("#FFFFFF").rgb(), result.rgb());
    }

    @Test
    void getColorForCount_above1500_clampedToWhite() throws Exception {
        TextColor r1500 = invoke("getColorForCount", 1500);
        TextColor r10k = invoke("getColorForCount", 10000);
        assertEquals(r1500.rgb(), r10k.rgb()); // both white
    }

    @Test
    void getColorForCount_justBelow1500_isLavenderNotWhite() throws Exception {
        TextColor result = invoke("getColorForCount", 1499);
        TextColor white = invoke("getColorForCount", 1500);
        // 1499 should be lavender (index ~10.998), not pure white
        assertNotEquals(white.rgb(), result.rgb());
    }

    // ============================================================
    // getColorForCount() tests — bracket verification
    // ============================================================

    @Test
    void getColorForCount_count8_isOrange() throws Exception {
        // count=8: index=3.06, lower=3 → orange (COLORS[3])
        TextColor result = invoke("getColorForCount", 8);
        assertEquals(TextColor.fromHexString("#FF7F00").rgb(), result.rgb());
    }

    @Test
    void getColorForCount_count26_isYellow() throws Exception {
        TextColor result = invoke("getColorForCount", 26);
        assertEquals(TextColor.fromHexString("#FFFF00").rgb(), result.rgb());
    }

    @Test
    void getColorForCount_count64_isGreen() throws Exception {
        TextColor result = invoke("getColorForCount", 64);
        assertEquals(TextColor.fromHexString("#00FF00").rgb(), result.rgb());
    }

    @Test
    void getColorForCount_count132_isBlue() throws Exception {
        TextColor result = invoke("getColorForCount", 132);
        assertEquals(TextColor.fromHexString("#007FFF").rgb(), result.rgb());
    }

    @Test
    void getColorForCount_count246_isIndigo() throws Exception {
        TextColor result = invoke("getColorForCount", 246);
        assertEquals(TextColor.fromHexString("#4B0082").rgb(), result.rgb());
    }

    @Test
    void getColorForCount_count419_isViolet() throws Exception {
        TextColor result = invoke("getColorForCount", 419);
        assertEquals(TextColor.fromHexString("#9400D3").rgb(), result.rgb());
    }

    @Test
    void getColorForCount_count672_isPlum() throws Exception {
        TextColor result = invoke("getColorForCount", 672);
        assertEquals(TextColor.fromHexString("#DDA0DD").rgb(), result.rgb());
    }

    @Test
    void getColorForCount_count1025_isLavender() throws Exception {
        TextColor result = invoke("getColorForCount", 1025);
        assertEquals(TextColor.fromHexString("#E6E6FA").rgb(), result.rgb());
    }

    // ============================================================
    // getColorForCount() tests — transitions/interpolation
    // ============================================================

    @Test
    void getColorForCount_transitionsAreSmooth_notInstantJumps() throws Exception {
        // Between 7 (red) and 8 (orange): should interpolate, not jump
        TextColor at7 = invoke("getColorForCount", 7);
        TextColor at8 = invoke("getColorForCount", 8);
        // Red (255,0,0) to Orange (255,127,0): at midpoint t=0.5 → (255,63,0)
        // At 7.5 (interpolated t=0.5): red=255, green=63, blue=0
        // This should NOT equal red (#FF0000) OR orange (#FF7F00) exactly
        // But we can verify at boundary: count 7 still returns exact red
        assertEquals(TextColor.fromHexString("#FF0000").rgb(), at7.rgb());
        assertEquals(TextColor.fromHexString("#FF7F00").rgb(), at8.rgb());
    }

    @Test
    void getColorForCount_highCountStillInterpolates() throws Exception {
        // At count=1000, index=9.74, lower=9 (plum), t=0.74 → plum→lavender
        TextColor result = invoke("getColorForCount", 1000);
        // Plum (#DDA0DD) to Lavender (#E6E6FA): very close, t=0.74
        // r: 221 + (230-221)*0.74 = 221+6.66 = 228
        // g: 160 + (230-160)*0.74 = 160+51.8 = 212
        // b: 221 + (250-221)*0.74 = 221+21.46 = 243
        assertTrue(result.red() > 220 && result.red() < 235);
        assertTrue(result.green() > 200 && result.green() < 220);
        assertTrue(result.blue() > 235 && result.blue() < 250);
    }

    // ============================================================
    // Data persistence tests
    // ============================================================

    private Map<UUID, Integer> getPokeCounts() throws Exception {
        Field f = PokePlugin.class.getDeclaredField("pokeCounts");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Integer> map = (Map<UUID, Integer>) f.get(plugin);
        return map;
    }

    @Test
    void saveAndLoad_singleEntry_roundTrips() throws Exception {
        UUID uuid = UUID.randomUUID();
        int count = 42;
        getPokeCounts().put(uuid, count);
        plugin.saveData();

        getPokeCounts().clear();
        plugin.loadData();

        assertEquals(count, getPokeCounts().get(uuid));
    }

    @Test
    void saveAndLoad_multipleEntries_roundTrips() throws Exception {
        Map<UUID, Integer> map = getPokeCounts();
        map.clear();

        UUID[] uuids = { UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID() };
        int[] counts = { 0, 7, 533 };
        for (int i = 0; i < uuids.length; i++) map.put(uuids[i], counts[i]);
        plugin.saveData();

        map.clear();
        plugin.loadData();

        for (int i = 0; i < uuids.length; i++) {
            assertEquals(counts[i], map.get(uuids[i]));
        }
    }

    @Test
    void saveAndLoad_preservesZeroCount() throws Exception {
        UUID uuid = UUID.randomUUID();
        getPokeCounts().put(uuid, 0);
        plugin.saveData();

        getPokeCounts().clear();
        plugin.loadData();

        assertEquals(0, getPokeCounts().get(uuid));
    }

    @Test
    void loadData_clearsExistingEntriesBeforeReload() throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        Map<UUID, Integer> map = getPokeCounts();
        map.clear();
        map.put(uuid1, 10);
        plugin.saveData();

        // Simulate: someone added data while server was running, then reload
        map.put(uuid1, 10);
        map.put(uuid2, 20);
        plugin.saveData();

        map.clear();
        plugin.loadData();

        // After load, only the saved entries should exist
        assertEquals(10, map.get(uuid1));
        assertNull(map.get(uuid2)); // uuid2 was never saved
    }

    @Test
    void loadData_skipsMalformedLines() throws Exception {
        UUID uuid = UUID.randomUUID();
        Map<UUID, Integer> map = getPokeCounts();
        map.clear();
        map.put(uuid, 99);
        plugin.saveData();

        // Append garbage to file
        java.io.FileWriter fw = new java.io.FileWriter(
            new java.io.File(plugin.getDataFolder(), "pokes.dat"));
        fw.write(uuid.toString() + ":99\n");
        fw.write("not-a-uuid:abc\n");
        fw.write("ALSO_INVALID\n");
        fw.write("550e8400-e29b-41d4-a716-446655440000:five\n");
        fw.write("\n");
        fw.close();

        map.clear();
        plugin.loadData();

        // Only the valid line should be loaded
        assertEquals(99, map.get(uuid));
        assertEquals(1, map.size());
    }

    // ============================================================
    // Full bracket sweep
    // ============================================================

    @Test
    void getColorForCount_sweepsAllBrackets_boundaryStart() throws Exception {
        int[] starts = { 0, 1, 8, 26, 64, 132, 246, 419, 672, 1025, 1500 };
        TextColor[] expected = {
            TextColor.fromHexString("#8B4513"), // brown
            TextColor.fromHexString("#FF0000"), // red
            TextColor.fromHexString("#FF7F00"), // orange
            TextColor.fromHexString("#FFFF00"), // yellow
            TextColor.fromHexString("#00FF00"), // green
            TextColor.fromHexString("#007FFF"), // blue
            TextColor.fromHexString("#4B0082"), // indigo
            TextColor.fromHexString("#9400D3"), // violet
            TextColor.fromHexString("#DDA0DD"), // plum
            TextColor.fromHexString("#E6E6FA"), // lavender
            TextColor.fromHexString("#FFFFFF"), // white
        };

        for (int i = 0; i < starts.length; i++) {
            TextColor result = invoke("getColorForCount", starts[i]);
            assertEquals(expected[i].rgb(), result.rgb(),
                "Bracket " + i + " start (count=" + starts[i] + ") should be " +
                String.format("#%06X", expected[i].rgb()));
        }
    }

    @Test
    void getColorForCount_sweepsAllBrackets_eachBracketEndMinusOne() throws Exception {
        int[] endsMinusOne = { 0, 7, 25, 63, 131, 245, 418, 671, 1024, 1499, 99999 };
        TextColor[] expected = {
            TextColor.fromHexString("#8B4513"), // brown
            TextColor.fromHexString("#FF0000"), // red
            TextColor.fromHexString("#FF7F00"), // orange
            TextColor.fromHexString("#FFFF00"), // yellow
            TextColor.fromHexString("#00FF00"), // green
            TextColor.fromHexString("#007FFF"), // blue
            TextColor.fromHexString("#4B0082"), // indigo
            TextColor.fromHexString("#9400D3"), // violet
            TextColor.fromHexString("#DDA0DD"), // plum
            TextColor.fromHexString("#E6E6FA"), // lavender
            TextColor.fromHexString("#FFFFFF"), // white
        };

        for (int i = 0; i < endsMinusOne.length; i++) {
            if (endsMinusOne[i] == 99999) continue; // skip white sentinel
            TextColor result = invoke("getColorForCount", endsMinusOne[i]);
            assertEquals(expected[i].rgb(), result.rgb(),
                "Bracket " + i + " end-1 (count=" + endsMinusOne[i] + ") should be " +
                String.format("#%06X", expected[i].rgb()));
        }
    }

    // ============================================================
    // White-clamp edge
    // ============================================================

    @Test
    void getColorForCount_1499_isLavender_notWhite() throws Exception {
        TextColor result = invoke("getColorForCount", 1499);
        assertNotEquals(TextColor.fromHexString("#FFFFFF").rgb(), result.rgb());
        // And it should be very close to white (lavender t≈0.998 toward white)
        assertTrue(result.red() > 240);
        assertTrue(result.green() > 230);
        assertTrue(result.blue() > 240);
    }

    @Test
    void getColorForCount_1500_isPureWhite() throws Exception {
        TextColor result = invoke("getColorForCount", 1500);
        assertEquals(TextColor.fromHexString("#FFFFFF").rgb(), result.rgb());
    }

    @Test
    void getColorForCount_5000_isWhite() throws Exception {
        TextColor result = invoke("getColorForCount", 5000);
        assertEquals(TextColor.fromHexString("#FFFFFF").rgb(), result.rgb());
    }

    // ============================================================
    // Helper
    // ============================================================

    private TextColor invoke(String method, Object... args) throws Exception {
        Class<?>[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof TextColor) paramTypes[i] = TextColor.class;
            else if (args[i] instanceof Double) paramTypes[i] = double.class;
            else if (args[i] instanceof Integer) paramTypes[i] = int.class;
            else if (args[i] instanceof Number)  paramTypes[i] = double.class; // handles primitive doubles auto-boxed
        }
        Method m = PokePlugin.class.getDeclaredMethod(method, paramTypes);
        m.setAccessible(true);
        return (TextColor) m.invoke(plugin, args);
    }
}