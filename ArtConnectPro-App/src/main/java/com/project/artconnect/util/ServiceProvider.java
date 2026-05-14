package com.project.artconnect.util;

import com.project.artconnect.service.*;
import com.project.artconnect.service.impl.*;

/**
 * ServiceProvider — mis à jour pour utiliser les implémentations JDBC.
 *
 * AVANT (InMemory) :
 *   private static final InMemoryArtistService artistService = new InMemoryArtistService();
 *
 * APRÈS (JDBC connecté à la base) :
 *   private static final ArtistService artistService = new JdbcArtistService();
 *
 * Les contrôleurs UI n'ont PAS besoin d'être modifiés car ils passent
 * tous par ServiceProvider.getXxxService() — interface identique.
 */
public class ServiceProvider {

    // ---------------------------------------------------------------
    // Services JDBC — branchés sur la base de données ArtConnect
    // ---------------------------------------------------------------
    private static final ArtistService    artistService    = new JdbcArtistService();
    private static final ArtworkService   artworkService   = new JdbcArtworkService();
    private static final GalleryService   galleryService   = new JdbcGalleryService();
    private static final WorkshopService  workshopService  = new JdbcWorkshopService();
    private static final CommunityService communityService = new JdbcCommunityService();

    // Pas d'initData() nécessaire : les données viennent de la base

    public static ArtistService    getArtistService()    { return artistService; }
    public static ArtworkService   getArtworkService()   { return artworkService; }
    public static GalleryService   getGalleryService()   { return galleryService; }
    public static WorkshopService  getWorkshopService()  { return workshopService; }
    public static CommunityService getCommunityService() { return communityService; }
}
