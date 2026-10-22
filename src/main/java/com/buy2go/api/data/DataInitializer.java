package com.buy2go.api.data;

import com.buy2go.api.constants.Constants;
import com.buy2go.api.entity.OrderEntity;
import com.buy2go.api.entity.ProductEntity;
import com.buy2go.api.entity.RoleEntity;
import com.buy2go.api.entity.UserAuthEntity;
import com.buy2go.api.entity.UserEntity;
import com.buy2go.api.enums.OrderStatus;
import com.buy2go.api.repository.OrderRepository;
import com.buy2go.api.repository.ProductRepository;
import com.buy2go.api.repository.RoleRepository;
import com.buy2go.api.repository.UserAuthRepository;
import com.buy2go.api.repository.UserRepository;
import com.buy2go.api.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Override
    public void run(String... args) {
        initRoles();
        initAdminUser();
        initCatalog();
        initDemoOrders();
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            log.info("Seeding default roles...");
            roleRepository.save(new RoleEntity(Constants.ROLE_ADMIN, "Administrator with full system privileges"));
            roleRepository.save(new RoleEntity(Constants.ROLE_CUSTOMER, "Standard customer user"));
            roleRepository.save(new RoleEntity(Constants.ROLE_STAFF, "Order fulfillment and staff member"));
        }
    }

    private void initAdminUser() {
        String adminEmail = "admin@buy2go.com";
        if (!userAuthRepository.existsByEmail(adminEmail)) {
            log.info("Creating default admin account ({} / Admin@123)...", adminEmail);

            UserEntity user = new UserEntity();
            user.setFirstName("Super");
            user.setLastName("Admin");
            user.setEmail(adminEmail);
            user.setPhone("9999999999");
            user.setRoleName(Constants.ROLE_ADMIN);
            user = userRepository.save(user);

            UserAuthEntity auth = new UserAuthEntity();
            auth.setEmail(adminEmail);
            auth.setPassword(PasswordUtils.encode("Admin@123"));
            auth.setFirstName("Super");
            auth.setLastName("Admin");
            auth.setUserId(user.getId());
            auth.setRoleName(Constants.ROLE_ADMIN);
            userAuthRepository.save(auth);
        }
    }

    private void initCatalog() {
        if (productRepository.count() == 0) {
            log.info("Seeding initial products collection...");

            productRepository.save(createProduct(
                    "Customised Key Chain",
                    "customised-key-chain",
                    "ACCESSORIES",
                    99,
                    99,
                    "From ₹99",
                    "/assests/1cliq-keychain.jpg",
                    "Durable acrylic key chains with double-sided HD print. Compact and scratch-resistant.",
                    true
            ));

            productRepository.save(createProduct(
                    "Mug Printing",
                    "mug-printing",
                    "DRINKWARE",
                    199,
                    199,
                    "From ₹199",
                    "/assests/1cliq-mug.jpg",
                    "Glossy ceramic mugs with microwave-safe vivid sublimation print.",
                    true
            ));

            productRepository.save(createProduct(
                    "Cap Printing",
                    "cap-printing",
                    "APPAREL",
                    249,
                    249,
                    "From ₹249",
                    "/assests/1cliq-cap.jpg",
                    "Structured 6-panel cotton twill caps with high-density embroidered monogram or print.",
                    true
            ));

            productRepository.save(createProduct(
                    "T-Shirt Printing",
                    "t-shirt-printing",
                    "APPAREL",
                    349,
                    349,
                    "From ₹349",
                    "/assests/1cliq-tshirt.jpg",
                    "100% combed cotton 220 GSM heavyweight tees with breathable DTG print.",
                    true
            ));
        }
    }

    private ProductEntity createProduct(String title, String slug, String category, double startingPrice, double price, String formattedPrice, String image, String description, boolean isLive) {
        ProductEntity doc = new ProductEntity();
        doc.setTitle(title);
        doc.setSlug(slug);
        doc.setCategory(category);
        doc.setStartingPrice(startingPrice);
        doc.setPrice(price);
        doc.setFormattedPrice(formattedPrice);
        doc.setImage(image);
        doc.setDescription(description);
        doc.setLive(isLive);
        doc.setStockQuantity(100);
        return doc;
    }

    private void initDemoOrders() {
        if (orderRepository.count() == 0) {
            log.info("Seeding initial demo order...");

            OrderEntity order = new OrderEntity();
            order.setOrderNumber("#9a4c3517");
            order.setCustomerName("Nicy");
            order.setLocation("Nellore 522002");
            order.setItemCount(1);
            order.setTotalAmount(99);
            order.setStatus(OrderStatus.New);

            order.setCustomer(new OrderEntity.CustomerDetail(
                    "Nicy",
                    "9490022488",
                    "nicy@example.com",
                    "Police colony, Nellore 522002"
            ));

            List<OrderEntity.OrderItemDetail> items = new ArrayList<>();
            OrderEntity.OrderItemDetail item = new OrderEntity.OrderItemDetail();
            item.setId("item-1");
            item.setTitle("Customised Key Chain");
            item.setVariant("Standard / Clear");
            item.setColor("Clear");
            item.setTextToPrint("Nicy & Friends 2026");
            item.setQuantity(1);
            item.setPrice(99);
            item.setPhotos(List.of("/assests/1cliq-keychain.jpg"));
            item.setPrintNotes("High-definition dual-sided acrylic print");
            items.add(item);
            order.setItems(items);

            order.setShipment(new OrderEntity.ShipmentDetail(
                    OrderStatus.New,
                    "Express Post",
                    "",
                    "Awaiting dispatch"
            ));

            order.getStatusHistory().add(new OrderEntity.OrderStatusHistory(
                    null,
                    OrderStatus.New,
                    "SYSTEM",
                    Instant.now(),
                    "Order created"
            ));

            orderRepository.save(order);
        }
    }
}
