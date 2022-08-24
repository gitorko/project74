package com.demo.project74;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.rabbitmq.stream.ByteCapacity;
import com.rabbitmq.stream.Consumer;
import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.Message;
import com.rabbitmq.stream.OffsetSpecification;
import com.rabbitmq.stream.Producer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@EnableAsync
@Service
@Slf4j
public class AsyncService {

    private static final int MESSAGE_COUNT = 10;
    private static final String STREAM_NAME = "my-stream";

    @SneakyThrows
    @Async
    public void producer() {
        log.info("Starting producer!");
        try (Environment environment = Environment.builder().uri("rabbitmq-stream://localhost:5552").build()) {
            environment.streamCreator()
                    .stream(STREAM_NAME)
                    .maxAge(Duration.ofHours(6))
                    .maxSegmentSizeBytes(ByteCapacity.MB(500))
                    .create();
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
                log.info("Published:  {}", message.getBody());
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            boolean done = confirmLatch.await(1, TimeUnit.MINUTES);
            log.info("Completed send: {}", done);
            //environment.deleteStream(STREAM_NAME);
        }
    }

    @SneakyThrows
    @Async
    public void consumer() {
        log.info("Starting consumer!");
        TimeUnit.SECONDS.sleep(2);
        try (Environment environment = Environment.builder().uri("rabbitmq-stream://localhost:5552").build()) {
            Consumer consumer = environment.consumerBuilder()
                    .stream(STREAM_NAME)
                    .offset(OffsetSpecification.last())
                    .messageHandler((context, message) -> {
                        log.info("Consumed:  {}", message.getBody());
                    })
                    .build();
            //Don't let the thread end.
            CountDownLatch finishLatch = new CountDownLatch(1);
            finishLatch.await();
        }
    }
}
