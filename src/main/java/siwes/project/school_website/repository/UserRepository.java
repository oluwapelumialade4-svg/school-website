package siwes.project.school_website.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import siwes.project.school_website.entity.User;
import siwes.project.school_website.entity.Role;
import siwes.project.school_website.entity.Department;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByMatricNumber(String matricNumber);
    Optional<User> findByResetPasswordToken(String token);
    List<User> findByRole(Role role);
    Page<User> findByRole(Role role, Pageable pageable);
    long countByRole(Role role);
    List<User> findByDepartmentAndRole(Department department, Role role);
}