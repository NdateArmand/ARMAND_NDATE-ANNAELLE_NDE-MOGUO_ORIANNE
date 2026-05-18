package com.project.artconnect.dao;

import com.project.artconnect.model.Artwork;
import java.util.List;

/**
 * DAO pour la table de liaison EXPOSER (exposition ↔ oeuvre).
 */
public interface ExposerDao {

    /** Retourne toutes les œuvres rattachées à une exposition (par son titre). */
    List<Artwork> findArtworksByExhibition(String exhibitionTitle);

    /** Retourne toutes les œuvres NON rattachées à une exposition donnée. */
    List<Artwork> findArtworksNotInExhibition(String exhibitionTitle);

    /**
     * Ajoute une œuvre à une exposition.
     * @param exhibitionTitle titre de l'exposition
     * @param artworkId       id de l'œuvre
     * @param emplacement     emplacement dans la galerie (peut être null)
     */
    void addArtworkToExhibition(String exhibitionTitle, Long artworkId, String emplacement);

    /**
     * Retire une œuvre d'une exposition.
     */
    void removeArtworkFromExhibition(String exhibitionTitle, Long artworkId);
}
