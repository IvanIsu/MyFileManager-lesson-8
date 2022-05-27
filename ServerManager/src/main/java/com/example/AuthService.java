package com.example;

import lombok.extern.java.Log;

import java.sql.*;
import java.util.logging.Level;

@Log
public class AuthService {
    private static Connection connection;
    private static Statement stmt;

    public static UserInfo getNickByLoginAndPassword(String login, int password){
        String sql = "SELECT id_user, user_name, user_password FROM users WHERE user_login = '" + login + "'";

        ResultSet rs = null;
        try {
            rs = stmt.executeQuery(sql);
            String userID = rs.getString(1);
            String nickName = rs.getString(2);
            int passDb = rs.getInt(3);
            if(password == passDb){
                return new UserInfo(userID,nickName,login,null);
            }

        } catch (SQLException throwables) {
            log.log(Level.WARNING,"Incorrect Login");
            //throwables.printStackTrace();
        }return null;
    }

    public static boolean createNewUserInBase (String login, String nick, int password){

        String query = "INSERT INTO users (user_login, user_name, user_password) VALUES (?, ?, ?);";
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(query);
            ps.setString(1, login);
            ps.setString(2, nick);
            ps.setInt(3, password);
            ps.executeUpdate();
            return true;
        } catch (SQLException throwables) {
            throwables.printStackTrace();

        }
        return false;
    }


    public static void connect(){
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:ServerManager/FileManageUsersDB.db");
            stmt = connection.createStatement();
            System.out.println("Connected to DataBase " + stmt.toString() + " " + connection.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void disconnect(){
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
