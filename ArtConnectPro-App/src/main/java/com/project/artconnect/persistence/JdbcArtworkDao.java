package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation JDBC de ArtworkDao — table OEUVRE.
 */
public class JdbcArtworkDao implements ArtworkDao {

    private static final String FIND_ALL =
            "SELECT o.titre, o.annee_creation, o.type, o.support, " +
            "       o.description, o.prix, o.statut, a.nom AS artiste_nom " +
            "FROM OEUVRE o " +
            "LEFT JOIN ARTISTE a ON a.id_oeuvre = o.id_oeuvre " +
            "ORDER BY o.titre";

    private static final String SAVE =
            "INSERT INTO OEUVRE (titre, annee_creation, type, support, description, prix, statut) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE =
            "UPDATE OEUVRE SET annee_creation=?, type=?, support=?, description=?, prix=?, statut=? " +
            "WHERE titre=?";

    private static final String DELETE = "DELETE FROM OEUVRE WHERE titre=?";

    private static final String FIND_BY_ARTIST =
            "SELECT o.titre, o.annee_creation, o.type, o.support, " +
            "       o.description, o.prix, o.statut, a.nom AS artiste_nom " +
            "FROM OEUVRE o " +
            "INNER JOIN ARTISTE a ON a.id_oeuvre = o.id_oeuvre " +
            "WHERE a.nom=? ORDER BY o.titre";

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
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur save() oeuvre : " + artwork.getTitle(), e);
        }
    }

    @Override
    public void update(Artwork artwork) {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            conn.setAutoCommit(false);
            try {
                ps.setObject(1, artwork.getCreationYear(), Types.INTEGER);
                ps.setString(2, artwork.getType());
                ps.setString(3, artwork.getMedium());
                ps.setString(4, artwork.getDescription());
                ps.setDouble(5, artwork.getPrice());
                ps.setString(6, artwork.getStatus() != null ? artwork.getStatus().name() : "FOR_SALE");
                ps.setString(7, artwork.getTitle());
                int rows = ps.executeUpdate();
                if (rows == 0) throw new RuntimeException("Oeuvre introuvable : " + artwork.getTitle());
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
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
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
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
            switch (statut.toLowerCase()) {
                case "vendu"  -> a.setStatus(Artwork.Status.SOLD);
                case "exposé" -> a.setStatus(Artwork.Status.EXHIBITED);
                default       -> a.setStatus(Artwork.Status.FOR_SALE);
            }
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
