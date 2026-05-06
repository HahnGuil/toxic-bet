package br.com.hahn.toxicbet.application.mapper;

import br.com.hahn.toxicbet.domain.model.Users;
import br.com.hahn.toxicbet.model.UserResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private UserMapper(){}

    public Users toEntity(String userName, String email) {
        if (userName == null || userName.isBlank() || email == null || email.isBlank()) return null;
        Users user = new Users();
        user.setName(extractReadableUserName(userName));
        user.setEmail(email);
        return user;
    }

    public UserResponseDTO toDTO(Users user) {
        if (user == null) return null;
        UserResponseDTO resp = new UserResponseDTO();
        resp.setUserEmail(user.getEmail());
        resp.setUserName(extractReadableUserName(user.getName()));
        return resp;
    }

    private String extractReadableUserName(String rawUserName) {
        String trimmedName = rawUserName == null ? "" : rawUserName.trim();
        if (trimmedName.isBlank() || !trimmedName.startsWith("{")) {
            return rawUserName;
        }

        try {
            JsonNode node = OBJECT_MAPPER.readTree(trimmedName);
            if (node.hasNonNull("userName")) {
                return node.get("userName").asText();
            }
            if (node.hasNonNull("name")) {
                return node.get("name").asText();
            }
        } catch (Exception ignored) {
            return rawUserName;
        }

        return rawUserName;
    }
}
