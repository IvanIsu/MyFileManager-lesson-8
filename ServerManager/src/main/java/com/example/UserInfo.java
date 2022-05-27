package com.example;


import lombok.Data;

import java.nio.file.Path;

@Data
public class UserInfo {

    private String userID;
    private String userName;
    private String userLogin;
    private String sessionToken;
    private Path userPathRoot;

    public UserInfo(String userID, String userName, String userLogin, String sessionToken ) {
        this.userID = userID;
        this.userName = userName;
        this.userLogin = userLogin;
        this.sessionToken = sessionToken;
    }
}
