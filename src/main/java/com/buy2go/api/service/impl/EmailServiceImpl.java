package com.buy2go.api.service.impl;

import com.buy2go.api.entity.OrderEntity;
import com.buy2go.api.enums.OrderStatus;
import com.buy2go.api.service.EmailService;
import com.buy2go.api.utils.EmailTemplateBuilder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${buy2go.mail.from:pawanfullstack@gmail.com}")
    private String mailFrom;

    @Value("${buy2go.mail.sender-name:BUY2GO.IN}")
    private String senderName;

    @Value("${buy2go.mail.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${buy2go.mail.support-email:support@buy2go.in}")
    private String supportEmail;

    @Override
    @Async
    public void sendOrderConfirmation(OrderEntity order) {
        if (order == null || order.getCustomer() == null) {
            return;
        }

        String recipientEmail = order.getCustomer().getEmail();
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            log.info("No customer email provided for order {}, skipping order confirmation email", order.getOrderNumber());
            return;
        }

        String orderNum = order.getOrderNumber() != null ? order.getOrderNumber() : order.getId();
        String subject = "Order Confirmed: " + orderNum + " - BUY2GO.IN";
        String trackingUrl = buildTrackingUrl(orderNum);

        try {
            String htmlBody = EmailTemplateBuilder.buildOrderEmailHtml(
                    order,
                    OrderStatus.New,
                    "Order placed successfully",
                    trackingUrl,
                    supportEmail
            );
            sendHtmlEmail(recipientEmail.trim(), subject, htmlBody);
            log.info("Order confirmation email successfully sent for order {} to {}", orderNum, recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order {}: {}", orderNum, e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendOrderStatusUpdate(OrderEntity order, OrderStatus oldStatus, OrderStatus newStatus, String remarks) {
        if (order == null || order.getCustomer() == null || newStatus == null) {
            return;
        }

        String recipientEmail = order.getCustomer().getEmail();
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            log.info("No customer email provided for order {}, skipping status update email", order.getOrderNumber());
            return;
        }

        String orderNum = order.getOrderNumber() != null ? order.getOrderNumber() : order.getId();
        String subject = getSubjectForStatus(newStatus, orderNum);
        String trackingUrl = buildTrackingUrl(orderNum);

        try {
            String htmlBody = EmailTemplateBuilder.buildOrderEmailHtml(
                    order,
                    newStatus,
                    remarks,
                    trackingUrl,
                    supportEmail
            );
            sendHtmlEmail(recipientEmail.trim(), subject, htmlBody);
            log.info("Order status update ({}) email sent for order {} to {}", newStatus, orderNum, recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send order status update email for order {}: {}", orderNum, e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setTo(toEmail.trim());
            helper.setSubject(subject);
            helper.setFrom(new InternetAddress(mailFrom, senderName));
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("HTML email successfully dispatched to '{}' with subject '{}'", toEmail, subject);
        } catch (Exception ex) {
            log.error("Error while dispatching HTML email to '{}': {}", toEmail, ex.getMessage(), ex);
        }
    }

    private String buildTrackingUrl(String orderNumber) {
        String base = (frontendBaseUrl != null && !frontendBaseUrl.isEmpty())
                ? frontendBaseUrl.replaceAll("/+$", "")
                : "http://localhost:3000";
        String cleanNum = (orderNumber != null) ? orderNumber.replace("#", "") : "";
        return base + "/track/" + cleanNum;
    }

    private String getSubjectForStatus(OrderStatus status, String orderNumber) {
        switch (status) {
            case Confirmed:
                return "Order Confirmed: " + orderNumber + " - BUY2GO.IN";
            case Printing:
                return "Printing in Progress: " + orderNumber + " - BUY2GO.IN";
            case Shipped:
                return "Your Order is on the Way! " + orderNumber + " - BUY2GO.IN";
            case Delivered:
                return "Delivered: Your Order " + orderNumber + " has arrived! - BUY2GO.IN";
            case Cancelled:
                return "Order Cancelled: " + orderNumber + " - BUY2GO.IN";
            case New:
            default:
                return "Order Received: " + orderNumber + " - BUY2GO.IN";
        }
    }
}
