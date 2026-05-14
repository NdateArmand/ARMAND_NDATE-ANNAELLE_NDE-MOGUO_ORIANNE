package com.project.artconnect.config;

/**
 * Database configuration constants.
 * Mettez à jour USER et PASSWORD selon votre environnement MySQL.
 */
public class DatabaseConfig {
    public static final String URL      = "jdbc:mysql://localhost:3306/artconnect"
                                        + "?useSSL=false"
                                        + "&allowPublicKeyRetrieval=true"
                                        + "&serverTimezone=Europe/Paris"
                                        + "&characterEncoding=UTF-8";
    public static final String USER     = "root";
    public static final String PASSWORD = "mysql"; // À changer
}
