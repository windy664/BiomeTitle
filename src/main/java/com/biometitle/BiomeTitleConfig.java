package com.biometitle;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class BiomeTitleConfig {

    private static final String DEFAULT_CONFIG =
        "# BiomeTitle Configuration\n" +
        "\n" +
        "# Title display timing (in ticks, 20 ticks = 1 second)\n" +
        "fade-in: 15\n" +
        "stay: 50\n" +
        "fade-out: 20\n" +
        "\n" +
        "# Title color (hex RGB, e.g. FFAA00 for gold)\n" +
        "title-color: FFAA00\n" +
        "\n" +
        "# Divider color\n" +
        "divider-color: 888888\n" +
        "\n" +
        "# Show decorative divider line\n" +
        "show-divider: true\n" +
        "\n" +
        "# Divider text (Unicode decorations)\n" +
        "divider-text: \"───── ✦ ─────\"\n" +
        "\n" +
        "# Biome blacklist (won't show titles for these)\n" +
        "# Use translation keys: biome.minecraft.ocean, biome.minecraft.river, etc.\n" +
        "blacklist:\n" +
        "  - biome.minecraft.ocean\n" +
        "  - biome.minecraft.deep_ocean\n" +
        "  - biome.minecraft.cold_ocean\n" +
        "  - biome.minecraft.deep_cold_ocean\n" +
        "  - biome.minecraft.lukewarm_ocean\n" +
        "  - biome.minecraft.deep_lukewarm_ocean\n" +
        "  - biome.minecraft.warm_ocean\n" +
        "  - biome.minecraft.deep_warm_ocean\n" +
        "  - biome.minecraft.frozen_ocean\n" +
        "  - biome.minecraft.deep_frozen_ocean\n" +
        "  - biome.minecraft.river\n" +
        "  - biome.minecraft.frozen_river\n";

    public int fadeIn = 15;
    public int stay = 50;
    public int fadeOut = 20;
    public int titleColor = 0xFFAA00;
    public int dividerColor = 0x888888;
    public boolean showDivider = true;
    public String dividerText = "───── ✦ ─────";
    public Set<String> blacklist = new HashSet<>();

    public void load(Path dataDirectory) {
        try {
            Files.createDirectories(dataDirectory);
            Path configPath = dataDirectory.resolve("config.yml");

            if (!Files.exists(configPath)) {
                Files.writeString(configPath, DEFAULT_CONFIG);
            }

            Map<String, Object> data = new Yaml().load(Files.newInputStream(configPath));
            if (data == null) return;

            fadeIn = getInt(data, "fade-in", 15);
            stay = getInt(data, "stay", 50);
            fadeOut = getInt(data, "fade-out", 20);
            titleColor = parseHex(data, "title-color", 0xFFAA00);
            dividerColor = parseHex(data, "divider-color", 0x888888);
            showDivider = getBool(data, "show-divider", true);
            dividerText = getString(data, "divider-text", "───── ✦ ─────");

            List<?> bl = (List<?>) data.get("blacklist");
            if (bl != null) {
                blacklist.clear();
                for (Object o : bl) {
                    blacklist.add(o.toString());
                }
            }
        } catch (IOException e) {
            // Use defaults
        }
    }

    public boolean isBlacklisted(String translationKey) {
        return blacklist.contains(translationKey);
    }

    private static int getInt(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    private static boolean getBool(Map<String, Object> m, String key, boolean def) {
        Object v = m.get(key);
        return v instanceof Boolean ? (Boolean) v : def;
    }

    private static String getString(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null ? v.toString() : def;
    }

    private static int parseHex(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v == null) return def;
        try {
            return (int) Long.parseLong(v.toString().replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
