package com.project.artconnect.persistence;

import com.project.artconnect.dao.ExposerDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation JDBC du DAO pour la table EXPOSER.
 * Gère le rattachement des œuvres aux expositions.
 */
public class JdbcExposerDao implements ExposerDao {

    private static final String FIND_IN =
            "SELECT o.id_oeuvre, o.titre, o.type, o.support, o.prix, o.statut, " +
            "       a.nom AS artiste_nom " +
            "FROM exposer ex " +
            "JOIN oeuvre o ON o.id_oeuvre = ex.id_oeuvre " +
            "JOIN exposition e ON e.id_exposition = ex.id_exposition " +
            "LEFT JOIN artiste a ON a.id_artiste = o.id_artiste " +
            "WHERE e.titre = ? " +
            "ORDER BY o.titre";

    private static final String FIND_NOT_IN =
            "SELECT o.id_oeuvre, o.titre, o.type, o.support, o.prix, o.statut, " +
            "       a.nom AS artiste_nom " +
            "FROM oeuvre o " +
            "LEFT JOIN artiste a ON a.id_artiste = o.id_artiste " +
            "WHERE o.id_oeuvre NOT IN ( " +
            "    SELECT ex.id_oeuvre FROM exposer ex " +
            "    JOIN exposition e ON e.id_exposition = ex.id_exposition " +
            "    WHERE e.titre = ? " +
            ") " +
            "ORDER BY o.titre";

    private static final String INSERT =
            "INSERT INTO exposer (id_exposition, id_oeuvre, emplacement) " +
            "SELECT e.id_exposition, ?, ? " +
            "FROM exposition e WHERE e.titre = ? LIMIT 1";

    private static final String DELETE =
            "DELETE ex FROM exposer ex " +
            "JOIN exposition e ON e.id_exposition = ex.id_exposition " +
            "WHERE e.titre = ? AND ex.id_oeuvre = ?";

    @Override
    public List<Artwork> findArtworksByExhibition(String exhibitionTitle) {
        List<Artwork> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_IN)) {
            ps.setString(1, exhibitionTitle);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findArtworksByExhibition : " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Artwork> findArtworksNotInExhibition(String exhibitionTitle) {
        List<Artwork> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_NOT_IN)) {
            ps.setString(1, exhibitionTitle);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findArtworksNotInExhibition : " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void addArtworkToExhibition(String exhibitionTitle, Long artworkId, String emplacement) {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT)) {
            conn.setAutoCommit(false);
            try {
                ps.setLong  (1, artworkId);
                ps.setString(2, emplacement);
                ps.setString(3, exhibitionTitle);
                int rows = ps.executeUpdate();
                if (rows == 0)
                    throw new RuntimeException("Exposition introuvable : " + exhibitionTitle);
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur addArtworkToExhibition : " + e.getMessage(), e);
        }
    }

    @Override
    public void removeArtworkFromExhibition(String exhibitionTitle, Long artworkId) {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {
            conn.setAutoCommit(false);
            try {
                ps.setString(1, exhibitionTitle);
                ps.setLong  (2, artworkId);
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur removeArtworkFromExhibition : " + e.getMessage(), e);
        }
    }

    private Artwork mapRow(ResultSet rs) throws SQLException {
        Artwork artwork = new Artwork();
        artwork.setId    (rs.getLong  ("id_oeuvre"));
        artwork.setTitle (rs.getString("titre"));
        artwork.setType  (rs.getString("type"));
        artwork.setMedium(rs.getString("support"));
        artwork.setPrice (rs.getDouble("prix"));

        String statut = rs.getString("statut");
        if (statut != null) {
            artwork.setStatus(switch (statut.toUpperCase()) {
                case "SOLD",      "VENDU"            -> Artwork.Status.SOLD;
                case "EXHIBITED", "EXPOSÉ", "EXPOSE" -> Artwork.Status.EXHIBITED;
                default -> Artwork.Status.FOR_SALE;
            });
        }

        String artisteNom = rs.getString("artiste_nom");
        if (artisteNom != null) {
            Artist artist = new Artist();
            artist.setName(artisteNom);
            artwork.setArtist(artist);
        }

        return artwork;
    }
}
