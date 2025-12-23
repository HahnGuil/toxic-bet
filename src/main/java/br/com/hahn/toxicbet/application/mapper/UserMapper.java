package br.com.hahn.toxicbet.application.mapper;

import br.com.hahn.toxicbet.domain.model.enums.SuccessMessages;
import br.com.hahn.toxicbet.domain.model.Users;
import br.com.hahn.toxicbet.model.UserRequestDTO;
import br.com.hahn.toxicbet.model.UserResponseDTO;
import org.springframework.stereotype.Component;


/**
 * Mapper class responsible for converting between User-related DTOs and entity objects.
 * This class provides methods to map data from UserRequestDTO to Users entity,
 * from Users entity to UserRequestDTO, and from Users entity to UserResponseDTO.
 *
 * @author HahnGuil
 */
@Component
public class UserMapper {

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @author HahnGuil
     */
    private UserMapper(){}


    /**
     * Converts a UserRequestDTO object to a Users entity.
     *
     * @author HahnGuil
     * @param dto the UserRequestDTO object to be converted
     * @return the corresponding Users entity, or null if the input is null
     */
    public Users toEntity(UserRequestDTO dto) {
        if (dto == null) return null;
        Users user = new Users();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        return user;
    }

    /**
     * Converts a Users entity to a UserRequestDTO object.
     *
     * @author HahnGuil
     * @param user the Users entity to be converted
     * @return the corresponding UserRequestDTO object, or null if the input is null
     */
    public UserRequestDTO toDTO(Users user) {
        if (user == null) return null;
        UserRequestDTO dto = new UserRequestDTO();
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        return dto;
    }

    /**
     * Converts a Users entity to a UserResponseDTO object.
     *
     * @author HahnGuil
     * @param user the Users entity to be converted
     * @return the corresponding UserResponseDTO object, or null if the input is null
     */
    public UserResponseDTO toResponseDTO(Users user) {
        if (user == null) return null;
        UserResponseDTO resp = new UserResponseDTO();
        resp.setMessage(SuccessMessages.REGISTER_USER.getMessage());
        return resp;
    }
}