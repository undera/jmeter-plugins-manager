package org.jmeterplugins.repository.exception;

import org.junit.Test;

import static org.junit.Assert.*;



public class DownloadExceptionTest {

    @Test
    public void test() throws Exception {
        DownloadException exception = new DownloadException();
        assertNull(exception.getMessage());
        exception = new DownloadException("Message");
        assertEquals("Message", exception.getMessage());
        exception = new DownloadException("Message", new RuntimeException("Cause"));
        assertEquals("Message", exception.getMessage());
        assertNotNull(exception.getCause() instanceof RuntimeException);
        assertEquals("Cause", exception.getCause().getMessage());
    }
}