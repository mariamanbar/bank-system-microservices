package com.mariam.accountservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.name}")
    private String queue;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;
    
    private String loanRoutingKey = "loan.event";

    @Bean 
    Queue queue() { 
        return new Queue(queue);  // The Mailroom
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(exchange); // The Mailbox
    }

    @Bean
    Binding binding() {
        return BindingBuilder.bind(queue())
                .to(exchange())
                .with(routingKey);  // The Address
    }
    
    @Bean
    Binding loanBinding(Queue queue, TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)                 // Use the same queue
                .to(exchange)                // Use the same exchange
                .with(loanRoutingKey);       // Use the Loan key
    }
}