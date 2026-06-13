package com.siseradar.notification;

import com.siseradar.domain.Notification;
import com.siseradar.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

  private final NotificationRepository repository;

  public NotificationService(NotificationRepository repository) {
    this.repository = repository;
  }

  public record NotificationResponse(Long id, String message, boolean read, Instant createdAt) {
    static NotificationResponse from(Notification n) {
      return new NotificationResponse(n.getId(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
  }

  public List<NotificationResponse> list(Long userId) {
    return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(NotificationResponse::from)
        .toList();
  }

  public long unreadCount(Long userId) {
    return repository.countByUserIdAndReadFalse(userId);
  }

  @Transactional
  public void markRead(Long userId, Long id) {
    Notification n =
        repository
            .findById(id)
            .filter(item -> item.getUserId().equals(userId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "없는 알림입니다"));
    n.markRead();
  }
}
