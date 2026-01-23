package org.ohdsi.webapi.smoketests.authorization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ohdsi.webapi.WebapiApplication;
import org.ohdsi.webapi.authorization.Permissions;
import org.ohdsi.webapi.authorization.dto.PermissionDTO;
import org.ohdsi.webapi.authorization.dto.RoleDTO;
import org.ohdsi.webapi.authorization.dto.UserDTO;
import org.ohdsi.webapi.smoketests.AbstractIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = WebapiApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Testcontainers
public class RolesIT extends AbstractIntegrationTest {

    @Test
    public void getRoles_Authorized_ReturnsAllRoles() {
        var returnedRoles =
                given()
                        .when()
                        .get("/WebAPI/role")
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(ContentType.JSON)
                        .extract()
                        .jsonPath()
                        .getList(".", RoleDTO.class);

        assertThat(returnedRoles).hasSizeGreaterThanOrEqualTo(8);
    }

    @Test
    public void getRole_WithValidId_ReturnsRole() {
        var roleId = 1L;
        var role =
                given()
                        .when()
                        .get("/WebAPI/role/{roleId}", roleId)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(ContentType.JSON)
                        .extract()
                        .as(RoleDTO.class);

        assertThat(role.id()).isEqualTo(roleId);
        assertThat(role.name()).isNotBlank();
    }

    @Test
    public void createRole_WithValidName_CreatesAndReturnsRole() {
        var newRole = new RoleDTO(null, "Test Integration Role", false);

        var createdRole =
                givenWithCsrf()
                        .contentType(ContentType.JSON)
                        .body(newRole)
                        .when()
                        .post("/WebAPI/role")
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(ContentType.JSON)
                        .extract()
                        .as(RoleDTO.class);

        assertThat(createdRole.id()).isNotNull();
        assertThat(createdRole.name()).isEqualTo("Test Integration Role");
        assertThat(createdRole.isSystemRole()).isTrue();
    }

    @Test
    public void changeRoleName_WithValidData_UpdatesRole() {
        // First create a role to update
        var newRole = new RoleDTO(null, "Original Role Name", false);
        var createdRole =
                givenWithCsrf()
                        .contentType(ContentType.JSON)
                        .body(newRole)
                        .post("/WebAPI/role")
                        .as(RoleDTO.class);

        // Update the role name
        var updatedRole = new RoleDTO(createdRole.id(), "Updated Role Name", createdRole.isSystemRole());
        var result =
                givenWithCsrf()
                        .contentType(ContentType.JSON)
                        .body(updatedRole)
                        .when()
                        .put("/WebAPI/role/{roleId}", createdRole.id())
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(ContentType.JSON)
                        .extract()
                        .as(RoleDTO.class);

        assertThat(result.id()).isEqualTo(createdRole.id());
        assertThat(result.name()).isEqualTo("Updated Role Name");
    }

    @Test
    public void deleteRole_WithValidId_DeletesRole() {
        // First create a role to delete
        var newRole = new RoleDTO(null, "Role To Delete", false);
        var createdRole =
                givenWithCsrf()
                        .contentType(ContentType.JSON)
                        .body(newRole)
                        .post("/WebAPI/role")
                        .as(RoleDTO.class);

        // Get all permissions
        var permissions =
                given()
                        .when()
                        .get("/WebAPI/permission")
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(ContentType.JSON)
                        .extract()
                        .jsonPath()
                        .getList(".", PermissionDTO.class);
        var deleteRolePermission = permissions.stream()
                .filter(p -> p.permission().equals(Permissions.DELETE_ROLE))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("DELETE_ROLE permission not found"));

        // Next, make sure the admin role has the permission to delete roles
        givenWithCsrf()
                .when()
                .put("/WebAPI/role/{roleId}/permissions/{permissionIdList}", 2L, deleteRolePermission.id())
                .then()
                .statusCode(HttpStatus.OK.value());

        // Delete the role
        givenWithCsrf()
                .when()
                .delete("/WebAPI/role/{roleId}", createdRole.id())
                .then()
                .statusCode(HttpStatus.OK.value());

        // Verify it's deleted by trying to get it
        given()
                .when()
                .get("/WebAPI/role/{roleId}", createdRole.id())
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void getRolePermissions_WithAdminRoleId_ReturnsPermissions() {
        var roleId = 2L;
        var permissions =
                given()
                        .when()
                        .get("/WebAPI/role/{roleId}/permissions", roleId)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(ContentType.JSON)
                        .extract()
                        .jsonPath()
                        .getList(".", PermissionDTO.class);

        assertThat(permissions).isNotNull();
        assertThat(permissions).anyMatch((p) -> p.permission().equals(Permissions.CREATE_ROLE));
    }


