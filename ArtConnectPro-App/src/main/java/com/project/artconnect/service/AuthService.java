package com.project.artconnect.service;

import com.project.artconnect.model.Session;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Vérifie les credentials dans les 3 tables existantes.
 * Pas de table UTILISATEUR séparée.
 */
public class AuthService {

    public boolean login(String email, String password) {
        if (tryLoginArtiste(email, password))      return true;
        if (tryLoginMembre(email, password))       return true;
        if (tryLoginOrganisateur(email, password)) return true;
        return false;
    }

    private boolean tryLoginArtiste(String email, String password) {
        String sql = "SELECT id_artiste, email_contact, nom FROM artiste " +
                     "WHERE email_contact = ? AND mot_passe = SHA2(?, 256)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Session.getInstance().loginArtiste(
                            rs.getInt("id_artiste"),
                            rs.getString("email_contact"),
                            rs.getString("nom"));
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur login artiste : " + e.getMessage());
        }
        return false;
    }

    private boolean tryLoginMembre(String email, String password) {
        String sql = "SELECT id_membre, email, nom FROM membre_communaute " +
                     "WHERE email = ? AND mot_passe = SHA2(?, 256)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Session.getInstance().loginMembre(
                            rs.getInt("id_membre"),
                            rs.getString("email"),
                            rs.getString("nom"));
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur login membre : " + e.getMessage());
        }
        return false;
    }

    private boolean tryLoginOrganisateur(String email, String password) {
        String sql = "SELECT id_organisateur, email, nomO FROM organisateur " +
                     "WHERE email = ? AND mot_passe = SHA2(?, 256)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Session.getInstance().loginOrganisateur(
                            rs.getInt("id_organisateur"),
                            rs.getString("email"),
                            rs.getString("nomO"));
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur login organisateur : " + e.getMessage());
        }
        return false;
    }

    public void logout() {
        Session.getInstance().logout();
    }
}
