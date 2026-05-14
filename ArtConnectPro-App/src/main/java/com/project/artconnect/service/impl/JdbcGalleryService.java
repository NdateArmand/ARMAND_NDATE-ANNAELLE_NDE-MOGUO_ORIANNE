package com.project.artconnect.service.impl;

import com.project.artconnect.dao.ExhibitionDao;
import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.dao.impl.DaoFactory;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.GalleryService;

import java.util.List;
import java.util.Optional;

/**
 * Implémentation JDBC de GalleryService.
 * Remplace InMemoryGalleryService.
 */
public class JdbcGalleryService implements GalleryService {

    private final GalleryDao    galleryDao    = DaoFactory.getGalleryDao();
    private final ExhibitionDao exhibitionDao = DaoFactory.getExhibitionDao();

    @Override
    public List<Gallery> getAllGalleries() {
        return galleryDao.findAll();
    }

    @Override
    public Optional<Gallery> getGalleryByName(String name) {
        return galleryDao.findAll().stream()
                .filter(g -> g.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * Retourne toutes les expositions de la base (elles sont déjà liées à leur galerie).
     * L'ExhibitionController les affiche directement.
     */
    @Override
    public List<Exhibition> getExhibitionsByGallery(Gallery gallery) {
        return exhibitionDao.findAll().stream()
                .filter(e -> e.getGallery() != null
                        && e.getGallery().getName().equalsIgnoreCase(gallery.getName()))
                .toList();
    }
}
