package br.com.hahn.toxicbet.application.mapper;

import br.com.hahn.toxicbet.domain.model.enums.SuccessMessages;
import br.com.hahn.toxicbet.domain.model.Users;
import br.com.hahn.toxicbet.model.UserRequestDTO;
import br.com.hahn.toxicbet.model.UserResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private UserMapper(){}

    public Users toEntity(UserRequestDTO dto) {
        if (dto == null) return null;
        Users user = new Users();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        return user;
    }
    
    public UserResponseDTO toResponseDTO(Users user) {
        if (user == null) return null;
        UserResponseDTO resp = new UserResponseDTO();
        resp.setMessage(SuccessMessages.REGISTER_USER.getMessage());
        return resp;
    }
}