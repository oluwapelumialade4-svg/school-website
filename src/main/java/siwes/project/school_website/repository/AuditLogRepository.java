package siwes.project.school_website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import siwes.project.school_website.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}