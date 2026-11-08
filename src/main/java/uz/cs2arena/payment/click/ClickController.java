package uz.cs2arena.payment.click;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Click kabinetida ko'rsatiladigan manzillar:
 *   Prepare:  /api/payments/click/prepare
 *   Complete: /api/payments/click/complete
 */
@RestController
@RequestMapping("/api/payments/click")
public class ClickController {

    private final ClickService service;

    public ClickController(ClickService service) {
        this.service = service;
    }

    @PostMapping(value = "/prepare", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Map<String, Object> prepare(@RequestParam Map<String, String> params) {
        return service.prepare(params);
    }

    @PostMapping(value = "/complete", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Map<String, Object> complete(@RequestParam Map<String, String> params) {
        return service.complete(params);
    }
}
