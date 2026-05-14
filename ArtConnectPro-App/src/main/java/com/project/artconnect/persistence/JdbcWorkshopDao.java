package com.project.artconnect.persistence;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JdbcWorkshopDao — utilise id_artiste dans ATELIER
 * (ajouté via migration_complete.sql).
 * Tous les ateliers sont visibles quelle que soit la relation artiste.
 */
public class JdbcWorkshopDao implements WorkshopDao {

    private static final String FIND_ALL =
            "SELECT at.id_atelier, at.titre, at.date_atelier, at.duree, " +
            "       at.participants_max, at.prix, at.description, at.niveau, at.lieu, " +
            "       a.nom AS artiste_nom " +
            "FROM ATELIER at " +
            "LEFT JOIN ARTISTE a ON a.id_artiste = at.id_artiste " +
            "ORDER BY at.titre";

    private static final String FIND_BY_ID =
            "SELECT at.id_atelier, at.titre, at.date_atelier, at.duree, " +
            "       at.participants_max, at.prix, at.description, at.niveau, at.lieu, " +
            "       a.nom AS artiste_nom " +
            "FROM ATELIER at " +
            "LEFT JOIN ARTISTE a ON a.id_artiste = at.id_artiste " +
            "WHERE at.id_atelier=?";

    private static final String SAVE =
            "INSERT INTO ATELIER (titre, date_atelier, duree, participants_max, prix, niveau, lieu, id_artiste) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, " +
            "(SELECT id_artiste FROM ARTISTE WHERE nom=? LIMIT 1))";

    // UPDATE avec possibilité de changer le titre
    private static final String UPDATE =
            "UPDATE ATELIER SET titre=?, date_atelier=?, duree=?, participants_max=?, prix=?, niveau=?, lieu=?, " +
            "id_artiste=(SELECT id_artiste FROM ARTISTE WHERE nom=? LIMIT 1) " +
            "WHERE titre=?";

    private static final String DELETE = "DELETE FROM ATELIER WHERE titre=?";

    @Override
    public List<Workshop> findAll() {
        List<Workshop> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findAll() ateliers", e);
        }
        return list;
    }

    @Override
    public Optional<Workshop> findById(Long id) {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findById() atelier id=" + id, e);
        }
        return Optional.empty();
    }

    public void save(Workshop w) {
        String instructorName = w.getInstructor() != null ? w.getInstructor().getName() : null;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SAVE)) {
            conn.setAutoCommit(false);
            try {
                ps.setString   (1, w.getTitle());
                ps.setTimestamp(2, w.getDate() != null ? Timestamp.valueOf(w.getDate()) : null);
                ps.setInt      (3, w.getDurationMinutes());
                ps.setInt      (4, w.getMaxParticipants());
                ps.setDouble   (5, w.getPrice());
                ps.setString   (6, w.getLevel());
                ps.setString   (7, w.getLocation());
                ps.setString   (8, instructorName); // null si pas d'instructeur
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur save() atelier : " + w.getTitle(), e);
        }
    }

    public void update(Workshop w) {
        update(w, w.getTitle());
    }

    /** Update avec ancien titre comme critère WHERE */
    public void update(Workshop w, String oldTitle) {
        String instructorName = w.getInstructor() != null ? w.getInstructor().getName() : null;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            conn.setAutoCommit(false);
            try {
                ps.setString   (1, w.getTitle());   // nouveau titre
                ps.setTimestamp(2, w.getDate() != null ? Timestamp.valueOf(w.getDate()) : null);
                ps.setInt      (3, w.getDurationMinutes());
                ps.setInt      (4, w.getMaxParticipants());
                ps.setDouble   (5, w.getPrice());
                ps.setString   (6, w.getLevel());
                ps.setString   (7, w.getLocation());
                ps.setString   (8, instructorName); // sous-SELECT id_artiste WHERE nom=?
                ps.setString   (9, oldTitle);       // WHERE titre=ancien_titre
                int rows = ps.executeUpdate();
                if (rows == 0) throw new RuntimeException("Atelier introuvable : " + oldTitle);
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur update() atelier", e);
        }
    }

    public void delete(String title) {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {
            conn.setAutoCommit(false);
            try {
                ps.setString(1, title);
                int rows = ps.executeUpdate();
                if (rows == 0) throw new RuntimeException("Atelier introuvable : " + title);
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur delete() atelier : " + title, e);
        }
    }

    private Workshop mapRow(ResultSet rs) throws SQLException {
        Workshop w = new Workshop();
        w.setTitle          (rs.getString("titre"));
        w.setDurationMinutes(rs.getInt   ("duree"));
        w.setMaxParticipants(rs.getInt   ("participants_max"));
        w.setPrice          (rs.getDouble("prix"));
        w.setDescription    (rs.getString("description"));
        w.setLevel          (rs.getString("niveau"));
        w.setLocation       (rs.getString("lieu"));
        Timestamp ts = rs.getTimestamp("date_atelier");
        if (ts != null) w.setDate(ts.toLocalDateTime());
        String nom = rs.getString("artiste_nom");
        if (nom != null) {
            Artist instructor = new Artist();
            instructor.setName(nom);
            w.setInstructor(instructor);
        }
        return w;
    }
}
