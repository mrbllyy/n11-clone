package com.n11bootcamp.shopping_cart_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
@CrossOrigin
public class ShoppingCartServiceApplication {

    /**
     * ?? RestTemplate Bean Tanımı
     * - @Bean › Bu metodu Spring context’e bir Bean olarak ekler.
     * - RestTemplate, Spring’in HTTP client’ıdır.
     *   Microservice’ler arası REST çağrıları yapmak için kullanılır.
     *
     * @LoadBalanced
     * - Eğer Spring Cloud Netflix Eureka veya başka bir Service Discovery kullanıyorsan,
     *   RestTemplate çağrılarında doğrudan **service-name** ile istek atabilirsin.
     *   Örn: http://PRODUCT-SERVICE/api/product/1
     *   Bu durumda Eureka’dan ilgili instance bulunur ve load balancing yapılır.
     * - Yani RestTemplate + Ribbon (ya da Spring Cloud LoadBalancer) birlikte çalışır.
     *
     * @return RestTemplate instance
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * ?? main metodu
     * - Spring Boot uygulamasını başlatır.
     * - Tomcat (embedded server) ayağa kalkar.
     * - Uygulama `http://localhost:8080` üzerinde çalışmaya başlar (default port).
     */
	public static void main(String[] args) {
		SpringApplication.run(ShoppingCartServiceApplication.class, args);
	}

}
