package com.buy2go.api.service.impl;

import com.buy2go.api.entity.OrderEntity;
import com.buy2go.api.dto.PagedResponse;
import com.buy2go.api.dto.request.ItemCustomizationDTO;
import com.buy2go.api.dto.request.OrderRequestDTO;
import com.buy2go.api.dto.request.OrderStatusUpdateDTO;
import com.buy2go.api.dto.request.ShipmentUpdateDTO;
import com.buy2go.api.dto.response.OrderResponseDTO;
import com.buy2go.api.enums.OrderStatus;
import com.buy2go.api.exception.ResourceNotFoundException;
import com.buy2go.api.exception.SystemException;
import com.buy2go.api.mapper.ModelMapper;
import com.buy2go.api.repository.OrderRepository;
import com.buy2go.api.repository.UserRepository;
import com.buy2go.api.service.FileStorageService;
import com.buy2go.api.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.buy2go.api.utils.DateUtil;
import com.buy2go.api.utils.OrderNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Override
    public OrderResponseDTO createOrder(OrderRequestDTO request) {
        OrderEntity doc = new OrderEntity();
        doc.setOrderNumber(OrderNumberGenerator.generateOrderNumber());

        OrderEntity.CustomerDetail customer = request.getCustomer() != null
                ? request.getCustomer()
                : new OrderEntity.CustomerDetail();
        if (customer.getEmail() == null || customer.getEmail().trim().isEmpty()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String authEmail = auth.getName();
                customer.setEmail(authEmail);
                userRepository.findByEmailAndIsDeletedFalse(authEmail).ifPresent(u -> {
                    if (customer.getPhone() == null || customer.getPhone().trim().isEmpty()) {
                        customer.setPhone(u.getPhone());
                    }
                    if (customer.getName() == null || customer.getName().trim().isEmpty() || "Customer".equals(customer.getName())) {
                        customer.setName((u.getFirstName() + " " + (u.getLastName() != null ? u.getLastName() : "")).trim());
                    }
                });
            }
        }
        doc.setCustomer(customer);
        doc.setCustomerName(customer.getName() != null && !customer.getName().trim().isEmpty() ? customer.getName() : "Customer");
        doc.setLocation(request.getLocation() != null ? request.getLocation() : (customer.getAddress() != null ? customer.getAddress() : ""));

        List<OrderEntity.OrderItemDetail> items = request.getItems() != null ? request.getItems() : new ArrayList<>();
        int itemIndex = 1;
        for (OrderEntity.OrderItemDetail item : items) {
            if (item.getId() == null || item.getId().trim().isEmpty()) {
                item.setId("item-" + itemIndex++);
            }
        }
        doc.setItems(items);

        // Synchronize photos list and primary photo for each item
        for (OrderEntity.OrderItemDetail item : items) {
            if ((item.getPhoto() == null || item.getPhoto().isEmpty()) && item.getPhotos() != null && !item.getPhotos().isEmpty()) {
                item.setPhoto(item.getPhotos().get(0));
            } else if (item.getPhoto() != null && !item.getPhoto().isEmpty() && (item.getPhotos() == null || item.getPhotos().isEmpty())) {
                item.setPhotos(new ArrayList<>(List.of(item.getPhoto())));
            }
        }

        // Synchronize order-level photos
        List<String> orderPhotos = request.getPhotos() != null ? new ArrayList<>(request.getPhotos()) : new ArrayList<>();
        String orderPhoto = request.getPhoto();
        if ((orderPhoto == null || orderPhoto.isEmpty()) && !orderPhotos.isEmpty()) {
            orderPhoto = orderPhotos.get(0);
        }
        if (orderPhoto == null || orderPhoto.isEmpty()) {
            for (OrderEntity.OrderItemDetail item : items) {
                if (item.getPhoto() != null && !item.getPhoto().isEmpty()) {
                    orderPhoto = item.getPhoto();
                    break;
                }
            }
        }
        if (orderPhotos.isEmpty()) {
            for (OrderEntity.OrderItemDetail item : items) {
                if (item.getPhotos() != null && !item.getPhotos().isEmpty()) {
                    orderPhotos.addAll(item.getPhotos());
                } else if (item.getPhoto() != null && !item.getPhoto().isEmpty()) {
                    orderPhotos.add(item.getPhoto());
                }
            }
        }
        if (orderPhoto != null && !orderPhotos.contains(orderPhoto)) {
            orderPhotos.add(0, orderPhoto);
        }
        doc.setPhoto(orderPhoto);
        doc.setPhotos(orderPhotos);

        doc.setItemCount(doc.getItems().stream().mapToInt(OrderEntity.OrderItemDetail::getQuantity).sum());
        doc.setTotalAmount(request.getTotalAmount());
        doc.setStatus(OrderStatus.New);

        OrderEntity.ShipmentDetail shipment = new OrderEntity.ShipmentDetail();
        shipment.setStatus(OrderStatus.New);
        shipment.setCourier("Pending Assignment");
        shipment.setTrackingNumber("");
        shipment.setNotes("");
        doc.setShipment(shipment);

        OrderEntity.OrderStatusHistory history = new OrderEntity.OrderStatusHistory(
                null,
                OrderStatus.New,
                "SYSTEM",
                Instant.now(),
                "Order placed successfully"
        );
        doc.getStatusHistory().add(history);

        doc = orderRepository.save(doc);
        return ModelMapper.toOrderResponse(doc);
    }

    @Override
    public PagedResponse<OrderResponseDTO> getAllOrders(int page, int size, OrderStatus status, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<OrderEntity> orderPage;
        boolean hasSearch = search != null && !search.trim().isEmpty();

        if (status != null && hasSearch) {
            orderPage = orderRepository.searchOrdersWithStatus(status, search.trim(), pageable);
        } else if (status != null) {
            orderPage = orderRepository.findByStatusAndIsDeletedFalse(status, pageable);
        } else if (hasSearch) {
            orderPage = orderRepository.searchOrders(search.trim(), pageable);
        } else {
            orderPage = orderRepository.findByIsDeletedFalse(pageable);
        }

        List<OrderResponseDTO> content = orderPage.getContent().stream()
                .map(ModelMapper::toOrderResponse)
                .toList();

        return PagedResponse.<OrderResponseDTO>builder()
                .content(content)
                .pageNumber(orderPage.getNumber())
                .pageSize(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .last(orderPage.isLast())
                .build();
    }

    @Override
    public OrderResponseDTO getOrderById(String id) {
        OrderEntity doc = findOrder(id);
        return ModelMapper.toOrderResponse(doc);
    }

    @Override
    public OrderResponseDTO getOrderByOrderNumber(String orderNumber) {
        OrderEntity doc = findOrder(orderNumber);
        return ModelMapper.toOrderResponse(doc);
    }

    @Override
    public OrderResponseDTO updateOrderStatus(String id, OrderStatusUpdateDTO request, String username) {
        OrderEntity doc = findOrder(id);

        OrderStatus oldStatus = doc.getStatus();
        OrderStatus newStatus = request.getStatus();

        doc.setStatus(newStatus);
        if (doc.getShipment() != null) {
            doc.getShipment().setStatus(newStatus);
        }

        OrderEntity.OrderStatusHistory history = new OrderEntity.OrderStatusHistory(
                oldStatus,
                newStatus,
                username != null ? username : "ADMIN",
                Instant.now(),
                request.getRemarks() != null ? request.getRemarks() : "Status updated to " + newStatus
        );
        doc.getStatusHistory().add(history);
        doc.setUpdatedAt(Instant.now());

        doc = orderRepository.save(doc);
        return ModelMapper.toOrderResponse(doc);
    }

    @Override
    public OrderResponseDTO updateShipmentDetails(String id, ShipmentUpdateDTO request) {
        OrderEntity doc = findOrder(id);

        OrderEntity.ShipmentDetail shipment = doc.getShipment();
        if (shipment == null) {
            shipment = new OrderEntity.ShipmentDetail();
            doc.setShipment(shipment);
        }

        if (request.getStatus() != null) {
            doc.setStatus(request.getStatus());
            shipment.setStatus(request.getStatus());
        }
        if (request.getCourier() != null) shipment.setCourier(request.getCourier());
        if (request.getTrackingNumber() != null) shipment.setTrackingNumber(request.getTrackingNumber());
        if (request.getNotes() != null) shipment.setNotes(request.getNotes());

        doc.setUpdatedAt(Instant.now());
        doc = orderRepository.save(doc);
        return ModelMapper.toOrderResponse(doc);
    }

    @Override
    public OrderResponseDTO cancelOrder(String id, String remarks, String username) {
        OrderStatusUpdateDTO updateDTO = new OrderStatusUpdateDTO(OrderStatus.Cancelled, remarks != null ? remarks : "Order cancelled");
        return updateOrderStatus(id, updateDTO, username);
    }

    @Override
    public String uploadOrderPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new SystemException("Uploaded file cannot be empty");
        }
        return fileStorageService.storeFile(file, "orders/photos");
    }

    @Override
    public List<String> uploadOrderPhotos(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> urls = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f != null && !f.isEmpty()) {
                urls.add(fileStorageService.storeFile(f, "orders/photos"));
            }
        }
        return urls;
    }

    @Override
    public java.io.InputStream downloadPhoto(String urlOrObjectName) {
        return fileStorageService.downloadFile(urlOrObjectName);
    }

    @Override
    public String resolvePhotoUrl(String idOrUrl) {
        if (idOrUrl == null || idOrUrl.trim().isEmpty()) {
            return null;
        }
        String clean = idOrUrl.trim();
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            return clean;
        }

        try {
            OrderEntity order = findOrder(clean);
            if (order != null) {
                if (order.getPhoto() != null && !order.getPhoto().isEmpty()) {
                    return order.getPhoto();
                }
                if (order.getItems() != null) {
                    for (OrderEntity.OrderItemDetail it : order.getItems()) {
                        if (it.getPhoto() != null && !it.getPhoto().isEmpty()) {
                            return it.getPhoto();
                        }
                    }
                }
                if (order.getPhotos() != null && !order.getPhotos().isEmpty()) {
                    return order.getPhotos().get(0);
                }
            }
        } catch (Exception ignored) {}

        List<OrderEntity> allOrders = orderRepository.findByIsDeletedFalse(PageRequest.of(0, 1000)).getContent();
        for (OrderEntity ord : allOrders) {
            if (ord.getItems() != null) {
                for (OrderEntity.OrderItemDetail it : ord.getItems()) {
                    if (clean.equalsIgnoreCase(it.getId()) || clean.equalsIgnoreCase(it.getPhotoId())) {
                        if (it.getPhoto() != null && !it.getPhoto().isEmpty()) {
                            return it.getPhoto();
                        }
                    }
                    if (it.getPhoto() != null && it.getPhoto().toLowerCase().contains(clean.toLowerCase())) {
                        return it.getPhoto();
                    }
                }
            }
            if (ord.getPhoto() != null && ord.getPhoto().toLowerCase().contains(clean.toLowerCase())) {
                return ord.getPhoto();
            }
        }

        return clean;
    }

    @Override
    public com.buy2go.api.dto.response.PhotoDetailsDTO getPhotoDetails(String idOrUrl) {
        if (idOrUrl == null || idOrUrl.trim().isEmpty()) {
            throw new ResourceNotFoundException("ID or URL cannot be empty");
        }
        String clean = idOrUrl.trim();

        try {
            OrderEntity order = findOrder(clean);
            if (order != null) {
                String photoUrl = order.getPhoto();
                if ((photoUrl == null || photoUrl.isEmpty()) && order.getItems() != null && !order.getItems().isEmpty()) {
                    photoUrl = order.getItems().get(0).getPhoto();
                }
                return com.buy2go.api.dto.response.PhotoDetailsDTO.builder()
                        .id(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .photoId(order.getPhotoId() != null ? order.getPhotoId() : OrderEntity.extractPhotoId(photoUrl))
                        .photoUrl(photoUrl)
                        .photos(order.getPhotos() != null ? order.getPhotos() : (photoUrl != null ? List.of(photoUrl) : Collections.emptyList()))
                        .build();
            }
        } catch (Exception ignored) {}

        String resolvedUrl = resolvePhotoUrl(clean);
        String photoId = OrderEntity.extractPhotoId(resolvedUrl != null ? resolvedUrl : clean);

        return com.buy2go.api.dto.response.PhotoDetailsDTO.builder()
                .id(clean)
                .orderNumber(clean.startsWith("#") ? clean : null)
                .photoId(photoId)
                .photoUrl(resolvedUrl)
                .photos(resolvedUrl != null ? List.of(resolvedUrl) : Collections.emptyList())
                .build();
    }

    @Override
    public OrderResponseDTO updateItemCustomization(String orderId, String itemId, ItemCustomizationDTO customization) {
        OrderEntity doc = findOrder(orderId);

        OrderEntity.OrderItemDetail targetItem = doc.getItems().stream()
                .filter(i -> itemId.equalsIgnoreCase(i.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in order with ID: " + itemId));

        if (customization.getVariant() != null) targetItem.setVariant(customization.getVariant());
        if (customization.getColor() != null) targetItem.setColor(customization.getColor());
        if (customization.getTextToPrint() != null) targetItem.setTextToPrint(customization.getTextToPrint());
        if (customization.getPhoto() != null) targetItem.setPhoto(customization.getPhoto());
        if (customization.getPrintNotes() != null) targetItem.setPrintNotes(customization.getPrintNotes());

        if (doc.getPhoto() == null || doc.getPhoto().isEmpty()) {
            doc.setPhoto(targetItem.getPhoto());
        }

        doc.setUpdatedAt(Instant.now());
        doc = orderRepository.save(doc);
        return ModelMapper.toOrderResponse(doc);
    }

    @Override
    public OrderResponseDTO updateItemPhoto(String orderId, String itemId, String photoUrl) {
        OrderEntity doc = findOrder(orderId);

        OrderEntity.OrderItemDetail targetItem = doc.getItems().stream()
                .filter(i -> itemId.equalsIgnoreCase(i.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in order with ID: " + itemId));

        targetItem.setPhoto(photoUrl);
        if (doc.getPhoto() == null || doc.getPhoto().isEmpty()) {
            doc.setPhoto(photoUrl);
        }

        doc.setUpdatedAt(Instant.now());
        doc = orderRepository.save(doc);
        return ModelMapper.toOrderResponse(doc);
    }

    @Override
    public OrderResponseDTO removeItemPhoto(String orderId, String itemId) {
        OrderEntity doc = findOrder(orderId);

        OrderEntity.OrderItemDetail targetItem = doc.getItems().stream()
                .filter(i -> itemId.equalsIgnoreCase(i.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in order with ID: " + itemId));

        targetItem.setPhoto(null);
        doc.setUpdatedAt(Instant.now());
        doc = orderRepository.save(doc);
        return ModelMapper.toOrderResponse(doc);
    }

    @Override
    public OrderResponseDTO updateOrderPhoto(String orderId, String photoUrl) {
        OrderEntity doc = findOrder(orderId);
        doc.setPhoto(photoUrl);
        doc.setUpdatedAt(Instant.now());
        doc = orderRepository.save(doc);
        return ModelMapper.toOrderResponse(doc);
    }

    @Override
    public List<OrderResponseDTO> createBulkOrders(List<OrderRequestDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        List<OrderResponseDTO> results = new ArrayList<>();
        for (OrderRequestDTO req : requests) {
            results.add(createOrder(req));
        }
        return results;
    }

    @Override
    public List<OrderResponseDTO> getOrdersByCustomer(String email, String phone) {
        String cleanEmail = email != null && !email.trim().isEmpty() ? email.trim() : null;
        String cleanPhone = phone != null && !phone.trim().isEmpty() ? phone.trim() : null;

        // If email is provided but not phone, look up user profile to also check orders placed by their registered phone
        if (cleanEmail != null && cleanPhone == null) {
            cleanPhone = userRepository.findByEmailAndIsDeletedFalse(cleanEmail)
                    .map(com.buy2go.api.entity.UserEntity::getPhone)
                    .filter(p -> p != null && !p.trim().isEmpty())
                    .orElse(null);
        }

        List<OrderEntity> orders;
        if (cleanEmail != null && cleanPhone != null) {
            orders = new ArrayList<>(orderRepository.findByCustomerIdentifier(cleanEmail));
            List<OrderEntity> phoneOrders = orderRepository.findByCustomerPhone(cleanPhone);
            for (OrderEntity po : phoneOrders) {
                if (orders.stream().noneMatch(o -> o.getId().equals(po.getId()))) {
                    orders.add(po);
                }
            }
        } else if (cleanEmail != null) {
            orders = orderRepository.findByCustomerEmail(cleanEmail);
        } else if (cleanPhone != null) {
            orders = orderRepository.findByCustomerPhone(cleanPhone);
        } else {
            return Collections.emptyList();
        }

        return orders.stream()
                .map(ModelMapper::toOrderResponse)
                .toList();
    }

    @Override
    public OrderResponseDTO removeOrderPhoto(String orderId) {
        OrderEntity doc = findOrder(orderId);
        doc.setPhoto(null);
        doc.setUpdatedAt(Instant.now());
        doc = orderRepository.save(doc);
        return ModelMapper.toOrderResponse(doc);
    }

    private OrderEntity findOrder(String idOrOrderNumber) {
        if (idOrOrderNumber == null || idOrOrderNumber.trim().isEmpty()) {
            throw new ResourceNotFoundException("Order ID or number cannot be empty");
        }
        String clean = idOrOrderNumber.trim();
        String withHash = clean.startsWith("#") ? clean : "#" + clean;
        String withoutHash = clean.startsWith("#") ? clean.substring(1) : clean;

        return orderRepository.findByIdAndIsDeletedFalse(clean)
                .or(() -> orderRepository.findByOrderNumberIgnoreCaseAndIsDeletedFalse(clean))
                .or(() -> orderRepository.findByOrderNumberIgnoreCaseAndIsDeletedFalse(withHash))
                .or(() -> orderRepository.findByOrderNumberIgnoreCaseAndIsDeletedFalse(withoutHash))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID/number: " + idOrOrderNumber));
    }

    @Override
    public ByteArrayInputStream downloadOrdersExcel(OrderStatus status) {
        List<OrderEntity> orders;
        if (status != null) {
            orders = orderRepository.findByStatusAndIsDeletedFalse(status, PageRequest.of(0, 1000)).getContent();
        } else {
            orders = orderRepository.findByIsDeletedFalse(PageRequest.of(0, 1000)).getContent();
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Orders");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] columns = {"Order Number", "Customer Name", "Phone", "Status", "Items Count", "Total Amount (INR)", "Courier", "Tracking Number", "Created At"};
            Row headerRow = sheet.createRow(0);

            for (int col = 0; col < columns.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns[col]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (OrderEntity o : orders) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(o.getOrderNumber());
                row.createCell(1).setCellValue(o.getCustomerName() != null ? o.getCustomerName() : "");
                row.createCell(2).setCellValue(o.getCustomer() != null && o.getCustomer().getPhone() != null ? o.getCustomer().getPhone() : "");
                row.createCell(3).setCellValue(o.getStatus() != null ? o.getStatus().name() : "");
                row.createCell(4).setCellValue(o.getItemCount());
                row.createCell(5).setCellValue(o.getTotalAmount());
                row.createCell(6).setCellValue(o.getShipment() != null ? o.getShipment().getCourier() : "");
                row.createCell(7).setCellValue(o.getShipment() != null ? o.getShipment().getTrackingNumber() : "");
                row.createCell(8).setCellValue(DateUtil.formatInstant(o.getCreatedAt()));
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report: " + e.getMessage(), e);
        }
    }
}
