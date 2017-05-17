package org.jmeterplugins.repository.exception;

/**
 *  Throws when there are any errors in downloading
 */
public class DownloadException extends RuntimeException {
    public DownloadException() {
    }

    public DownloadException(String message) {
        super(message);
    }

    public DownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
