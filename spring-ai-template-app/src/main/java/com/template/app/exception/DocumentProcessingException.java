package com.template.app.exception;

/**
 * Thrown when the asynchronous document processing pipeline fails.
 * The message is rejected to the dead-letter queue (see RabbitMqConfig);
 * the document row is already marked as {@code failed} at this point.
 */
public class DocumentProcessingException extends RuntimeException {

    private final String docId;

    public DocumentProcessingException(String docId, Throwable cause) {
        super("Failed to process document " + docId, cause);
        this.docId = docId;
    }

    public String getDocId() {
        return docId;
    }
}
