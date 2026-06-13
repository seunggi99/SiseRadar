package com.siseradar.alert;

import com.siseradar.alert.AlertDtos.AlertRuleRequest;
import com.siseradar.alert.AlertDtos.AlertRuleResponse;
import com.siseradar.auth.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts", description = "알림 규칙 (로그인 필요)")
public class AlertController {

  private final AlertService service;

  public AlertController(AlertService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "내 알림 규칙 조회")
  public List<AlertRuleResponse> list() {
    return service.list(CurrentUser.id());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "관심목록 항목에 알림 규칙 추가")
  public AlertRuleResponse create(@Valid @RequestBody AlertRuleRequest request) {
    return service.create(CurrentUser.id(), request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "알림 규칙 삭제")
  public void delete(@PathVariable Long id) {
    service.delete(CurrentUser.id(), id);
  }
}
