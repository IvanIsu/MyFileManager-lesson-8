package dto;

import lombok.Data;

@Data public class RegRequest extends BasicRequest {

    private String login;
    private int password;
    private String nickName;

    public RegRequest(String token, String login, String nickName, int password) {
        super(token);
        this.login = login;
        this.password = password;
        this.nickName = nickName;
    }


}
