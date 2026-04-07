package com.sentimospadel.backend.user.repository;

import com.sentimospadel.backend.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"managedClub"})
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @EntityGraph(attributePaths = {"managedClub"})
    Optional<User> findByEmailVerificationTokenHash(String emailVerificationTokenHash);
}
