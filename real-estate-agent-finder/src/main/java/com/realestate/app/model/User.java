package com.realestate.app.model;

/**
 * Represents a registered client/user of the system.
 * Inherits from Person. Demonstrates Inheritance, Encapsulation.
 */
public class User extends Person {

    // ── Private Fields ──
    private String preferredLocation; // e.g. "Colombo", "Kandy"
    private String propertyType; // e.g. "Residential", "Commercial"
    private boolean active;

    // ── Constructors ──

    public User() {
        super();
        this.active = true;
    }

    public User(String id, String name, String email, String phone, String password) {
        super(id, name, email, phone, password);
        this.active = true;
    }

    public User(String id, String name, String email, String phone, String password,
            String preferredLocation, String propertyType) {
        super(id, name, email, phone, password);
        this.preferredLocation = preferredLocation;
        this.propertyType = propertyType;
        this.active = true;
    }

    // ── Polymorphic Overrides ──

    @Override
    public String getRole() {
        return "USER";
    }

    /**
     * CSV format: id,name,email,phone,password,preferredLocation,propertyType,active
     */
    @Override
    public String toFileString() {
        return getId() + "," + getName() + "," + getEmail() + "," + getPhone() + ","
                + getPassword() + "," + nullSafe(preferredLocation) + "," + nullSafe(propertyType)
                + "," + active;
    }

    // ── Parse from CSV line ──

    public static User fromFileString(String line) {
        String[] p = line.split(",", -1);
        User user = new User(p[0], p[1], p[2], p[3], p[4]);
        if (p.length > 5)
            user.setPreferredLocation(p[5].isBlank() ? null : p[5]);
        if (p.length > 6)
            user.setPropertyType(p[6].isBlank() ? null : p[6]);
        if (p.length > 7)
            user.setActive(Boolean.parseBoolean(p[7]));
        return user;
    }

    // ── Private helper ──

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    // ── Getters ──

    public String getPreferredLocation() {
        return preferredLocation;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public boolean isActive() {
        return active;
    }

    public String getUsername() {
        return getName();
    }

    // ── Setters ──

    public void setPreferredLocation(String preferredLocation) {
        this.preferredLocation = preferredLocation;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setUsername(String username) {
        setName(username);
    }
}
