package com.buy2go.api.utils;

import com.buy2go.api.entity.OrderEntity;
import com.buy2go.api.enums.OrderStatus;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class EmailTemplateBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
            .withZone(ZoneId.of("Asia/Kolkata"));

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("en", "IN"));

    /**
     * Builds a mobile-responsive, dark-themed HTML email for order confirmation and
     * status updates.
     */
    public static String buildOrderEmailHtml(OrderEntity order, OrderStatus status, String customMessage,
            String trackingUrl, String supportEmail) {
        String orderNum = order.getOrderNumber() != null ? order.getOrderNumber()
                : (order.getId() != null ? order.getId() : "N/A");
        String formattedDate = order.getCreatedAt() != null ? DATE_FORMATTER.format(order.getCreatedAt())
                : "Recently placed";
        String customerName = order.getCustomerName() != null && !order.getCustomerName().trim().isEmpty()
                ? order.getCustomerName()
                : "Customer";
        String formattedTotal = CURRENCY_FORMAT.format(order.getTotalAmount()).replace("₹", "₹ ");

        // Status metadata
        String statusTitle;
        String statusSubtitle;
        String badgeColor = "#FF5E00";
        String badgeBg = "rgba(255, 94, 0, 0.15)";

        switch (status) {
            case Confirmed:
                statusTitle = "Order Confirmed & Verified";
                statusSubtitle = "Great news! Your order details have been verified and passed to our production unit.";
                break;
            case Printing:
                statusTitle = "Printing & Production in Progress";
                statusSubtitle = "Our specialists are now printing and customizing your items with precision.";
                break;
            case Shipped:
                statusTitle = "Your Order is on the Way!";
                statusSubtitle = "Your package has been dispatched with our logistics partner and is en route.";
                break;
            case Delivered:
                statusTitle = "Order Delivered!";
                statusSubtitle = "Your package has been successfully delivered. We hope you love your custom product!";
                break;
            case Cancelled:
                statusTitle = "Order Cancelled";
                statusSubtitle = customMessage != null && !customMessage.isEmpty()
                        ? customMessage
                        : "Your order has been cancelled. If you have questions, please reach out to our team.";
                badgeColor = "#EF4444";
                badgeBg = "rgba(239, 68, 68, 0.15)";
                break;
            case New:
            default:
                statusTitle = "Order Placed Successfully!";
                statusSubtitle = "Thank you for shopping with us! We have received your order and are getting it ready.";
                break;
        }

        // Build Progress Tracker Steps HTML
        String trackerHtml = buildProgressTracker(status);

        // Build Items rows HTML
        StringBuilder itemsHtml = new StringBuilder();
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderEntity.OrderItemDetail item : order.getItems()) {
                String itemTitle = item.getTitle() != null ? escapeHtml(item.getTitle()) : "Custom Product";
                String itemVariant = item.getVariant() != null ? escapeHtml(item.getVariant())
                        : (item.getColor() != null ? escapeHtml(item.getColor()) : "");
                String customText = item.getTextToPrint() != null && !item.getTextToPrint().trim().isEmpty()
                        ? escapeHtml(item.getTextToPrint().trim())
                        : null;
                String itemPrice = CURRENCY_FORMAT.format(item.getPrice()).replace("₹", "₹ ");
                String itemSubtotal = CURRENCY_FORMAT.format(item.getPrice() * item.getQuantity()).replace("₹", "₹ ");
                String photoUrl = (item.getPhoto() != null && !item.getPhoto().isEmpty()) ? item.getPhoto()
                        : order.getPhoto();

                itemsHtml.append("<tr>")
                        .append("<td style=\"padding: 16px 12px; border-bottom: 1px solid rgba(255,255,255,0.08); vertical-align: top;\">")
                        .append("<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">")
                        .append("<tr>");

                if (photoUrl != null && !photoUrl.isEmpty()) {
                    itemsHtml.append("<td width=\"64\" style=\"vertical-align: top; padding-right: 14px;\">")
                            .append("<img src=\"").append(photoUrl).append("\" alt=\"").append(itemTitle)
                            .append("\" width=\"60\" height=\"60\" style=\"border-radius: 6px; object-fit: cover; border: 1px solid rgba(255,255,255,0.12); display: block; background-color: #1A1412;\" />")
                            .append("</td>");
                }

                itemsHtml.append("<td style=\"vertical-align: top;\">")
                        .append("<div style=\"color: #FFFFFF; font-size: 15px; font-weight: 700; line-height: 1.3; margin-bottom: 4px;\">")
                        .append(itemTitle)
                        .append("</div>");

                if (!itemVariant.isEmpty()) {
                    itemsHtml.append("<div style=\"color: #8C867E; font-size: 13px; margin-bottom: 4px;\">")
                            .append("Variant / Color: <strong style=\"color: #E2DDD5;\">").append(itemVariant)
                            .append("</strong>")
                            .append("</div>");
                }

                if (customText != null) {
                    itemsHtml.append(
                            "<div style=\"display: inline-block; background-color: rgba(255,94,0,0.12); border: 1px solid rgba(255,94,0,0.25); border-radius: 4px; padding: 3px 8px; margin-top: 4px;\">")
                            .append("<span style=\"color: #FF5E00; font-size: 11px; font-weight: 800; text-transform: uppercase;\">Print Text: </span>")
                            .append("<span style=\"color: #F5F2EB; font-size: 12px; font-weight: 600;\">\"")
                            .append(customText).append("\"</span>")
                            .append("</div>");
                }

                itemsHtml.append("<div style=\"color: #8C867E; font-size: 13px; margin-top: 6px;\">")
                        .append("Qty: <strong style=\"color: #FFFFFF;\">").append(item.getQuantity())
                        .append("</strong> × ").append(itemPrice)
                        .append("</div>")
                        .append("</td>")
                        .append("<td align=\"right\" style=\"vertical-align: top; white-space: nowrap; padding-left: 10px;\">")
                        .append("<div style=\"color: #FFFFFF; font-size: 15px; font-weight: 800;\">")
                        .append(itemSubtotal).append("</div>")
                        .append("</td>")
                        .append("</tr>")
                        .append("</table>")
                        .append("</td>")
                        .append("</tr>");
            }
        } else {
            itemsHtml.append(
                    "<tr><td style=\"padding: 16px; color: #8C867E; font-size: 14px;\">Customized Merchandise Item(s)</td></tr>");
        }

        // Build Shipment details box if status is Shipped or Delivered
        String shipmentBoxHtml = "";
        if (order.getShipment() != null && (status == OrderStatus.Shipped || status == OrderStatus.Delivered)) {
            String courier = order.getShipment().getCourier() != null && !order.getShipment().getCourier().isEmpty()
                    ? order.getShipment().getCourier()
                    : "Standard Delivery";
            String trackingNum = order.getShipment().getTrackingNumber() != null
                    && !order.getShipment().getTrackingNumber().isEmpty() ? order.getShipment().getTrackingNumber()
                            : "Pending";
            String notes = order.getShipment().getNotes();

            shipmentBoxHtml = "<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color: #1A1411; border: 1px solid rgba(255,94,0,0.3); border-radius: 8px; margin-bottom: 24px;\">"
                    +
                    "<tr><td style=\"padding: 18px 20px;\">" +
                    "<div style=\"color: #FF5E00; font-size: 11px; font-weight: 800; letter-spacing: 0.12em; text-transform: uppercase; margin-bottom: 8px;\">🚚 SHIPMENT DETAILS</div>"
                    +
                    "<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">" +
                    "<tr>" +
                    "<td width=\"50%\" style=\"vertical-align: top; padding-right: 10px;\">" +
                    "<div style=\"color: #8C867E; font-size: 12px;\">Courier Partner</div>" +
                    "<div style=\"color: #FFFFFF; font-size: 14px; font-weight: 700; margin-top: 2px;\">"
                    + escapeHtml(courier) + "</div>" +
                    "</td>" +
                    "<td width=\"50%\" style=\"vertical-align: top; padding-left: 10px;\">" +
                    "<div style=\"color: #8C867E; font-size: 12px;\">Tracking Number</div>" +
                    "<div style=\"color: #FF5E00; font-size: 14px; font-weight: 800; font-family: monospace; margin-top: 2px;\">"
                    + escapeHtml(trackingNum) + "</div>" +
                    "</td>" +
                    "</tr>" +
                    (notes != null && !notes.trim().isEmpty()
                            ? "<tr><td colspan=\"2\" style=\"padding-top: 10px; color: #A39D94; font-size: 13px;\"><em>Note: "
                                    + escapeHtml(notes) + "</em></td></tr>"
                            : "")
                    +
                    "</table>" +
                    "</td></tr>" +
                    "</table>";
        }

        // Customer Delivery Info
        String customerPhone = order.getCustomer() != null && order.getCustomer().getPhone() != null
                ? order.getCustomer().getPhone()
                : "";
        String customerAddress = order.getLocation() != null && !order.getLocation().isEmpty()
                ? order.getLocation()
                : (order.getCustomer() != null && order.getCustomer().getAddress() != null
                        ? order.getCustomer().getAddress()
                        : "Address provided during checkout");

        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
                "  <title>" + statusTitle + " - " + orderNum + "</title>\n" +
                "  <style type=\"text/css\">\n" +
                "    body, table, td, a { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }\n" +
                "    table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }\n" +
                "    img { -ms-interpolation-mode: bicubic; border: 0; outline: none; text-decoration: none; }\n" +
                "    body { margin: 0; padding: 0; width: 100% !important; background-color: #0A0806; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }\n"
                +
                "    @media only screen and (max-width: 600px) {\n" +
                "      .email-container { width: 100% !important; margin: 0 auto !important; }\n" +
                "      .responsive-padding { padding-left: 16px !important; padding-right: 16px !important; }\n" +
                "      .stack-column { display: block !important; width: 100% !important; max-width: 100% !important; padding-left: 0 !important; padding-right: 0 !important; margin-bottom: 12px !important; }\n"
                +
                "      .tracker-text { font-size: 9px !important; }\n" +
                "      .tracker-circle { width: 22px !important; height: 22px !important; line-height: 22px !important; font-size: 10px !important; }\n"
                +
                "      .order-header-title { font-size: 24px !important; }\n" +
                "      .btn-cta { width: 100% !important; text-align: center !important; display: block !important; box-sizing: border-box !important; }\n"
                +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body style=\"margin: 0; padding: 0; background-color: #0A0806; color: #FFFFFF;\">\n" +
                "  <center style=\"width: 100%; background-color: #0A0806; padding: 24px 0 40px 0;\">\n" +
                "    <table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" class=\"email-container\" style=\"max-width: 600px; width: 100%; margin: 0 auto; background-color: #14100E; border-radius: 12px; border: 1px solid rgba(255,255,255,0.08); overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.6);\">\n"
                +
                "      \n" +
                "      <!-- BRAND HEADER -->\n" +
                "      <tr>\n" +
                "        <td align=\"center\" style=\"padding: 32px 24px 24px 24px; background: linear-gradient(180deg, #18120F 0%, #14100E 100%); border-bottom: 1px solid rgba(255,255,255,0.06);\">\n"
                +
                "          <a href=\"" + trackingUrl
                + "\" target=\"_blank\" style=\"text-decoration: none; display: inline-block;\">\n" +
                "            <div style=\"letter-spacing: -0.5px; font-weight: 900; line-height: 1;\">\n" +
                "              <span style=\"color: #FF5E00; font-size: 32px; font-weight: 900;\">BUY</span><span style=\"color: #FFFFFF; font-size: 32px; font-weight: 900;\">2GO.IN</span>\n"
                +
                "            </div>\n" +
                "          </a>\n" +
                "          <div style=\"color: #8C867E; font-size: 11px; font-weight: 800; letter-spacing: 0.16em; text-transform: uppercase; margin-top: 6px;\">PREMIUM CUSTOM PRINTS & MERCHANDISE</div>\n"
                +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "      <!-- STATUS HERO -->\n" +
                "      <tr>\n" +
                "        <td style=\"padding: 32px 28px 20px 28px; text-align: center;\" class=\"responsive-padding\">\n"
                +
                "          <div style=\"display: inline-block; background-color: " + badgeBg + "; border: 1px solid "
                + badgeColor + "; color: " + badgeColor
                + "; border-radius: 20px; padding: 6px 18px; font-size: 13px; font-weight: 800; letter-spacing: 0.1em; text-transform: uppercase; margin-bottom: 16px;\">\n"
                +
                "            " + status.name().toUpperCase() + "\n" +
                "          </div>\n" +
                "          <h1 class=\"order-header-title\" style=\"color: #FFFFFF; font-size: 28px; font-weight: 900; line-height: 1.2; margin: 0 0 10px 0; letter-spacing: -0.02em;\">\n"
                +
                "            " + statusTitle + "\n" +
                "          </h1>\n" +
                "          <p style=\"color: #A39D94; font-size: 15px; line-height: 1.5; margin: 0 auto; max-width: 480px;\">\n"
                +
                "            " + statusSubtitle + "\n" +
                "          </p>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "      <!-- PROGRESS TRACKER (Only for normal order flow) -->\n" +
                (status != OrderStatus.Cancelled ? "      <tr>\n" +
                        "        <td style=\"padding: 10px 28px 28px 28px;\" class=\"responsive-padding\">\n" +
                        "          " + trackerHtml + "\n" +
                        "        </td>\n" +
                        "      </tr>\n" : "")
                +
                "\n" +
                "      <!-- ORDER DETAILS META CARD -->\n" +
                "      <tr>\n" +
                "        <td style=\"padding: 0 28px 24px 28px;\" class=\"responsive-padding\">\n" +
                "          <table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color: #0E0B09; border: 1px solid rgba(255,255,255,0.08); border-radius: 8px;\">\n"
                +
                "            <tr>\n" +
                "              <td width=\"50%\" class=\"stack-column\" style=\"padding: 16px 20px; border-right: 1px solid rgba(255,255,255,0.06);\">\n"
                +
                "                <div style=\"color: #8C867E; font-size: 11px; font-weight: 800; letter-spacing: 0.1em; text-transform: uppercase;\">ORDER NUMBER</div>\n"
                +
                "                <div style=\"color: #FF5E00; font-size: 17px; font-weight: 800; margin-top: 4px; font-family: monospace;\">"
                + orderNum + "</div>\n" +
                "              </td>\n" +
                "              <td width=\"50%\" class=\"stack-column\" style=\"padding: 16px 20px;\">\n" +
                "                <div style=\"color: #8C867E; font-size: 11px; font-weight: 800; letter-spacing: 0.1em; text-transform: uppercase;\">ORDER DATE</div>\n"
                +
                "                <div style=\"color: #F5F2EB; font-size: 14px; font-weight: 600; margin-top: 4px;\">"
                + formattedDate + "</div>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "      <!-- SHIPMENT BOX (IF APPLICABLE) -->\n" +
                (shipmentBoxHtml.isEmpty() ? ""
                        : "      <tr>\n" +
                                "        <td style=\"padding: 0 28px;\" class=\"responsive-padding\">\n" +
                                "          " + shipmentBoxHtml + "\n" +
                                "        </td>\n" +
                                "      </tr>\n")
                +
                "\n" +
                "      <!-- ITEMS LIST -->\n" +
                "      <tr>\n" +
                "        <td style=\"padding: 0 28px 24px 28px;\" class=\"responsive-padding\">\n" +
                "          <div style=\"color: #8C867E; font-size: 11px; font-weight: 800; letter-spacing: 0.12em; text-transform: uppercase; margin-bottom: 12px;\">ORDER ITEMS ("
                + order.getItemCount() + ")</div>\n" +
                "          <table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color: #0E0B09; border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; border-collapse: separate;\">\n"
                +
                "            " + itemsHtml.toString() + "\n" +
                "          </table>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "      <!-- TOTAL SUMMARY -->\n" +
                "      <tr>\n" +
                "        <td style=\"padding: 0 28px 28px 28px;\" class=\"responsive-padding\">\n" +
                "          <table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color: #1A1412; border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 18px 20px;\">\n"
                +
                "            <tr>\n" +
                "              <td style=\"color: #A39D94; font-size: 14px; padding-bottom: 8px;\">Subtotal ("
                + order.getItemCount() + " items)</td>\n" +
                "              <td align=\"right\" style=\"color: #FFFFFF; font-size: 14px; font-weight: 600; padding-bottom: 8px;\">"
                + formattedTotal + "</td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"color: #A39D94; font-size: 14px; padding-bottom: 12px; border-bottom: 1px solid rgba(255,255,255,0.08);\">Shipping</td>\n"
                +
                "              <td align=\"right\" style=\"color: #10B981; font-size: 13px; font-weight: 700; padding-bottom: 12px; border-bottom: 1px solid rgba(255,255,255,0.08);\">FREE</td>\n"
                +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"color: #FFFFFF; font-size: 16px; font-weight: 800; padding-top: 12px;\">Total Amount</td>\n"
                +
                "              <td align=\"right\" style=\"color: #FF5E00; font-size: 20px; font-weight: 900; padding-top: 12px;\">"
                + formattedTotal + "</td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "      <!-- CUSTOMER & DELIVERY INFO -->\n" +
                "      <tr>\n" +
                "        <td style=\"padding: 0 28px 28px 28px;\" class=\"responsive-padding\">\n" +
                "          <table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color: #0E0B09; border: 1px solid rgba(255,255,255,0.08); border-radius: 8px;\">\n"
                +
                "            <tr>\n" +
                "              <td style=\"padding: 18px 20px;\">\n" +
                "                <div style=\"color: #8C867E; font-size: 11px; font-weight: 800; letter-spacing: 0.12em; text-transform: uppercase; margin-bottom: 8px;\">DELIVERY INFORMATION</div>\n"
                +
                "                <div style=\"color: #FFFFFF; font-size: 15px; font-weight: 700;\">"
                + escapeHtml(customerName) + "</div>\n" +
                "                <div style=\"color: #A39D94; font-size: 13px; line-height: 1.5; margin-top: 4px;\">"
                + escapeHtml(customerAddress) + "</div>\n" +
                (customerPhone != null && !customerPhone.isEmpty()
                        ? "                <div style=\"color: #8C867E; font-size: 13px; margin-top: 4px;\">Phone: <span style=\"color: #E2DDD5;\">"
                                + escapeHtml(customerPhone) + "</span></div>\n"
                        : "")
                +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "      <!-- CTA BUTTON -->\n" +
                "      <tr>\n" +
                "        <td align=\"center\" style=\"padding: 0 28px 36px 28px;\" class=\"responsive-padding\">\n" +
                "          <table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n"
                +
                "            <tr>\n" +
                "              <td align=\"center\">\n" +
                "                <a href=\"" + trackingUrl
                + "\" target=\"_blank\" class=\"btn-cta\" style=\"background-color: #FF5E00; color: #000000; font-size: 15px; font-weight: 800; text-decoration: none; padding: 15px 36px; border-radius: 6px; display: inline-block; letter-spacing: 0.05em; text-transform: uppercase; box-shadow: 0 4px 16px rgba(255,94,0,0.35);\">\n"
                +
                "                  TRACK YOUR ORDER\n" +
                "                </a>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "      <!-- FOOTER -->\n" +
                "      <tr>\n" +
                "        <td style=\"background-color: #0A0806; border-top: 1px solid rgba(255,255,255,0.06); padding: 28px 24px; text-align: center;\">\n"
                +
                "          <div style=\"color: #8C867E; font-size: 13px; line-height: 1.6; margin-bottom: 12px;\">\n" +
                "            Have questions or need to make changes? Reach out to our customer care team at <a href=\"mailto:"
                + supportEmail + "\" style=\"color: #FF5E00; text-decoration: none; font-weight: 700;\">" + supportEmail
                + "</a>.\n" +
                "          </div>\n" +
                "          <div style=\"color: #57524C; font-size: 11px; text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 4px;\">\n"
                +
                "            © " + java.time.Year.now().getValue() + " BUY2GO.IN — ALL RIGHTS RESERVED\n" +
                "          </div>\n" +
                "          <div style=\"color: #403B36; font-size: 10px;\">\n" +
                "            This is an automated order notification. Please do not reply directly to this email.\n" +
                "          </div>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "    </table>\n" +
                "  </center>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * Builds responsive visual 5-step order timeline: Placed -> Confirmed ->
     * Printing -> Shipped -> Delivered
     */
    private static String buildProgressTracker(OrderStatus currentStatus) {
        OrderStatus[] steps = { OrderStatus.New, OrderStatus.Confirmed, OrderStatus.Printing, OrderStatus.Shipped,
                OrderStatus.Delivered };
        String[] labels = { "Placed", "Confirmed", "Printing", "Shipped", "Delivered" };

        int currentIndex = 0;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == currentStatus) {
                currentIndex = i;
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(
                "<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color: #0E0B09; border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 18px 12px;\">");
        sb.append("<tr>");

        for (int i = 0; i < steps.length; i++) {
            boolean isCompleted = i < currentIndex;
            boolean isCurrent = i == currentIndex;

            String circleBg = (isCompleted || isCurrent) ? "#FF5E00" : "#221C18";
            String circleColor = (isCompleted || isCurrent) ? "#000000" : "#7E776F";
            String circleBorder = (isCompleted || isCurrent) ? "#FF5E00" : "#3D352F";
            String labelColor = isCurrent ? "#FF5E00" : (isCompleted ? "#FFFFFF" : "#7E776F");
            String iconText = isCompleted ? "✓" : String.valueOf(i + 1);

            sb.append("<td align=\"center\" width=\"20%\" style=\"vertical-align: top;\">")
                    .append("<div class=\"tracker-circle\" style=\"width: 26px; height: 26px; line-height: 26px; border-radius: 50%; background-color: ")
                    .append(circleBg)
                    .append("; border: 1px solid ").append(circleBorder).append("; color: ").append(circleColor)
                    .append("; font-size: 11px; font-weight: 900; margin: 0 auto 6px auto; text-align: center;\">")
                    .append(iconText)
                    .append("</div>")
                    .append("<div class=\"tracker-text\" style=\"color: ").append(labelColor)
                    .append("; font-size: 11px; font-weight: ").append(isCurrent ? "800" : "600")
                    .append("; text-transform: uppercase; letter-spacing: 0.05em;\">")
                    .append(labels[i])
                    .append("</div>")
                    .append("</td>");
        }

        sb.append("</tr>");
        sb.append("</table>");
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
