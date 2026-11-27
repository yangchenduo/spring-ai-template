package com.template.app.config;

import com.template.app.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes document-processing requests. Failures propagate so the container
 * rejects the message to the dead-letter queue; the document row is marked
 * {@code failed} by the service before the exception is rethrown.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqConsumer {

    private final KnowledgeService knowledgeService;

    @RabbitListener(queues = RabbitMqConfig.QUEUE_DOC_PROCESS)
    public void onDocumentProcess(@Payload Map<String, Object> message) {
        String docId = (String) message.get("docId");
        log.info("Received document processing request: docId={}", docId);
        knowledgeService.processDocument(docId);
    }
}
