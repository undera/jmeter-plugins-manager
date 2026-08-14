package org.jmeterplugins.repository;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * A local endpoint that always answers 500, for exercising the repository and download
 * failure paths.
 * <p>
 * These tests used to point at the public httpstat.us service, which made the suite depend on
 * a third party being reachable and honest about its status codes, fail outright when offline,
 * and spend minutes per run inside connect timeouts and retry waits - 221 of the 230 seconds
 * of one local run. A loopback server answers instantly and always.
 */
class FailingHttpStub implements Closeable {

    private final HttpServer server;

    FailingHttpStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] body = "failed on purpose".getBytes();
                exchange.sendResponseHeaders(500, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            }
        });
        server.start();
    }

    /** Port is picked by the OS, so this has to be read rather than assumed. */
    String url() {
        return "http://localhost:" + server.getAddress().getPort() + "/";
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
