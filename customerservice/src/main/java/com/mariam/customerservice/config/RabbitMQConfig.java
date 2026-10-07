package com.mariam.customerservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	@Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(exchange);
    }
}
