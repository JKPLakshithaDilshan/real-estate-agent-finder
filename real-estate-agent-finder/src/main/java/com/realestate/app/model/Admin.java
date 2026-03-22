package com.realestate.app.model;

/**
 * Represents an administrator of the system.
 * Inherits from Person. Demonstrates Inheritance.
 */
public class Admin extends Person {

    // ── Private Field (beyond Person) ──
    private String adminLevel; // e.g. "SUPER", "STANDARD"

    // ── Constructors ──

    public Admin() {
        super();
        this.adminLevel = "STANDARD";
    }

    public Admin(String id, String name, String email, String phone, String password) {
        super(id, name, email, phone, password);
        this.adminLevel = "STANDARD";
    }

    public Admin(String id, String name, String email, String phone,
            String password, String adminLevel) {
        super(id, name, email, phone, password);
        this.adminLevel = adminLevel;
    }

    // ── Polymorphic Overrides ──

    @Override
    public String getRole() {
        return "ADMIN";
    }

    /**
     * CSV format: id,name,email,phone,password,adminLevel
     */
    @Override
    public String toFileString() {
        return getId() + "," + getName() + "," + getEmail() + ","
                + getPhone() + "," + getPassword() + "," + adminLevel;
    }

    // ── Parse from CSV line ──

    public static Admin fromFileString(String line) {
        String[] p = line.split(",", -1);
        Admin admin = new Admin(p[0], p[1], p[2], p[3], p[4]);
        if (p.length > 5)
            admin.setAdminLevel(p[5]);
        return admin;
    }

    // ── Getter / Setter ──

    public String getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
    }
}
