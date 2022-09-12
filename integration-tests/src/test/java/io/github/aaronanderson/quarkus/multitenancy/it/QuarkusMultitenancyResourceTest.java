package io.github.aaronanderson.quarkus.multitenancy.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusMultitenancyResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/quarkus-multitenancy")
                .then()
                .statusCode(200)
                .body(is("Hello quarkus-multitenancy"));
    }
}
