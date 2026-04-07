package com.organization.accessgrid;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Functional interface for sending HTTP requests.
 * Enables mocking in tests without needing to mock HttpClient directly.
 */
@FunctionalInterface
public interface HttpSender {
    HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException;
}
