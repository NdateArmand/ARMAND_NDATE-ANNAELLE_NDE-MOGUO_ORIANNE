package com.project.artconnect.dao.impl;

import com.project.artconnect.dao.*;
import com.project.artconnect.persistence.*;

/**
 * Fabrique centralisée des implémentations DAO JDBC.
 * Package requis par le sujet : com.project.artconnect.dao.impl
 */
public class DaoFactory {

    private DaoFactory() {}

    public static ArtistDao          getArtistDao()          { return new JdbcArtistDao(); }
    public static ArtworkDao         getArtworkDao()         { return new JdbcArtworkDao(); }
    public static ExhibitionDao      getExhibitionDao()      { return new JdbcExhibitionDao(); }
    public static GalleryDao         getGalleryDao()         { return new JdbcGalleryDao(); }
    public static WorkshopDao        getWorkshopDao()        { return new JdbcWorkshopDao(); }
    public static CommunityMemberDao getCommunityMemberDao() { return new JdbcCommunityMemberDao(); }
}
