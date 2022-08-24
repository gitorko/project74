package com.demo.project74;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import com.rabbitmq.stream.Consumer;
import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.Message;
import com.rabbitmq.stream.OffsetSpecification;
import com.rabbitmq.stream.Producer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Slf4j
public class Main {

    private static final int MESSAGE_COUNT = 10;
    private static final String STREAM_NAME = "my-stream";

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public CommandLineRunner producer() {
        return args -> {
            log.info("Starting producer!");
            try (Environment environment = Environment.builder().uri("rabbitmq-stream://localhost:5552").build()) {
                environment.streamCreator().stream(STREAM_NAME).create();
                Producer producer = environment
                        .producerBuilder()
                        .stream(STREAM_NAME)
                        .build();

                CountDownLatch confirmLatch = new CountDownLatch(MESSAGE_COUNT);
                IntStream.range(0, MESSAGE_COUNT).forEach(i -> {
                    Message message = producer.messageBuilder()
                            .properties()
                            .creationTime(System.currentTimeMillis())
                            .messageId(i)
                            .messageBuilder()
                            .addData(("customer_" + i).getBytes(StandardCharsets.UTF_8))
                            .build();
                    producer.send(message, confirmationStatus -> confirmLatch.countDown());
                });
                boolean done = confirmLatch.await(1, TimeUnit.MINUTES);
                log.info("Completed send: {}", done);
            }
        };
    }

    @Bean
    public CommandLineRunner consumer() {
        return args -> {
            log.info("Starting consumer!");
            TimeUnit.SECONDS.sleep(2);
            try (Environment environment = Environment.builder().uri("rabbitmq-stream://localhost:5552").build()) {
                AtomicInteger messageConsumed = new AtomicInteger(0);
                Consumer consumer = environment.consumerBuilder()
                        .stream(STREAM_NAME)
                        .offset(OffsetSpecification.first())
                        .messageHandler((context, message) -> {
                            log.info("Consumed:  {}", message.getBody());
                            messageConsumed.incrementAndGet();
                        })
                        .build();
            }
        };
    }
}
