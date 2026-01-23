package org.ohdsi.webapi.authorization.service;

import org.ohdsi.webapi.authorization.dto.PermissionDTO;
import org.ohdsi.webapi.authorization.entity.PermissionEntity;
import org.ohdsi.webapi.authorization.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PermissionService {
    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public List<PermissionDTO> getAllPermissions() {
        var permissionEntities = permissionRepository.findAll();

        var permissionDtos = new ArrayList<PermissionDTO>();
        for (var permissionEntity : permissionEntities) {
            var permissionDto = new PermissionDTO(
                    permissionEntity.getId(),
                    permissionEntity.getValue(),
                    permissionEntity.getDescription());
            permissionDtos.add(permissionDto);
        }
        return permissionDtos;
    }

    public PermissionEntity createPermission(String value, String description) {
        var permissionEntity = new PermissionEntity();
        permissionEntity.setValue(value);
        permissionEntity.setDescription(description);
        return permissionRepository.saveAndFlush(permissionEntity);
    }

    public void deletePermission(String value) {
        var permissionEntity = permissionRepository.findByValueIgnoreCase(value);
        if (permissionEntity != null) {
            permissionRepository.delete(permissionEntity);
        }
    }

    public void deletePermission(PermissionEntity permission) {
        permissionRepository.delete(permission);
    }

    public PermissionEntity getPermissionByValue(String value) {
        return permissionRepository.findByValueIgnoreCase(value);
    }

    public void deletePermissionByValue(String value) {
        permissionRepository.deleteByValueIgnoreCase(value);
    }
}
