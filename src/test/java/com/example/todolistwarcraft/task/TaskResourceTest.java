package com.example.todolistwarcraft.task;

import com.example.todolistwarcraft.user.User;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TaskResourceTest {
    @Test
    @TestSecurity(user = "user", roles = "user")
    void shouldListTask() {
        given()
                .body("{\"name\": \"to-be-listed\"}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/tasks").as(Task.class);
        given()
                .when().get("/api/v1/tasks")
                .then()
                .statusCode(200)
                .body("$",
                        allOf(
                                hasItem(
                                        hasEntry("name", "to-be-listed")
                                ),
                                everyItem(
                                        hasEntry(is("user"), (Matcher)hasEntry("name", "user"))
                                )
                        ));
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void shouldCreateNewTask() {
        given()
                .body("{\"name\": \"new-task\"}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/tasks")
                .then()
                .statusCode(201)
                .body(
                        "name", is("new-task"),
                        "created", not(emptyString())
                );
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void shouldUpdateTask() {
        var toUpdate = given()
                .body("{\"name\": \"to-update\"}")
                .contentType(ContentType.JSON)
                .post("/api/v1/tasks").as(Task.class);
        toUpdate.name = "updated";
        given()
                .body(toUpdate)
                .contentType(ContentType.JSON)
                .when().put("/api/v1/tasks" + toUpdate.id)
                .then()
                .statusCode(200)
                .body(
                        "title", is("updated"),
                        "version", is(toUpdate.version + 1)
                );
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void shouldNotUpdateNonExistentTask() {
        given()
                .body("{\"name\":\"updated\"}")
                .contentType(ContentType.JSON)
                .when().put("/api/v1/tasks/1337")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void shouldNotUpdateUnauthorized() {
        final User admin = User.<User>findById(0L).await().indefinitely();
        Task adminTask = new Task();
        adminTask.name = "admins-task";
        adminTask.user = admin;
        adminTask = adminTask.<Task>persistAndFlush().await().indefinitely();
        given()
                .body("{\"name\":\"to-update\"}")
                .contentType(ContentType.JSON)
                .when().put("/api/v1/tasks/" + adminTask.id)
                .then()
                .statusCode(401); // TODO: TaskService UnauthorizedException should be changed to ForbiddenException
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void shouldDeleteTask() {
        var toDelete = given()
                .body("{\"name\":\"to-delete\"}")
                .contentType(ContentType.JSON)
                .post("/api/v1/tasks").as(Task.class);
        given()
                .when().delete("/api/v1/tasks/" + toDelete.id)
                .then()
                .statusCode(204);
        assertThat(Task.findById(toDelete.id).await().indefinitely(), nullValue());
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void shouldSetTaskAsDone() {
        var toSetDone = given()
                .body("{\"name\": \"to-set-done\"}")
                .contentType(ContentType.JSON)
                .post("/api/v1/tasks").as(Task.class);
        System.out.println(toSetDone);
        given()
                .body("\"true\"")
                .contentType(ContentType.JSON)
                .when().put("/api/v1/tasks" + toSetDone.id + "/complete")
                .then()
                .statusCode(200);
        assertThat(Task.findById(toSetDone.id).await()
                .indefinitely(),
                allOf(
                        hasProperty("complete", notNullValue()),
                        hasProperty("version", is(toSetDone.version + 1))
                ));
    }
}