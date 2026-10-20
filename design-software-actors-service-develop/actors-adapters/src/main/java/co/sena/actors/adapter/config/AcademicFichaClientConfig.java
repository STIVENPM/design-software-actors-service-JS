package co.sena.actors.adapter.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AcademicFichaClientConfig {

    @Bean("academicFichaRestClient")
    RestClient academicFichaRestClient(
            RestClient.Builder builder, @Value("${academic-management.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
