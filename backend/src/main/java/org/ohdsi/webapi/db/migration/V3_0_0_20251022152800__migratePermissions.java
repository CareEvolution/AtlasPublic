package org.ohdsi.webapi.db.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.ohdsi.webapi.authorization.Permissions;
import org.ohdsi.webapi.authorization.dto.PermissionDTO;
import org.ohdsi.webapi.authorization.permissionmapping.UrlPermissionMapper;
import org.springframework.stereotype.Component;

@Component
public class V3_0_0_20251022152800__migratePermissions extends BaseJavaMigration {
    private final UrlPermissionMapper urlPermissionMapper = new UrlPermissionMapper();

    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();

        // Create a map of all of the new permissions indexed by their value
        var newPermissions = getNewPermissionsMap();
        long nextPermissionId = newPermissions.size() + 1;

        // Map from old permission ID to new permission values
        var oldToNewPermissionMap = new HashMap<Long, List<String>>();
        var permissionSelectSql = "SELECT p.id, p.value FROM sec_permission p";
        try (PreparedStatement statement = connection.prepareStatement(permissionSelectSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Long id = resultSet.getLong("id");
                String value = resultSet.getString("value");
                var newPermissionValues = urlPermissionMapper.getMappedPermission(value);
                for (String newPermissionValue : newPermissionValues) {
                    oldToNewPermissionMap.computeIfAbsent(id, k -> new ArrayList<>()).add(newPermissionValue);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Migration failed while mapping old permissions to new permissions", e);
        }

        // Roles
        var roleSelectSql = "SELECT r.id, r.name, r.system_role FROM sec_role r";
        var prefix = "Source user (";
        HashMap<Long, Long> sourceRoleToPermissionIdMap = new HashMap<>();
        HashMap<Long, List<Long>> roleToOldPermissionIdsMap = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(roleSelectSql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Long roleId = resultSet.getLong("id");
                String name = resultSet.getString("name");
                Boolean isSystemRole = resultSet.getBoolean("system_role");

                if (name.startsWith(prefix)) {
                    // For roles associated with sources, add read permission associated with the source
                    var sourceName = name.replace(prefix, "").replace(")", "");
                    var readPermission = String.format("source:%s:read", sourceName);
                    newPermissions.put(
                            readPermission,
                            new PermissionDTO(nextPermissionId, readPermission, String.format("Read source %s", sourceName)));
                    sourceRoleToPermissionIdMap.put(roleId, nextPermissionId);
                    nextPermissionId++;
                } else if (isSystemRole) {
                    // For non-source system roles, get their old permissions
                    var rolePermissionSelectSql = "SELECT rp.permission_id FROM sec_role_permission rp WHERE rp.role_id = ?";
                    try (PreparedStatement rolePermStatement = connection.prepareStatement(rolePermissionSelectSql)) {
                        rolePermStatement.setLong(1, roleId);
                        try (ResultSet rolePermResultSet = rolePermStatement.executeQuery()) {
                            while (rolePermResultSet.next()) {
                                Long permId = rolePermResultSet.getLong("permission_id");
                                roleToOldPermissionIdsMap.computeIfAbsent(roleId, k -> new ArrayList<>()).add(permId);
                            }
                        }
                    }

                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Migration failed while getting role permissions", e);
        }

        // Clear out the role_permissions table
        var clearRolePermsSql = "DELETE FROM sec_role_permission";
        try (PreparedStatement clearRolePermsStatement = connection.prepareStatement(clearRolePermsSql)) {
            clearRolePermsStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Migration failed while clearing role_permissions table", e);
        }

        // Clear out the permissions table
        var clearPermsSql = "DELETE FROM sec_permission";
        try (PreparedStatement clearPermsStatement = connection.prepareStatement(clearPermsSql)) {
            clearPermsStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Migration failed while clearing permissions table", e);
        }

        // Populate the permissions table with the new permissions
        var insertPermSql = "INSERT INTO sec_permission (id, value, description) VALUES (?, ?, ?)";
        try (PreparedStatement insertPermStatement = connection.prepareStatement(insertPermSql)) {
            for (PermissionDTO permissionDTO : newPermissions.values()) {
                insertPermStatement.setLong(1, permissionDTO.id());
                insertPermStatement.setString(2, permissionDTO.permission());
                insertPermStatement.setString(3, permissionDTO.description());
                insertPermStatement.addBatch();
            }
            insertPermStatement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Migration failed while inserting new permissions", e);
        }

        // Re-populate the role_permissions table with the new mappings
        var insertRolePermSql = "INSERT INTO sec_role_permission (role_id, permission_id) VALUES (?, ?)";
        try (PreparedStatement insertRolePermStatement = connection.prepareStatement(insertRolePermSql)) {
            for (Map.Entry<Long, Long> entry : sourceRoleToPermissionIdMap.entrySet()) {
                Long roleId = entry.getKey();
                Long permissionId = entry.getValue();
                insertRolePermStatement.setLong(1, roleId);
                insertRolePermStatement.setLong(2, permissionId);
                insertRolePermStatement.addBatch();
            }
            insertRolePermStatement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Migration failed while inserting new source role permissions", e);
        }

        try (PreparedStatement insertRolePermStatement = connection.prepareStatement(insertRolePermSql)) {
            for (Map.Entry<Long, List<Long>> entry : roleToOldPermissionIdsMap.entrySet()) {
                Long roleId = entry.getKey();
                var newPermissionsForRole = new HashSet<Long>();
                for (Long oldPermissionId : entry.getValue()) {
                    var newPermissionValues = oldToNewPermissionMap.get(oldPermissionId);
                    if (newPermissionValues == null) {
                        continue;
                    }
                    for (String newPermissionValue : newPermissionValues) {
                        var permissionDTO = newPermissions.get(newPermissionValue);
                        if (permissionDTO == null) {
                            continue;
                        }
                        Long permissionId = permissionDTO.id();
                        newPermissionsForRole.add(permissionId);
                    }
                }
                for (Long permissionId : newPermissionsForRole) {
                    insertRolePermStatement.setLong(1, roleId);
                    insertRolePermStatement.setLong(2, permissionId);
                    insertRolePermStatement.addBatch();
                }
            }
            insertRolePermStatement.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Migration failed while inserting new system role permissions", e);
        }

        // Assign the source roles to the anonymous user
        var assignRoleSql = "INSERT INTO sec_user_role (user_id, role_id, origin) VALUES (1, ?, 'SYSTEM')";
        try (PreparedStatement assignRoleStatement = connection.prepareStatement(assignRoleSql)) {
            for (Map.Entry<Long, Long> entry : sourceRoleToPermissionIdMap.entrySet()) {
                Long roleId = entry.getKey();
                assignRoleStatement.setLong(1, roleId);
                assignRoleStatement.addBatch();
            }
            assignRoleStatement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Migration failed while assigning source roles to the anonymous user", e);
        }

    }

    private HashMap<String, PermissionDTO> getNewPermissionsMap() {
        var newPermissions = new HashMap<String, PermissionDTO>();

        newPermissions.put(Permissions.CREATE_COHORT_DEFINITION,new PermissionDTO(1L, Permissions.CREATE_COHORT_DEFINITION, "Create cohort definitions"));
        newPermissions.put(Permissions.DELETE_COHORT_DEFINITION, new PermissionDTO(2L, Permissions.DELETE_COHORT_DEFINITION, "Delete cohort definitions"));
        newPermissions.put(Permissions.READ_COHORT_DEFINITION, new PermissionDTO(3L, Permissions.READ_COHORT_DEFINITION, "Read cohort definitions"));
        newPermissions.put(Permissions.UPDATE_COHORT_DEFINITION, new PermissionDTO(4L, Permissions.UPDATE_COHORT_DEFINITION, "Update cohort definitions"));

        newPermissions.put(Permissions.CREATE_SOURCE, new PermissionDTO(5L, Permissions.CREATE_SOURCE, "Create (add) data sources"));
        newPermissions.put(Permissions.DELETE_SOURCE, new PermissionDTO(6L, Permissions.DELETE_SOURCE, "Delete data sources"));
        newPermissions.put(Permissions.READ_SOURCE, new PermissionDTO(7L, Permissions.READ_SOURCE, "Read data sources"));
        newPermissions.put(Permissions.UPDATE_SOURCE, new PermissionDTO(8L, Permissions.UPDATE_SOURCE, "Update data sources"));

        newPermissions.put(Permissions.CREATE_PERMISSION, new PermissionDTO(9L, Permissions.CREATE_PERMISSION, "Create permissions"));
        newPermissions.put(Permissions.DELETE_PERMISSION, new PermissionDTO(10L, Permissions.DELETE_PERMISSION, "Delete permissions"));
        newPermissions.put(Permissions.READ_PERMISSION, new PermissionDTO(11L, Permissions.READ_PERMISSION, "Read permissions"));
        newPermissions.put(Permissions.UPDATE_PERMISSION, new PermissionDTO(12L, Permissions.UPDATE_PERMISSION, "Update permissions"));

        newPermissions.put(Permissions.CREATE_ROLE, new PermissionDTO(13L, Permissions.CREATE_ROLE, "Create roles"));
        newPermissions.put(Permissions.DELETE_ROLE, new PermissionDTO(14L, Permissions.DELETE_ROLE, "Delete roles"));
        newPermissions.put(Permissions.READ_ROLE, new PermissionDTO(15L, Permissions.READ_ROLE, "Read roles"));
        newPermissions.put(Permissions.UPDATE_ROLE, new PermissionDTO(16L, Permissions.UPDATE_ROLE, "Update roles"));

        newPermissions.put(Permissions.CREATE_TAG, new PermissionDTO(17L, Permissions.CREATE_TAG, "Create tags"));
        newPermissions.put(Permissions.READ_TAG, new PermissionDTO(18L, Permissions.READ_TAG, "Read tags"));
        newPermissions.put(Permissions.UPDATE_TAG, new PermissionDTO(19L, Permissions.UPDATE_TAG, "Update tags"));
        newPermissions.put(Permissions.DELETE_TAG, new PermissionDTO(20L, Permissions.DELETE_TAG, "Delete tags"));

        newPermissions.put(Permissions.READ_USER, new PermissionDTO(21L, Permissions.READ_USER, "Read user profiles"));
        newPermissions.put(Permissions.UPDATE_USER, new PermissionDTO(22L, Permissions.UPDATE_USER, "Update user profiles"));

        newPermissions.put(Permissions.READ_JOB, new PermissionDTO(23L, Permissions.READ_JOB, "Read jobs"));

        newPermissions.put(Permissions.READ_NOTIFICATIONS, new PermissionDTO(24L, Permissions.READ_NOTIFICATIONS, "Read notifications"));
        newPermissions.put(Permissions.UPDATE_NOTIFICATIONS, new PermissionDTO(25L, Permissions.UPDATE_NOTIFICATIONS, "Update notifications"));

        newPermissions.put(Permissions.READ_PERSON, new PermissionDTO(26L, Permissions.READ_PERSON, "Read person profile"));
        newPermissions.put(Permissions.READ_PERSON_DATES, new PermissionDTO(27L, Permissions.READ_PERSON_DATES, "Read person dates"));

        newPermissions.put(Permissions.READ_CONCEPT_SET, new PermissionDTO(28L, Permissions.READ_CONCEPT_SET, "Read concept sets"));
        newPermissions.put(Permissions.UPDATE_CONCEPT_SET, new PermissionDTO(29L, Permissions.UPDATE_CONCEPT_SET, "Update concept sets"));
        newPermissions.put(Permissions.DELETE_CONCEPT_SET, new PermissionDTO(30L, Permissions.DELETE_CONCEPT_SET, "Delete concept sets"));
        newPermissions.put(Permissions.CREATE_CONCEPT_SET, new PermissionDTO(31L, Permissions.CREATE_CONCEPT_SET, "Create concept sets"));

        newPermissions.put(Permissions.CREATE_COHORT_CHARACTERIZATION, new PermissionDTO(32L, Permissions.CREATE_COHORT_CHARACTERIZATION, "Create cohort characterizations"));
        newPermissions.put(Permissions.DELETE_COHORT_CHARACTERIZATION, new PermissionDTO(33L, Permissions.DELETE_COHORT_CHARACTERIZATION, "Delete cohort characterizations"));
        newPermissions.put(Permissions.READ_COHORT_CHARACTERIZATION, new PermissionDTO(34L, Permissions.READ_COHORT_CHARACTERIZATION, "Read cohort characterizations"));
        newPermissions.put(Permissions.UPDATE_COHORT_CHARACTERIZATION, new PermissionDTO(35L, Permissions.UPDATE_COHORT_CHARACTERIZATION, "Update cohort characterizations"));

        newPermissions.put(Permissions.CREATE_FEATURE_ANALYSIS, new PermissionDTO(36L, Permissions.CREATE_FEATURE_ANALYSIS, "Create feature analyses"));
        newPermissions.put(Permissions.DELETE_FEATURE_ANALYSIS, new PermissionDTO(37L, Permissions.DELETE_FEATURE_ANALYSIS, "Delete feature analyses"));
        newPermissions.put(Permissions.READ_FEATURE_ANALYSIS, new PermissionDTO(38L, Permissions.READ_FEATURE_ANALYSIS, "Read feature analyses"));
        newPermissions.put(Permissions.UPDATE_FEATURE_ANALYSIS, new PermissionDTO(39L, Permissions.UPDATE_FEATURE_ANALYSIS, "Update feature analyses"));

        newPermissions.put(Permissions.READ_SENSITIVE_INFO, new PermissionDTO(40L, Permissions.READ_SENSITIVE_INFO, "Read sensitive information"));

        return newPermissions;
    }
}
