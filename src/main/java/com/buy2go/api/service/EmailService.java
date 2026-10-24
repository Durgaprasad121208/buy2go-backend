package com.buy2go.api.service;

import com.buy2go.api.entity.OrderEntity;
import com.buy2go.api.enums.OrderStatus;

public interface EmailService {

    /**
     * Send order confirmation email to the customer when an order is placed.
     */
    void sendOrderConfirmation(OrderEntity order);

    /**
     * Send status update email to the customer when the order status changes (e.g. Confirmed, Printing, Shipped, Delivered, Cancelled).
     */
    void sendOrderStatusUpdate(OrderEntity order, OrderStatus oldStatus, OrderStatus newStatus, String remarks);

    /**
     * Send a raw custom HTML email.
     */
    void sendHtmlEmail(String toEmail, String subject, String htmlBody);
}
