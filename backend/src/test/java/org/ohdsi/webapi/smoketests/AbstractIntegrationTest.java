package org.ohdsi.webapi.smoketests;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class AbstractIntegrationTest {
    @LocalServerPort
    private int port;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("webapi.app-db.url", postgres::getJdbcUrl);
        registry.add("webapi.app-db.username", postgres::getUsername);
        registry.add("webapi.app-db.password", postgres::getPassword);
        registry.add("webapi.app-db.flyway-username", postgres::getUsername);
        registry.add("webapi.app-db.flyway-password", postgres::getPassword);
    }

    @BeforeEach
    public void init() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
    }

    /**
     * Helper method to create a request with CSRF token for POST/PUT/DELETE operations
     */
    protected RequestSpecification givenWithCsrf() {
        // First, make a GET request to obtain the CSRF token cookie
        var response = given()
            .when()
            .get("/WebAPI/role")
            .then()
            .extract()
            .response();

        var csrfToken = response.getCookie("XSRF-TOKEN");

        // Return a RequestSpecification with the CSRF token
        return given()
            .cookie("XSRF-TOKEN", csrfToken)
            .header("X-XSRF-TOKEN", csrfToken);
    }
}
