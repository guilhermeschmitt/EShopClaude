package br.com.guilherme.eshop.basket.client;

import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * One shared RestClient.Builder for both peer clients (Catalog and Ordering), replacing Boot's prototype builder.
 * Boot's configurer still applies every RestClientCustomizer, so tests get a single MockRestServiceServer binding
 * that stubs both peers. Each client must clone() the builder before setting its base URL.
 */
@Configuration(proxyBeanMethods = false)
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder());
    }
}
