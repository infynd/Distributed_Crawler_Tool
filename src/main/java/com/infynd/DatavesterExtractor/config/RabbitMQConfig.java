package com.infynd.DatavesterExtractor.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    @Value("${crawler.rabbitmq.exchange:crawler.direct.exchange}")
    private String exchangeName;

    @Value("${crawler.rabbitmq.queue.tasks:crawler.tasks.queue}")
    private String taskQueueName;

    @Value("${crawler.rabbitmq.queue.results:crawler.results.queue}")
    private String resultQueueName;

    @Value("${crawler.rabbitmq.queue.dlq:crawler.tasks.dlq}")
    private String dlqQueueName;

    @Value("${crawler.rabbitmq.routing.tasks:crawler.tasks.key}")
    private String taskRoutingKey;

    @Value("${crawler.rabbitmq.routing.results:crawler.results.key}")
    private String resultRoutingKey;

    @Value("${crawler.rabbitmq.routing.dlq:crawler.dlq.key}")
    private String dlqRoutingKey;

    // 1. Direct Exchange
    @Bean
    public DirectExchange crawlerExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    // 2. Dead Letter Queue (DLQ) for failed/poison domains
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqQueueName).build();
    }

    // 3. Task Queue (with Dead Letter configuration)
    @Bean
    public Queue taskQueue() {
        Map<String, Object> args = new HashMap<>();
        // Send rejected/failed messages to the DLQ automatically
        args.put("x-dead-letter-exchange", exchangeName);
        args.put("x-dead-letter-routing-key", dlqRoutingKey);
        return QueueBuilder.durable(taskQueueName).withArguments(args).build();
    }

    // 4. Result Queue (Holds extracted metadata from workers)
    @Bean
    public Queue resultQueue() {
        return QueueBuilder.durable(resultQueueName).build();
    }

    // 5. Bindings
    @Bean
    public Binding taskBinding(Queue taskQueue, DirectExchange crawlerExchange) {
        return BindingBuilder.bind(taskQueue).to(crawlerExchange).with(taskRoutingKey);
    }

    @Bean
    public Binding resultBinding(Queue resultQueue, DirectExchange crawlerExchange) {
        return BindingBuilder.bind(resultQueue).to(crawlerExchange).with(resultRoutingKey);
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, DirectExchange crawlerExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(crawlerExchange).with(dlqRoutingKey);
    }

    // 6. JSON Message Converter (Translates Java objects <-> JSON automatically)
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 7. Configured RabbitTemplate
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
