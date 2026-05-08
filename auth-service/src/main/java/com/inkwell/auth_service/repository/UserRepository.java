package com.inkwell.auth_service.repository;

import com.inkwell.auth_service.model.User;
import com.inkwell.auth_service.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findByRole(UserRole role);

    List<User> findByUsernameContainingIgnoreCase(String username);
}