    @Test
    public void getRolePermissions_WithValidRoleId_ReturnsPermissions() {
        var roleId = 1L;
        var permissions =
                given()
                        .when()
                        .get("/WebAPI/role/{roleId}/permissions", roleId)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(ContentType.JSON)
                        .extract()
                        .jsonPath()
                        .getList(".", PermissionDTO.class);

        assertThat(permissions).isNotNull();
    }

    @Test
    public void addPermissionsToRole_WithValidData_AddsPermissions() {
        // Create a role
        var newRole = new RoleDTO(null, "Role For Permissions", false);
        var createdRole =
                givenWithCsrf()
                        .contentType(ContentType.JSON)
                        .body(newRole)
                        .post("/WebAPI/role")
                        .as(RoleDTO.class);

        // Add permissions (using permission IDs 1 and 2)
        givenWithCsrf()
                .when()
                .put("/WebAPI/role/{roleId}/permissions/{permissionIdList}", createdRole.id(), "1+2")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Verify permissions were added
        var permissions =
                given()
                        .when()
                        .get("/WebAPI/role/{roleId}/permissions", createdRole.id())
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath()
                        .getList(".", PermissionDTO.class);

        assertThat(permissions).hasSizeGreaterThanOrEqualTo(2);
        assertThat(permissions).extracting(PermissionDTO::id).contains(1L, 2L);
    }

    @Test
    public void deletePermissionsFromRole_WithValidData_RemovesPermissions() {
        // Create a role and add permissions
        var newRole = new RoleDTO(null, "Role For Permission Removal", false);
        var createdRole =
                givenWithCsrf()
                        .contentType(ContentType.JSON)
                        .body(newRole)
                        .post("/WebAPI/role")
                        .as(RoleDTO.class);

        givenWithCsrf()
                .put("/WebAPI/role/{roleId}/permissions/{permissionIdList}", createdRole.id(), "1+2");

        // Remove permission 1
        givenWithCsrf()
                .when()
                .delete("/WebAPI/role/{roleId}/permissions/{permissionIdList}", createdRole.id(), "1")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Verify permission was removed
        var permissions =
                given()
                        .when()
                        .get("/WebAPI/role/{roleId}/permissions", createdRole.id())
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath()
                        .getList(".", PermissionDTO.class);

        assertThat(permissions).extracting(PermissionDTO::id).doesNotContain(1L);
    }

    @Test
    public void getRoleUsers_WithValidRoleId_ReturnsUsers() {
        var roleId = 1L;
        var users =
                given()
                        .when()
                        .get("/WebAPI/role/{roleId}/users", roleId)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(ContentType.JSON)
                        .extract()
                        .jsonPath()
                        .getList(".", UserDTO.class);

        assertThat(users).isNotNull();
    }

    @Test
    public void addUsersToRole_WithValidData_AddsUsers() {
        // Create a role
        var newRole = new RoleDTO(null, "Role For Users", false);
        var createdRole =
                givenWithCsrf()
                        .contentType(ContentType.JSON)
                        .body(newRole)
                        .post("/WebAPI/role")
                        .as(RoleDTO.class);

        // Add anonymous user to the role
        givenWithCsrf()
                .when()
                .put("/WebAPI/role/{roleId}/users/{userIdList}", createdRole.id(), "1")
                .then()
                .statusCode(is(HttpStatus.OK.value()));
    }

    @Test
    public void removeRoleFromUsers_WithValidData_RemovesRole() {
        // Create a role
        var newRole = new RoleDTO(null, "Role For User Removal", false);
        var createdRole =
                givenWithCsrf()
                        .contentType(ContentType.JSON)
                        .body(newRole)
                        .post("/WebAPI/role")
                        .as(RoleDTO.class);

        // Add anonymous user to the role
        givenWithCsrf()
            .when()
            .put("/WebAPI/role/{roleId}/users/{userIdList}", createdRole.id(), "1")
            .then()
            .statusCode(is(HttpStatus.OK.value()));


        // Remove anonymous user from the role
        givenWithCsrf()
            .when()
            .delete("/WebAPI/role/{roleId}/users/{userIdList}", createdRole.id(), "1")
            .then()
            .statusCode(is(HttpStatus.OK.value()));
    }
}
