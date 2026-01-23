package org.ohdsi.webapi.smoketests.authorization;

import org.junit.jupiter.api.Test;
import org.ohdsi.webapi.WebapiApplication;
import org.ohdsi.webapi.authorization.dto.PermissionDTO;
import org.ohdsi.webapi.smoketests.AbstractIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = WebapiApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Testcontainers
public class PermissionsIT extends AbstractIntegrationTest {

    @Test
    public void getPermissions_Authorized_ReturnsAllPermissions() {
        var returnedPermissions =
                given()
                        .when()
                        .get("/WebAPI/permission")
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(ContentType.JSON)
                        .extract()
                        .jsonPath()
                        .getList(".", PermissionDTO.class);

        assertThat(returnedPermissions).hasSize(40);
    }

}
