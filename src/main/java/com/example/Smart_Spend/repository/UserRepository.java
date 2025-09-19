package com.example.Smart_Spend.repository;

import com.example.Smart_Spend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByVerificationToken(String verificationToken);

    Optional<User> findByResetToken(String resetToken);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.emailVerified = true")
    Optional<User> findByEmailAndVerified(@Param("email") String email);

    long countByRole(User.Role role);
}