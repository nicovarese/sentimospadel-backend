package com.sentimospadel.backend.tournament.repository;

import com.sentimospadel.backend.tournament.entity.TournamentEntry;
import com.sentimospadel.backend.tournament.enums.TournamentEntryKind;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentEntryRepository extends JpaRepository<TournamentEntry, Long> {

    boolean existsByTournamentIdAndPrimaryPlayerProfileId(Long tournamentId, Long playerProfileId);

    @EntityGraph(attributePaths = {
            "primaryPlayerProfile",
            "primaryPlayerProfile.user",
            "secondaryPlayerProfile",
            "secondaryPlayerProfile.user",
            "createdBy",
            "createdBy.user"
    })
    List<TournamentEntry> findAllByTournamentIdOrderByCreatedAtAsc(Long tournamentId);

    @EntityGraph(attributePaths = {
            "primaryPlayerProfile",
            "primaryPlayerProfile.user",
            "secondaryPlayerProfile",
            "secondaryPlayerProfile.user",
            "createdBy",
            "createdBy.user"
    })
    List<TournamentEntry> findAllByTournamentIdAndEntryKindOrderByCreatedAtAsc(Long tournamentId, TournamentEntryKind entryKind);

    @EntityGraph(attributePaths = {
            "primaryPlayerProfile",
            "primaryPlayerProfile.user",
            "secondaryPlayerProfile",
            "secondaryPlayerProfile.user",
            "createdBy",
            "createdBy.user"
    })
    Optional<TournamentEntry> findByTournamentIdAndPrimaryPlayerProfileId(Long tournamentId, Long playerProfileId);

    @EntityGraph(attributePaths = {
            "primaryPlayerProfile",
            "primaryPlayerProfile.user",
            "secondaryPlayerProfile",
            "secondaryPlayerProfile.user",
            "createdBy",
            "createdBy.user"
    })
    Optional<TournamentEntry> findByTournamentIdAndPrimaryPlayerProfileIdAndEntryKind(Long tournamentId, Long playerProfileId, TournamentEntryKind entryKind);

    @EntityGraph(attributePaths = {
            "primaryPlayerProfile",
            "primaryPlayerProfile.user",
            "secondaryPlayerProfile",
            "secondaryPlayerProfile.user",
            "createdBy",
            "createdBy.user"
    })
    List<TournamentEntry> findAllByTournamentId(Long tournamentId);

    @EntityGraph(attributePaths = {
            "primaryPlayerProfile",
            "primaryPlayerProfile.user",
            "secondaryPlayerProfile",
            "secondaryPlayerProfile.user",
            "createdBy",
            "createdBy.user"
    })
    List<TournamentEntry> findAllByTournamentIdAndEntryKind(Long tournamentId, TournamentEntryKind entryKind);
}
