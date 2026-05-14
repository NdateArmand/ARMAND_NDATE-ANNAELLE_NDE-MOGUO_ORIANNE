package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JdbcArtworkDao — utilise la colonne id_artiste dans OEUVRE
 * (ajoutée via migration_oeuvre_id_artiste.sql).
 * Toutes les oeuvres sont visibles quelle que soit leur relation artiste.
 */
public class JdbcArtworkDao implements ArtworkDao {

    private static final String FIND_ALL =
            "SELECT o.id_oeuvre, o.titre, o.annee_creation, o.type, o.support, " +
            "       o.description, o.prix, o.statut, a.nom AS artiste_nom " +
            "FROM OEUVRE o " +
            "LEFT JOIN ARTISTE a ON a.id_artiste = o.id_artiste " +
            "ORDER BY o.titre";

    private static final String FIND_BY_ARTIST =
            "SELECT o.id_oeuvre, o.titre, o.annee_creation, o.type, o.support, " +
            "       o.description, o.prix, o.statut, a.nom AS artiste_nom " +
            "FROM OEUVRE o " +
            "INNER JOIN ARTISTE a ON a.id_artiste = o.id_artiste " +
            "WHERE a.nom = ? ORDER BY o.titre";

    private static final String SAVE =
            "INSERT INTO OEUVRE (titre, annee_creation, type, support, description, prix, statut, id_artiste) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, " +
            "(SELECT id_artiste FROM ARTISTE WHERE nom=? LIMIT 1))";

    // UPDATE avec possibilité de changer le titre
    // Paramètres : nouveau_titre, annee, type, support, desc, prix, statut, ancien_titre
    private static final String UPDATE =
            "UPDATE OEUVRE SET titre=?, annee_creation=?, type=?, support=?, description=?, prix=?, statut=?, " +
            "id_artiste=(SELECT id_artiste FROM ARTISTE WHERE nom=? LIMIT 1) " +
            "WHERE titre=?";

    private static final String DELETE = "DELETE FROM OEUVRE WHERE titre=?";

    @Override
    public List<Artwork> findAll() {
        List<Artwork> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findAll() oeuvres", e);
        }
        return list;
    }

    @Override
    public void save(Artwork artwork) {
        String artistName = artwork.getArtist() != null ? artwork.getArtist().getName() : null;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SAVE)) {
            conn.setAutoCommit(false);
            try {
                ps.setString(1, artwork.getTitle());
                ps.setObject(2, artwork.getCreationYear(), Types.INTEGER);
                ps.setString(3, artwork.getType());
                ps.setString(4, artwork.getMedium());
                ps.setString(5, artwork.getDescription());
                ps.setDouble(6, artwork.getPrice());
                ps.setString(7, artwork.getStatus() != null ? artwork.getStatus().name() : "FOR_SALE");
                ps.setString(8, artistName); // null si pas d'artiste
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur save() oeuvre : " + artwork.getTitle(), e);
        }
    }

    @Override
    public void update(Artwork artwork) {
        update(artwork, artwork.getTitle());
    }

    /** Update avec ancien titre comme critère WHERE */
    public void update(Artwork artwork, String oldTitle) {
        String artistName = artwork.getArtist() != null ? artwork.getArtist().getName() : null;
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            conn.setAutoCommit(false);
            try {
                ps.setString(1, artwork.getTitle());   // nouveau titre
                ps.setObject(2, artwork.getCreationYear(), Types.INTEGER);
                ps.setString(3, artwork.getType());
                ps.setString(4, artwork.getMedium());
                ps.setString(5, artwork.getDescription());
                ps.setDouble(6, artwork.getPrice());
                ps.setString(7, artwork.getStatus() != null ? artwork.getStatus().name() : "FOR_SALE");
                ps.setString(8, artistName);           // sous-SELECT id_artiste WHERE nom=?
                ps.setString(9, oldTitle);             // WHERE titre=ancien_titre
                int rows = ps.executeUpdate();
                if (rows == 0) throw new RuntimeException("Oeuvre introuvable : " + oldTitle);
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur update() oeuvre", e);
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
                if (rows == 0) throw new RuntimeException("Oeuvre introuvable : " + title);
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur delete() oeuvre : " + title, e);
        }
    }

    @Override
    public List<Artwork> findByArtistName(String artistName) {
        List<Artwork> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ARTIST)) {
            ps.setString(1, artistName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByArtistName() : " + artistName, e);
        }
        return list;
    }

    private Artwork mapRow(ResultSet rs) throws SQLException {
        Artwork a = new Artwork();
        a.setTitle       (rs.getString("titre"));
        a.setCreationYear(rs.getObject("annee_creation") != null ? rs.getInt("annee_creation") : null);
        a.setType        (rs.getString("type"));
        a.setMedium      (rs.getString("support"));
        a.setDescription (rs.getString("description"));
        a.setPrice       (rs.getDouble("prix"));
        String statut = rs.getString("statut");
        if (statut != null) {
            a.setStatus(switch (statut.toUpperCase()) {
                case "SOLD",      "VENDU"            -> Artwork.Status.SOLD;
                case "EXHIBITED", "EXPOSÉ", "EXPOSE" -> Artwork.Status.EXHIBITED;
                default -> Artwork.Status.FOR_SALE;
            });
        }
        String artisteNom = rs.getString("artiste_nom");
        if (artisteNom != null) {
            Artist artist = new Artist();
            artist.setName(artisteNom);
            a.setArtist(artist);
        }
        return a;
    }
}
