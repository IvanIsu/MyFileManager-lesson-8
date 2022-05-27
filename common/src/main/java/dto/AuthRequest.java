package dto;

import lombok.Data;

@Data public class AuthRequest extends BasicRequest {

    private String login;
    private int password;

    public AuthRequest(String token, String login, int password) {
        super(token);
        this.login = login;
        this.password = password;
    }

}
