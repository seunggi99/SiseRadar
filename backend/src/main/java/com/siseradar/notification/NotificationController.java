package com.siseradar.notification;

import com.siseradar.auth.CurrentUser;
import com.siseradar.notification.NotificationService.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "인앱 알림함 (로그인 필요)")
public class NotificationController {

  private final NotificationService service;

  public NotificationController(NotificationService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "내 알림 목록")
  public List<NotificationResponse> list() {
    return service.list(CurrentUser.id());
  }

  @GetMapping("/unread-count")
  @Operation(summary = "안 읽은 알림 수")
  public Map<String, Long> unreadCount() {
    return Map.of("count", service.unreadCount(CurrentUser.id()));
  }

  @PatchMapping("/{id}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "알림 읽음 처리")
  public void markRead(@PathVariable Long id) {
    service.markRead(CurrentUser.id(), id);
  }
}
