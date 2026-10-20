package co.sena.actors.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "co.sena.actors")
@EnableJpaRepositories(basePackages = "co.sena.actors.adapter.out.persistence")
@EntityScan(basePackages = "co.sena.actors.adapter.out.persistence")
public class ActorsApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ActorsApiApplication.class, args);
    }
}
