package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JdbcArtistDao — mis à jour pour inclure :
 * - bio, website, social_media (nouvelles colonnes dans ARTISTE)
 * - disciplines (via jointure ARTISTE_DISCIPLINE → DISCIPLINE)
 */
public class JdbcArtistDao implements ArtistDao {

    private static final String FIND_ALL =
            "SELECT id_artiste, nom, bio, annee_naissance, email_contact, " +
                    "       telephone, ville, website, social_media " +
                    "FROM ARTISTE ORDER BY nom";

    private static final String FIND_DISCIPLINES =
            "SELECT d.nom FROM DISCIPLINE d " +
                    "JOIN ARTISTE_DISCIPLINE ad ON ad.id_discipline = d.id_discipline " +
                    "WHERE ad.id_artiste = ?";

    private static final String SAVE =
            "INSERT INTO ARTISTE (nom, bio, annee_naissance, email_contact, telephone, ville, website, social_media) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    // UPDATE avec possibilité de changer le nom
    // Paramètres: nouveau_nom, bio, annee, email, tel, ville, website, social, ancien_nom
    private static final String UPDATE =
            "UPDATE ARTISTE SET nom=?, bio=?, annee_naissance=?, email_contact=?, " +
            "telephone=?, ville=?, website=?, social_media=? WHERE nom=?";

    private static final String DELETE = "DELETE FROM ARTISTE WHERE nom=?";

    private static final String FIND_BY_CITY =
            "SELECT id_artiste, nom, bio, annee_naissance, email_contact, " +
                    "       telephone, ville, website, social_media " +
                    "FROM ARTISTE WHERE ville=? ORDER BY nom";

    @Override
    public List<Artist> findAll() {
        List<Artist> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs, conn));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findAll() artistes", e);
        }
        return list;
    }

    @Override
    public void save(Artist artist) {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SAVE)) {
            conn.setAutoCommit(false);
            try {
                ps.setString(1, artist.getName());
                ps.setString(2, artist.getBio());
                ps.setObject(3, artist.getBirthYear(), Types.INTEGER);
                ps.setString(4, artist.getContactEmail());
                ps.setString(5, artist.getPhone());
                ps.setString(6, artist.getCity());
                ps.setString(7, artist.getWebsite());
                ps.setString(8, artist.getSocialMedia());
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur save() artiste : " + artist.getName(), e);
        }
    }

    @Override
    public void update(Artist artist) {
        update(artist, artist.getName());
    }

    /** Update avec ancien nom comme critère WHERE */
    public void update(Artist artist, String oldName) {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            conn.setAutoCommit(false);
            try {
                ps.setString(1, artist.getName());         // nouveau nom
                ps.setString(2, artist.getBio());
                ps.setObject(3, artist.getBirthYear(), Types.INTEGER);
                ps.setString(4, artist.getContactEmail());
                ps.setString(5, artist.getPhone());
                ps.setString(6, artist.getCity());
                ps.setString(7, artist.getWebsite());
                ps.setString(8, artist.getSocialMedia());
                ps.setString(9, oldName);                  // WHERE nom=ancien_nom
                int rows = ps.executeUpdate();
                if (rows == 0) throw new RuntimeException("Artiste introuvable : " + oldName);
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur update() artiste", e);
        }
    }

    @Override
    public void delete(String artistName) {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {
            conn.setAutoCommit(false);
            try {
                ps.setString(1, artistName);
                int rows = ps.executeUpdate();
                if (rows == 0) throw new RuntimeException("Artiste introuvable : " + artistName);
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur delete() artiste : " + artistName, e);
        }
    }

    @Override
    public List<Artist> findByCity(String city) {
        List<Artist> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_CITY)) {
            ps.setString(1, city);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs, conn));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByCity() : " + city, e);
        }
        return list;
    }

    private Artist mapRow(ResultSet rs, Connection conn) throws SQLException {
        Artist a = new Artist();
        int idArtiste = rs.getInt("id_artiste");
        a.setName        (rs.getString("nom"));
        a.setBio         (rs.getString("bio"));
        a.setBirthYear   (rs.getObject("annee_naissance") != null ? rs.getInt("annee_naissance") : null);
        a.setContactEmail(rs.getString("email_contact"));
        a.setPhone       (rs.getString("telephone"));
        a.setCity        (rs.getString("ville"));
        a.setWebsite     (rs.getString("website"));
        a.setSocialMedia (rs.getString("social_media"));
        a.setActive(true);

        // Charger les disciplines depuis ARTISTE_DISCIPLINE
        try (PreparedStatement psDis = conn.prepareStatement(FIND_DISCIPLINES)) {
            psDis.setInt(1, idArtiste);
            try (ResultSet rsDis = psDis.executeQuery()) {
                while (rsDis.next()) {
                    a.getDisciplines().add(new Discipline(rsDis.getString("nom")));
                }
            }
        }
        return a;
    }
}
