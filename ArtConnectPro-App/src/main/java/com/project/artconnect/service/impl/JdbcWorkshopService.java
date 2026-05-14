package com.project.artconnect.service.impl;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.dao.impl.DaoFactory;
import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation JDBC de WorkshopService.
 * Remplace InMemoryWorkshopService.
 */
public class JdbcWorkshopService implements WorkshopService {

    private final WorkshopDao workshopDao = DaoFactory.getWorkshopDao();

    @Override
    public List<Workshop> getAllWorkshops() {
        return workshopDao.findAll();
    }

    @Override
    public Optional<Workshop> getWorkshopByTitle(String title) {
        return workshopDao.findAll().stream()
                .filter(w -> w.getTitle().equalsIgnoreCase(title))
                .findFirst();
    }

    /**
     * Inscrit un membre à un atelier via la table INSCRIPTION.
     * Utilise une transaction pour garantir l'atomicité.
     * Le trigger trg_verif_places vérifie automatiquement les places disponibles.
     */
    @Override
    public void bookWorkshop(Workshop workshop, CommunityMember member) {
        String sql = "INSERT INTO INSCRIPTION (id_membre, id_atelier, date_inscription, nb_places, statut_paie) " +
                     "SELECT mc.id_membre, at.id_atelier, CURDATE(), 1, 'en attente' " +
                     "FROM MEMBRE_COMMUNAUTE mc, ATELIER at " +
                     "WHERE mc.nom = ? AND at.titre = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                ps.setString(1, member.getName());
                ps.setString(2, workshop.getTitle());
                ps.executeUpdate();
                conn.commit();
                // Mettre à jour l'objet en mémoire
                Booking b = new Booking(workshop, member);
                member.addBooking(b);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur bookWorkshop()", e);
        }
    }

    @Override
    public List<Booking> getBookingsByMember(CommunityMember member) {
        // Les bookings sont gérés en mémoire sur l'objet CommunityMember
        return member.getBookings();
    }
}
