package com.project.artconnect.service.impl;

import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.dao.impl.DaoFactory;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Review;
import com.project.artconnect.service.CommunityService;

import java.util.List;
import java.util.Optional;

/**
 * Implémentation JDBC de CommunityService.
 * Remplace InMemoryCommunityService.
 */
public class JdbcCommunityService implements CommunityService {

    private final CommunityMemberDao memberDao = DaoFactory.getCommunityMemberDao();

    @Override
    public List<CommunityMember> getAllMembers() {
        return memberDao.findAll();
    }

    @Override
    public Optional<CommunityMember> getMemberByName(String name) {
        return memberDao.findAll().stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public List<Review> getReviewsByMember(CommunityMember member) {
        // Les reviews sont gérées en mémoire sur l'objet CommunityMember
        return member.getReviews();
    }
}
