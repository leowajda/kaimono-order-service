package com.kaimono.order.service.book;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

@TestMethodOrder(MethodOrderer.Random.class)
public class BookClientTests {

    private MockWebServer mockWebServer;
    private BookClient bookClient;

    @BeforeEach
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        var webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").uri().toString())
                .build();

        bookClient = new BookClient(webClient);
    }

    @AfterEach
    public void clean() throws IOException {
        mockWebServer.shutdown();
    }

    @ParameterizedTest
    @ValueSource(strings = "1234567890")
    public void whenBookExistsThenReturnBook(String isbn) {

        var mockResponse = new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                            "isbn": %s,
                            "title": "mock-title",
                            "author": "mock-author",
                            "price": 9.90,
                            "publisher": "mock-publisher"
                        }
                        """.formatted(isbn));
        mockWebServer.enqueue(mockResponse);

        var book = bookClient.getBookByIsbn(isbn);
        StepVerifier.create(book)
                .assertNext(b -> b.isbn().equals(b.isbn()))
                .verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(strings = "1234567890")
    public void whenBookNotExistsThenReturnEmpty(String isbn) {

        var mockResponse = new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.NOT_FOUND.value());

        var book = bookClient.getBookByIsbn(isbn);
        StepVerifier.create(book)
                .expectNextCount(0)
                .verifyComplete();
    }

}
