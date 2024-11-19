package com.fanci.Hyperion_be.service.serviceImpl;

import com.fanci.Hyperion_be.dto.OrderNotificationDto;
import com.fanci.Hyperion_be.dto.response.NotificationResponse;
import com.fanci.Hyperion_be.entity.Notification;
import com.fanci.Hyperion_be.entity.Orders;
import com.fanci.Hyperion_be.entity.Role;
import com.fanci.Hyperion_be.enums.NotificationStatus;
import com.fanci.Hyperion_be.exception.AppException;
import com.fanci.Hyperion_be.exception.ErrorCode;
import com.fanci.Hyperion_be.repository.NotificationRepository;
import com.fanci.Hyperion_be.repository.RoleRepository;
import com.fanci.Hyperion_be.repository.UserRepository;
import com.fanci.Hyperion_be.service.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    @Value("${spring.mail.username}")
    private String senderMail;

    private final JavaMailSender mailSender;

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final NotificationRepository notificationRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;


    public void sendOrderMail(String to, String subject, String htmlContent, Long billId) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(senderMail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(String.format(htmlContent, billId, billId), true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new AppException(ErrorCode.CANT_SEND_MAIL);
        }
    }

    public void createNewNotificationWhenCreateNewOrder(Orders order) {

        Set<String> roleName = Set.of("ADMIN", "MODERATOR", "SALE");
        Set<Role> roles = roleRepository.findAll().stream().filter(role -> roleName.contains(role.getRoleName()))
                .collect(Collectors.toSet());

        Notification notification = Notification.builder()
                .createdAt(LocalDateTime.now())
                .orderStatus(order.getStatus())
                .orders(order)
                .roles(roles)
                .notificationStatus(NotificationStatus.NEW)
                .build();
        notificationRepository.save(notification);
    }



    public void sendNotificationByWebSocket(Orders order) {
        OrderNotificationDto orderNotificationDto = OrderNotificationDto.builder()
                .orderId(order.getId())
                .orderStatus(order.getStatus().name())
                .build();
        simpMessagingTemplate.convertAndSend("/topic/notifications", orderNotificationDto);
    }
    @Override
    public void setReadNotificationById(Long id) {
        var notification = notificationRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_ID_NOT_FOUND));
        notification.setNotificationStatus(NotificationStatus.READ);
        notificationRepository.save(notification);
    }

    @Override
    public Page<NotificationResponse> findAllNotificationByUserId(Long userId, int page, int size){
        var user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_ID_NOT_FOUND));
        Pageable pageable = PageRequest.of(page, size);

        Set<Role> roles = user.getRoles();

        Set<Notification> notificationSet = new HashSet<>();
        roles.forEach(role -> notificationSet.addAll(role.getNotifications()));

        List<Notification> notifications = new ArrayList<>(notificationSet);

        notifications.sort(Comparator.comparing(Notification::getCreatedAt).reversed());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), notifications.size());
        List<NotificationResponse> subList = notifications.subList(start, end)
                .stream()
                .map(this::toNotificationResponse)
                .toList();

        return new PageImpl<>(subList, pageable, notifications.size());
    }

    NotificationResponse toNotificationResponse(Notification notification){
        return NotificationResponse.builder()
                .id(notification.getId())
                .createdAt(notification.getCreatedAt())
                .orderStatus(notification.getOrderStatus().name())
                .notificationStatus(notification.getNotificationStatus().name())
                .orderId(notification.getOrders().getId())
                .build();
    }


}
