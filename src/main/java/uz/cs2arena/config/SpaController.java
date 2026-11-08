package uz.cs2arena.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** React Router sahifalari brauzerda yangilanganda ham index.html qaytariladi. */
@Controller
public class SpaController {

    @GetMapping({"/play", "/tournaments", "/leaderboard", "/teams", "/premium", "/settings", "/wallet",
            "/admin", "/admin/**", "/match/**", "/profile/**", "/invite/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
