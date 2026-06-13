package com.siseradar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification", indexes = @Index(name = "idx_notif_user", columnList = "user_id"))
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "message", nullable = false, length = 500)
  private String message;

  @Column(name = "is_read", nullable = false)
  private boolean read;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Notification() {}

  public Notification(Long userId, String message, Instant createdAt) {
    this.userId = userId;
    this.message = message;
    this.read = false;
    this.createdAt = createdAt;
  }

  public void markRead() {
    this.read = true;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public String getMessage() {
    return message;
  }

  public boolean isRead() {
    return read;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
