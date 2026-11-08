package uz.cs2arena.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uz.cs2arena.config.AppProperties;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Steam OpenID login va Steam Web API (profil) bilan ishlash. */
@Component
public class SteamClient {

    private static final String OPENID_ENDPOINT = "https://steamcommunity.com/openid/login";
    private static final Pattern CLAIMED_ID = Pattern.compile("^https?://steamcommunity\\.com/openid/id/(\\d{17})$");
    private static final Logger log = LoggerFactory.getLogger(SteamClient.class);

    public record SteamProfile(String nickname, String avatarUrl) {}

    private final AppProperties props;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public SteamClient(AppProperties props, ObjectMapper json) {
        this.props = props;
        this.json = json;
    }

    public String returnUrl() {
        return props.baseUrlClean() + "/auth/steam/return";
    }

    public String loginRedirectUrl() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("openid.ns", "http://specs.openid.net/auth/2.0");
        p.put("openid.mode", "checkid_setup");
        p.put("openid.return_to", returnUrl());
        p.put("openid.realm", props.baseUrlClean());
        p.put("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select");
        p.put("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select");
        return OPENID_ENDPOINT + "?" + formEncode(p);
    }

    /** Steam qaytargan javobni Steam'ning o'zida tekshiradi va SteamID64 qaytaradi. */
    public Optional<String> verify(Map<String, String> openidParams) {
        String returnTo = openidParams.getOrDefault("openid.return_to", "");
        Matcher m = CLAIMED_ID.matcher(openidParams.getOrDefault("openid.claimed_id", ""));
        if (!returnTo.startsWith(returnUrl()) || !m.matches()) {
            return Optional.empty();
        }
        Map<String, String> check = new LinkedHashMap<>(openidParams);
        check.put("openid.mode", "check_authentication");
        try {
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(OPENID_ENDPOINT))
                            .timeout(Duration.ofSeconds(10))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(formEncode(check)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            return r.body().contains("is_valid:true") ? Optional.of(m.group(1)) : Optional.empty();
        } catch (Exception e) {
            log.warn("Steam OpenID tekshiruvi muvaffaqiyatsiz: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** API kalit bo'lsa — nik va avatar; bo'lmasa bo'sh. */
    public Optional<SteamProfile> fetchProfile(String steamId) {
        String key = props.steamApiKey();
        if (key == null || key.isBlank()) return Optional.empty();
        try {
            String url = "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key="
                    + URLEncoder.encode(key, StandardCharsets.UTF_8) + "&steamids=" + steamId;
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofString());
            JsonNode player = json.readTree(r.body()).path("response").path("players").path(0);
            if (player.isMissingNode()) return Optional.empty();
            return Optional.of(new SteamProfile(
                    player.path("personaname").asText("Player"),
                    player.path("avatarfull").asText(null)));
        } catch (Exception e) {
            log.warn("Steam profilini olib bo'lmadi: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String formEncode(Map<String, String> p) {
        return p.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
