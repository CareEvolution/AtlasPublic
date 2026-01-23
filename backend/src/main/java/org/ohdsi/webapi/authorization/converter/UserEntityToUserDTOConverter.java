package org.ohdsi.webapi.authorization.converter;

import org.ohdsi.webapi.authorization.entity.UserEntity;
import org.ohdsi.webapi.authorization.dto.UserDTO;

public class UserEntityToUserDTOConverter {

    public static UserDTO convert(UserEntity userEntity) {
        if (userEntity == null) {
            return null;
        }
        return new UserDTO(userEntity.getId(), userEntity.getName(), userEntity.getLogin());
    }
}
