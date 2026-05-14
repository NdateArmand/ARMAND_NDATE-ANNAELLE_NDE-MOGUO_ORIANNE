package com.project.artconnect.dao;

import com.project.artconnect.model.Exhibition;
import java.util.List;

public interface ExhibitionDao {
    List<Exhibition> findAll();

    void save(Exhibition exhibition);

    void update(Exhibition exhibition);

    /** Update avec ancien titre comme critère WHERE (permet de changer le titre) */
    void update(Exhibition exhibition, String oldTitle);

    void delete(String title);
}
