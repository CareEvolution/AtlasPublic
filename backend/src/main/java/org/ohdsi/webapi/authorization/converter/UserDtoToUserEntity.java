package org.ohdsi.webapi.authorization.converter;

import org.ohdsi.webapi.authorization.dto.UserDTO;
import org.ohdsi.webapi.authorization.entity.UserEntity;

public class UserDtoToUserEntity {
    public static UserEntity convert(UserDTO userDTO) {
        var userEntity = new UserEntity();
        userEntity.setId(userDTO.id());
        userEntity.setLogin(userDTO.login());
        userEntity.setName(userDTO.name());
        return userEntity;
    }
}
