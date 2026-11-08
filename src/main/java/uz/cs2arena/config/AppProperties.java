package uz.cs2arena.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String name,
        String baseUrl,
        String steamApiKey,
        List<String> superAdminSteamIds,
        Banner banner,
        List<String> mapPool,
        Matchzy matchzy,
        Premium premium,
        Payme payme,
        Click click,
        Topup topup
) {
    public AppProperties {
        superAdminSteamIds = superAdminSteamIds == null ? List.of() : superAdminSteamIds;
        mapPool = mapPool == null || mapPool.isEmpty()
                ? List.of("de_mirage", "de_inferno", "de_dust2", "de_nuke", "de_ancient", "de_anubis", "de_train")
                : mapPool;
    }

    /** Oxiridagi "/" belgisisiz manzil */
    public String baseUrlClean() {
        String b = baseUrl == null ? "http://localhost:8080" : baseUrl;
        return b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
    }

    public record Banner(String badge, String title, String subtitle,
                         String prizeLabel, String prizeValue, String prizeNote) {}

    public record Matchzy(String secret) {}

    public record Premium(int yearlyDiscountPercent, List<Plan> plans) {
        public record Plan(String code, String name, long monthlyPrice) {}
    }

    public record Payme(boolean enabled, boolean testMode, String merchantId, String key, String accountField) {}

    public record Click(boolean enabled, String serviceId, String merchantId, String secretKey) {}

    public record Topup(long minAmount, long maxAmount) {}
}
