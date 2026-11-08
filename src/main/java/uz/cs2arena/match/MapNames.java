package uz.cs2arena.match;

import java.util.Map;

public final class MapNames {

    private static final Map<String, String> NAMES = Map.of(
            "de_dust2", "Dust II",
            "de_mirage", "Mirage",
            "de_inferno", "Inferno",
            "de_nuke", "Nuke",
            "de_ancient", "Ancient",
            "de_anubis", "Anubis",
            "de_train", "Train",
            "de_vertigo", "Vertigo",
            "de_overpass", "Overpass");

    private MapNames() {}

    public static String pretty(String map) {
        if (map == null) return "—";
        return NAMES.getOrDefault(map, map.replace("de_", ""));
    }
}
