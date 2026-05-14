package com.project.artconnect.dao;

import com.project.artconnect.model.Artist;
import java.util.List;

/**
 * Data Access Object for Artist entity.
 */
public interface ArtistDao {
    List<Artist> findAll();

    void save(Artist artist);

    void update(Artist artist);

    /** Update avec ancien nom comme critère WHERE (permet de changer le nom) */
    void update(Artist artist, String oldName);

    void delete(String artistName);

    List<Artist> findByCity(String city);
}
