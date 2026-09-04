package com.platform.notification.service;

import com.platform.common.pagination.PageResponse;
import com.platform.notification.domain.Notification;
import com.platform.notification.domain.NotificationType;
import com.platform.notification.dto.NotificationDto;
import com.platform.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification send(UUID userId, NotificationType type, String title, String message,
                             String referenceType, UUID referenceId) {
        Notification notification = new Notification(userId, type, title, message, referenceType, referenceId);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> listForUser(UUID userId, int page, int size) {
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return toPageResponse(notifications);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    public static NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getType().name(),
                n.getTitle(),
                n.getMessage(),
                n.getReferenceType(),
                n.getReferenceId(),
                n.isRead(),
                n.getCreatedAt().toString()
        );
    }

    private PageResponse<NotificationDto> toPageResponse(Page<Notification> page) {
        return new PageResponse<>(
                page.getContent().stream().map(NotificationService::toDto).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
