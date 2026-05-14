package com.project.artconnect.persistence;

import com.project.artconnect.dao.ExhibitionDao;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JdbcExhibitionDao — mis à jour pour inclure la colonne theme.
 * Lit depuis la vue vue_programme_expositions (qui joint EXPOSITION et GALERIE).
 */
public class JdbcExhibitionDao implements ExhibitionDao {

    // Lecture via la vue (inclut maintenant theme)
    private static final String FIND_ALL =
            "SELECT e.id_exposition, e.titre, e.theme, e.date_debut, e.date_fin, " +
            "       e.description, g.nom AS nom_galerie, g.adresse " +
            "FROM EXPOSITION e " +
            "LEFT JOIN GALERIE g ON g.id_exposition = e.id_exposition " +
            "ORDER BY e.date_debut DESC";

    private static final String SAVE =
            "INSERT INTO EXPOSITION (titre, theme, date_debut, date_fin, description) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE =
            "UPDATE EXPOSITION " +
            "SET theme=?, date_debut=?, date_fin=?, description=? " +
            "WHERE titre=?";

    private static final String DELETE =
            "DELETE FROM EXPOSITION WHERE titre=?";

    @Override
    public List<Exhibition> findAll() {
        List<Exhibition> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findAll() expositions", e);
        }
        return list;
    }

    @Override
    public void save(Exhibition exhibition) {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SAVE)) {
            conn.setAutoCommit(false);
            try {
                ps.setString(1, exhibition.getTitle());
                ps.setString(2, exhibition.getTheme());
                ps.setDate  (3, Date.valueOf(exhibition.getStartDate()));
                ps.setDate  (4, Date.valueOf(exhibition.getEndDate()));
                ps.setString(5, exhibition.getDescription());
                ps.executeUpdate();
                // Trigger trg_verif_dates se déclenche automatiquement ici
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur save() exposition : " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Exhibition exhibition) {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            conn.setAutoCommit(false);
            try {
                ps.setString(1, exhibition.getTheme());
                ps.setDate  (2, Date.valueOf(exhibition.getStartDate()));
                ps.setDate  (3, Date.valueOf(exhibition.getEndDate()));
                ps.setString(4, exhibition.getDescription());
                ps.setString(5, exhibition.getTitle());
                int rows = ps.executeUpdate();
                if (rows == 0) throw new RuntimeException("Exposition introuvable : " + exhibition.getTitle());
                // Trigger trg_audit_exposition se déclenche automatiquement ici
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur update() exposition", e);
        }
    }

    @Override
    public void delete(String title) {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {
            conn.setAutoCommit(false);
            try {
                ps.setString(1, title);
                int rows = ps.executeUpdate();
                if (rows == 0) throw new RuntimeException("Exposition introuvable : " + title);
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur delete() exposition : " + title, e);
        }
    }

    private Exhibition mapRow(ResultSet rs) throws SQLException {
        Exhibition e = new Exhibition();
        e.setTitle      (rs.getString("titre"));
        e.setTheme      (rs.getString("theme"));       // nouveau champ
        e.setStartDate  (rs.getDate("date_debut").toLocalDate());
        e.setEndDate    (rs.getDate("date_fin").toLocalDate());
        e.setDescription(rs.getString("description"));

        String galerieNom = rs.getString("nom_galerie");
        if (galerieNom != null) {
            Gallery g = new Gallery();
            g.setName   (galerieNom);
            g.setAddress(rs.getString("adresse"));
            e.setGallery(g);
        }
        return e;
    }
}
