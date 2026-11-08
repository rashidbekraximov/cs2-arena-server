package uz.cs2arena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class Cs2ArenaApplication {

    public static void main(String[] args) {
        SpringApplication.run(Cs2ArenaApplication.class, args);
    }
}
