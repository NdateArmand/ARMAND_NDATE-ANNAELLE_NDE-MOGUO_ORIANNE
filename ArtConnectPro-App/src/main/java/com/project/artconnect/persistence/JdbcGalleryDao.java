package com.project.artconnect.persistence;

import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Implémentation JDBC de GalleryDao — table GALERIE. */
public class JdbcGalleryDao implements GalleryDao {

    private static final String FIND_ALL =
            "SELECT id_galerie, nom, adresse, nom_proprietaire, heure_ouverture, heure_fermeture " +
            "FROM GALERIE ORDER BY nom";

    private static final String FIND_BY_ID =
            "SELECT id_galerie, nom, adresse, nom_proprietaire, heure_ouverture, heure_fermeture " +
            "FROM GALERIE WHERE id_galerie=?";

    @Override
    public List<Gallery> findAll() {
        List<Gallery> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findAll() galeries", e);
        }
        return list;
    }

    @Override
    public Optional<Gallery> findById(Long id) {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findById() galerie id=" + id, e);
        }
        return Optional.empty();
    }

    private Gallery mapRow(ResultSet rs) throws SQLException {
        Gallery g = new Gallery();
        g.setName        (rs.getString("nom"));
        g.setAddress     (rs.getString("adresse"));
        g.setOwnerName   (rs.getString("nom_proprietaire"));
        Time ouv  = rs.getTime("heure_ouverture");
        Time ferm = rs.getTime("heure_fermeture");
        if (ouv != null && ferm != null)
            g.setOpeningHours(ouv.toLocalTime() + " - " + ferm.toLocalTime());
        return g;
    }
}
