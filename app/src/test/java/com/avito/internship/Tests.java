package com.avito.internship;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Tests {

    private static final String URL = "https://qa-internship.avito.com";
    private static String createdItemId;
    private static int sellerId;

    private static final Pattern ID_PATTERN =
            Pattern.compile("[0-9a-fA-F\\-]{36}");

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = URL;
        sellerId = ThreadLocalRandom.current().nextInt(111111, 999999);
    }

    @Test
    @Order(1)
    @DisplayName("Позитивный: создание объявления")
    void createItem() {
        Statistics statistics = new Statistics();
        statistics.contacts = 3;
        statistics.likes = 5;
        statistics.viewCount = 10;

        ItemRequest request = new ItemRequest();
        request.sellerID = sellerId;
        request.name = "Электроплитка";
        request.price = 45000;
        request.statistics = statistics;

        String status = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/1/item")
                .then()
                .log().ifError()
                .statusCode(200)
                .extract()
                .path("status");

        Matcher matcher = ID_PATTERN.matcher(status);
        Assertions.assertTrue(matcher.find(), "Не удалось найти ID объявления");

        createdItemId = matcher.group();
    }

    @Test
    @Order(2)
    @DisplayName("Позитивный: получение объявления по ID")
    void getItem() {
        Assertions.assertNotNull(createdItemId, "ID объявления не был создан в 1 тесте");

        given()
                .pathParam("id", createdItemId)
                .when()
                .get("/api/1/item/{id}")
                .then()
                .log().ifError()
                .statusCode(200)
                .body("[0].id", equalTo(createdItemId));
    }

    @Test
    @Order(3)
    @DisplayName("Позитивный: получение списка объявлений по sellerID")
    void getItemsBySeller() {
        given()
                .pathParam("sellerID", sellerId)
                .when()
                .get("/api/1/{sellerID}/item")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", greaterThan(0));
    }

    @Test
    @Order(4)
    @DisplayName("Позитивный: получение статистики объявления")
    void getStats() {
        Assertions.assertNotNull(createdItemId, "ID объявления не был создан в 1 тесте");

        given()
                .pathParam("id", createdItemId)
                .when()
                .get("/api/1/statistic/{id}")
                .then()
                .log().ifError()
                .statusCode(200)
                .body("[0].contacts", greaterThanOrEqualTo(0))
                .body("[0].likes", greaterThanOrEqualTo(0))
                .body("[0].viewCount", greaterThanOrEqualTo(0));

    }

    @Test
    @DisplayName("Негативный: получение несуществующего объявления")
    void getItemNotFound() {
        String nonExistentId = UUID.randomUUID().toString();

        given()
                .pathParam("id", nonExistentId)
                .when()
                .get("/api/1/item/{id}")
                .then()
                .log().ifValidationFails()
                .statusCode(404);
    }

    @Test
    @DisplayName("Негативный: создание товара с некорректной ценой")
    void createItemInvalidPrice() {
        ItemInvalidRequest invalidRequest = new ItemInvalidRequest();
        invalidRequest.sellerID = 123456;
        invalidRequest.name = "товар";
        invalidRequest.price = "бесплатно";

        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .post("/api/1/item")
                .then()
                .log().ifValidationFails()
                .statusCode(400);
    }

    static class ItemRequest {
        public int sellerID;
        public String name;
        public int price;
        public Statistics statistics;
    }

    static class Statistics {
        public int contacts;
        public int likes;
        public int viewCount;
    }

    static class ItemInvalidRequest {
        public int sellerID;
        public String name;
        public String price;
    }
}
