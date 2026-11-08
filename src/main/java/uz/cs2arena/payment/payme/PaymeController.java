package uz.cs2arena.payment.payme;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uz.cs2arena.config.AppProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** Payme Merchant API (JSON-RPC 2.0) endpoint. Payme kabinetida shu manzil ko'rsatiladi: /api/payments/payme */
@RestController
public class PaymeController {

    private static final Logger log = LoggerFactory.getLogger(PaymeController.class);

    private final PaymeService service;
    private final AppProperties props;
    private final ObjectMapper json;

    public PaymeController(PaymeService service, AppProperties props, ObjectMapper json) {
        this.service = service;
        this.props = props;
        this.json = json;
    }

    @PostMapping("/api/payments/payme")
    public Map<String, Object> handle(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestBody String body) {
        JsonNode request;
        try {
            request = json.readTree(body);
        } catch (Exception e) {
            return error(null, new PaymeException(PaymeException.PARSE_ERROR, "JSON xato"));
        }
        JsonNode id = request.path("id");

        if (!authorized(authorization)) {
            return error(id, new PaymeException(PaymeException.AUTH_FAILED, "Avtorizatsiya xatosi"));
        }

        try {
            JsonNode params = request.path("params");
            Object result = switch (request.path("method").asText()) {
                case "CheckPerformTransaction" -> service.checkPerformTransaction(params);
                case "CreateTransaction" -> service.createTransaction(params);
                case "PerformTransaction" -> service.performTransaction(params);
                case "CancelTransaction" -> service.cancelTransaction(params);
                case "CheckTransaction" -> service.checkTransaction(params);
                case "GetStatement" -> service.getStatement(params);
                default -> throw new PaymeException(PaymeException.METHOD_NOT_FOUND, "Metod topilmadi");
            };
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("jsonrpc", "2.0");
            r.put("id", id);
            r.put("result", result);
            return r;
        } catch (PaymeException e) {
            return error(id, e);
        } catch (Exception e) {
            log.error("Payme so'rovini bajarishda xato", e);
            return error(id, new PaymeException(-32400, "Ichki xato"));
        }
    }

    /** Payme "Basic base64(Paycom:KEY)" yuboradi. */
    private boolean authorized(String header) {
        String key = props.payme().key();
        if (header == null || !header.startsWith("Basic ") || key == null || key.isBlank()) return false;
        try {
            String decoded = new String(Base64.getDecoder().decode(header.substring(6)), StandardCharsets.UTF_8);
            String expected = "Paycom:" + key;
            return MessageDigest.isEqual(decoded.getBytes(StandardCharsets.UTF_8),
                    expected.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static Map<String, Object> error(JsonNode id, PaymeException e) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("code", e.getCode());
        err.put("message", Map.of("uz", e.getMessage(), "ru", e.getMessage(), "en", e.getMessage()));
        if (e.getData() != null) err.put("data", e.getData());
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("jsonrpc", "2.0");
        r.put("id", id);
        r.put("error", err);
        return r;
    }
}
