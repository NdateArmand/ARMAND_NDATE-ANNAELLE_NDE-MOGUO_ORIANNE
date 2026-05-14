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
 * JdbcWorkshopDao — mis à jour pour inclure :
 * - date_atelier → Workshop.date
 * - niveau       → Workshop.level
 * - lieu         → Workshop.location
 */
public class JdbcWorkshopDao implements WorkshopDao {

    private static final String FIND_ALL =
            "SELECT at.id_atelier, at.titre, at.date_atelier, at.duree, " +
                    "       at.participants_max, at.prix, at.description, at.niveau, at.lieu, " +
                    "       a.nom AS artiste_nom " +
                    "FROM ATELIER at " +
                    "LEFT JOIN ARTISTE a ON a.id_atelier = at.id_atelier " +
                    "ORDER BY at.titre";

    private static final String FIND_BY_ID =
            "SELECT at.id_atelier, at.titre, at.date_atelier, at.duree, " +
                    "       at.participants_max, at.prix, at.description, at.niveau, at.lieu, " +
                    "       a.nom AS artiste_nom " +
                    "FROM ATELIER at " +
                    "LEFT JOIN ARTISTE a ON a.id_atelier = at.id_atelier " +
                    "WHERE at.id_atelier=?";

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

    private Workshop mapRow(ResultSet rs) throws SQLException {
        Workshop w = new Workshop();
        w.setTitle          (rs.getString("titre"));
        w.setDurationMinutes(rs.getInt   ("duree"));
        w.setMaxParticipants(rs.getInt   ("participants_max"));
        w.setPrice          (rs.getDouble("prix"));
        w.setDescription    (rs.getString("description"));
        w.setLevel          (rs.getString("niveau"));   // nouvelle colonne
        w.setLocation       (rs.getString("lieu"));     // nouvelle colonne

        // date_atelier → LocalDateTime
        Timestamp ts = rs.getTimestamp("date_atelier");
        if (ts != null) w.setDate(ts.toLocalDateTime());

        // Instructeur
        String nom = rs.getString("artiste_nom");
        if (nom != null) {
            Artist instructor = new Artist();
            instructor.setName(nom);
            w.setInstructor(instructor);
        }
        return w;
    }
}
