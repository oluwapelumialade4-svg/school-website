package siwes.project.school_website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import siwes.project.school_website.entity.Notification;
import siwes.project.school_website.entity.User;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByTimestampDesc(User recipient);
}