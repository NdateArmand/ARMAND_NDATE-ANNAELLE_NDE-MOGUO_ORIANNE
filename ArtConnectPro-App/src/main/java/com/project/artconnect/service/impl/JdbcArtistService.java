package com.project.artconnect.service.impl;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.dao.impl.DaoFactory;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JdbcArtistService — mis à jour :
 * - getAllDisciplines() lit depuis la table DISCIPLINE
 * - searchArtists() filtre par discipline via ARTISTE_DISCIPLINE
 */
public class JdbcArtistService implements ArtistService {

    private final ArtistDao artistDao = DaoFactory.getArtistDao();

    @Override
    public List<Artist> getAllArtists() {
        return artistDao.findAll();
    }

    @Override
    public Optional<Artist> getArtistByName(String name) {
        return artistDao.findAll().stream()
                .filter(a -> a.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public void createArtist(Artist artist) { artistDao.save(artist); }

    @Override
    public void updateArtist(Artist artist) { artistDao.update(artist); }

    @Override
    public void deleteArtist(String name)   { artistDao.delete(name); }

    /**
     * Charge toutes les disciplines depuis la table DISCIPLINE.
     * Maintenant fonctionnel grâce à la table créée par le script ALTER.
     */
    @Override
    public List<Discipline> getAllDisciplines() {
        List<Discipline> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT nom FROM DISCIPLINE ORDER BY nom");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new Discipline(rs.getString("nom")));
        } catch (SQLException e) {
            System.err.println("Table DISCIPLINE non disponible : " + e.getMessage());
        }
        return list;
    }

    /**
     * Recherche par nom, discipline et/ou ville.
     * Le filtre discipline interroge la table ARTISTE_DISCIPLINE.
     */
    @Override
    public List<Artist> searchArtists(String query, String disciplineName, String city) {
        List<Artist> all = artistDao.findAll();
        return all.stream()
                .filter(a -> query == null || query.isBlank()
                        || a.getName().toLowerCase().contains(query.toLowerCase()))
                .filter(a -> city == null || city.isBlank()
                        || (a.getCity() != null && a.getCity().equalsIgnoreCase(city)))
                .filter(a -> disciplineName == null || disciplineName.isBlank()
                        || a.getDisciplines().stream()
                        .anyMatch(d -> d.getName().equalsIgnoreCase(disciplineName)))
                .collect(Collectors.toList());
    }
}
