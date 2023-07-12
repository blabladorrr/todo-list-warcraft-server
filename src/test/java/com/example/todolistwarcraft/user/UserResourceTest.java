package com.example.todolistwarcraft.user;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class UserResourceTest {
    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void shouldListAllUsers() {
        given()
                .when().get("/api/v1/users")
                .then()
                .statusCode(200)
                .body("$.size()", greaterThanOrEqualTo(1),
                        "[0].name", is("admin"),
                        "[0].password", nullValue());
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void shouldCreateUser() {
        given()
                .body("{\"name\": \"test\", \"password\": \"test\", \"roles\": [\"user\"]}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/users")
                .then()
                .statusCode(201)
                .body(
                        "name", is("test"),
                        "password", nullValue(),
                        "created", not(emptyString())
                );
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void shouldNotCreateUserUnauthorized() {
        given()
                .body("{\"name\": \"test-unauthorized\", \"password\": \"test\", \"roles\": [\"user\"]}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/users")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void shouldNotCreateDuplicateNameUser() {
        given()
                .body("{\"name\": \"user\", \"password\": \"test\", \"roles\": [\"user\"]}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/users")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void shouldUpdateUserName() {
        var user = given()
                .body("{\"name\": \"before-update\", \"password\": \"test\", \"roles\": [\"user\"]}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/users")
                .as(User.class);

        user.name = "after-update";

        given()
                .body(user)
                .contentType(ContentType.JSON)
                .when().put("/api/v1/users" + user.id)
                .then()
                .statusCode(200)
                .body(
                        "name", is("after-update"),
                        "version", is(user.version + 1)
                );
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void shouldNotUpdateOptimisticLock() {
        given()
                .body("{\"name\": \"after-update\", \"version\": 1337}")
                .contentType(ContentType.JSON)
                .when().put("/api/v1/users/0")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void shouldNotUpdateNonExistingUser() {
        given()
                .body("{\"name\": \"idontexist\", \"password\": \"test\"}")
                .contentType(ContentType.JSON)
                .when().put("/api/v1/users/99999")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "user")
    void shouldChangePassword() {
        given()
                .body("{\"currentPassword\": \"quarkus\", \"newPassword\": \"changed\"}")
                .contentType(ContentType.JSON)
                .when().put("/api/v1/users/self/password")
                .then()
                .statusCode(200);
        assertTrue(BcryptUtil.matches("changed",
                User.<User>findById(0L).await().indefinitely().password));
    }

    @Test
    @TestSecurity(user = "admin", roles = "user")
    void shouldNotChangeNotMatchingPassword() {
        given()
                .body("{\"currentPassword\": \"aaaaaa\", \"newPassword\": \"changed\"}")
                .contentType(ContentType.JSON)
                .when().put("/api/v1/users/self/password")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "admin", roles = "user")
    void shouldGetCurrentUser() {
        given()
                .when().get("/api/v1/users/self")
                .then()
                .statusCode(200)
                .body("name", is("admin"));
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void shouldNotGetNonExistentUser() {
        given()
                .when().get("/api/v1/users/99999")
                .then()
                .statusCode(404);
    }
}