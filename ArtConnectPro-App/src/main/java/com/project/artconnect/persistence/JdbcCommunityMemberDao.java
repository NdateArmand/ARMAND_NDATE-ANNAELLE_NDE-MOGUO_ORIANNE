package com.project.artconnect.persistence;

import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Implémentation JDBC de CommunityMemberDao — table MEMBRE_COMMUNAUTE. */
public class JdbcCommunityMemberDao implements CommunityMemberDao {

    private static final String FIND_ALL =
            "SELECT id_membre, nom, email, annee_naissance, telephone, ville, type_adhesion " +
            "FROM MEMBRE_COMMUNAUTE ORDER BY nom";

    private static final String FIND_BY_ID =
            "SELECT id_membre, nom, email, annee_naissance, telephone, ville, type_adhesion " +
            "FROM MEMBRE_COMMUNAUTE WHERE id_membre=?";

    @Override
    public List<CommunityMember> findAll() {
        List<CommunityMember> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findAll() membres", e);
        }
        return list;
    }

    @Override
    public Optional<CommunityMember> findById(Long id) {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findById() membre id=" + id, e);
        }
        return Optional.empty();
    }

    private CommunityMember mapRow(ResultSet rs) throws SQLException {
        CommunityMember m = new CommunityMember();
        m.setName          (rs.getString("nom"));
        m.setEmail         (rs.getString("email"));
        m.setBirthYear     (rs.getObject("annee_naissance") != null ? rs.getInt("annee_naissance") : null);
        m.setPhone         (rs.getString("telephone"));
        m.setCity          (rs.getString("ville"));
        m.setMembershipType(rs.getString("type_adhesion"));
        return m;
    }
}
