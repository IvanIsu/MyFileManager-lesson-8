package dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class BasicRequest implements Serializable {

    private String token;

    public BasicRequest(String token) {
        this.token = token;
    }
}
