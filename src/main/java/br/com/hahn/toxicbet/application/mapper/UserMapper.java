package br.com.hahn.toxicbet.application.mapper;

import br.com.hahn.toxicbet.domain.model.Users;
import br.com.hahn.toxicbet.model.UserResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private UserMapper(){}

    public Users toEntity(String userName, String email) {
        if (userName == null || userName.isBlank() || email == null || email.isBlank()) return null;
        Users user = new Users();
        user.setName(userName);
        user.setEmail(email);
        return user;
    }

    public UserResponseDTO toDTO(Users user) {
        if (user == null) return null;
        UserResponseDTO resp = new UserResponseDTO();
        resp.setUserEmail(user.getEmail());
        resp.setUserName(user.getName());
        return resp;
    }
}