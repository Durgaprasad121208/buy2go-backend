package com.buy2go.api.data;

import com.buy2go.api.constants.Constants;
import com.buy2go.api.entity.RoleEntity;
import com.buy2go.api.entity.UserAuthEntity;
import com.buy2go.api.entity.UserEntity;
import com.buy2go.api.repository.RoleRepository;
import com.buy2go.api.repository.UserAuthRepository;
import com.buy2go.api.repository.UserRepository;
import com.buy2go.api.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;

    @Override
    public void run(String... args) {
        initRoles();
        initAdminUser();
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
}
