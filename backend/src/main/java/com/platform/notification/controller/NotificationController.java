package com.platform.notification.controller;

import com.platform.common.pagination.PageResponse;
import com.platform.common.security.CurrentUser;
import com.platform.notification.dto.NotificationDto;
import com.platform.notification.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/me/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public PageResponse<NotificationDto> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return notificationService.listForUser(CurrentUser.id(), page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", notificationService.countUnread(CurrentUser.id()));
    }

    @PostMapping("/read-all")
    public void markAllAsRead() {
        notificationService.markAllAsRead(CurrentUser.id());
    }
}
