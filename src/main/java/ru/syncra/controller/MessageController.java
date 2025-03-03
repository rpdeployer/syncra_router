package ru.syncra.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/mobile")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private final RabbitTemplate rabbitTemplate;
    private final String messageQueue;
    private final String dbQueue;
    private final String logQueue;
    private final ObjectMapper objectMapper;
    private final Executor executor = Executors.newFixedThreadPool(10);

    public MessageController(RabbitTemplate rabbitTemplate,
                             @Value("${spring.queues.message-queue}") String messageQueue,
                             @Value("${spring.queues.db-queue}") String dbQueue,
                             @Value("${spring.queues.log-queue}") String logQueue,
                             ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.messageQueue = messageQueue;
        this.logQueue = logQueue;
        this.dbQueue = dbQueue;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/sms")
    public ResponseEntity<Map<String, Boolean>> sendSms(@RequestBody String message) throws JsonProcessingException {
        logger.info("Получен запрос на отправку Sms/Notification: {}", message);

        try {
            CompletableFuture<Void> firstQueueFuture = CompletableFuture.runAsync(() -> sendToQueue(messageQueue, message), executor);
            CompletableFuture<Void> secondQueueFuture = CompletableFuture.runAsync(() -> sendToQueue(dbQueue, message), executor);
            CompletableFuture.allOf(firstQueueFuture, secondQueueFuture).join();

            logger.info("Sms/Notification успешно отправлено в обе очереди");
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            logger.error("Ошибка при отправке сообщения: {}", e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false));
        }
    }

    @PostMapping("/log")
    public ResponseEntity<Map<String, Boolean>> sendLog(@RequestBody String message) throws JsonProcessingException {
        logger.info("Получен запрос на отправку Log: {}", message);

        try {
            CompletableFuture<Void> firstQueueFuture = CompletableFuture.runAsync(() -> sendToQueue(logQueue, message), executor);
            CompletableFuture.allOf(firstQueueFuture).join();

            logger.info("Log успешно отправлено в очередь");
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            logger.error("Ошибка при отправке сообщения: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false));
        }
    }

    private void sendToQueue(String queue, String message) {
        try {
            rabbitTemplate.convertAndSend(queue, objectMapper.readTree(message));
            logger.info("Сообщение отправлено в очередь {}: {}", queue, message);
        } catch (Exception e) {
            logger.error("Ошибка при обработке JSON для очереди {}: {}", queue, e.getMessage(), e);
            throw new RuntimeException("Ошибка при отправке JSON в RabbitMQ", e);
        }
    }

}