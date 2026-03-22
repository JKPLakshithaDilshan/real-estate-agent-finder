package com.realestate.app.config;

import com.realestate.app.model.Admin;
import com.realestate.app.service.AdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DefaultAdminSeeder implements ApplicationRunner {
    private final AdminService adminService;

    @Value("${app.seed.admin.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.admin.email:admin@realestate.com}")
    private String seedEmail;

    @Value("${app.seed.admin.password:Admin@123}")
    private String seedPassword;

    @Value("${app.seed.admin.name:System Admin}")
    private String seedName;

    public DefaultAdminSeeder(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }

        if (!adminService.getAllAdmins().isEmpty()) {
            return;
        }

        Admin admin = new Admin();
        admin.setName(seedName);
        admin.setEmail(seedEmail);
        admin.setPhone("0000000000");
        admin.setPassword(seedPassword);
        admin.setAdminLevel("SUPER");

        adminService.saveAdmin(admin);
    }
}
