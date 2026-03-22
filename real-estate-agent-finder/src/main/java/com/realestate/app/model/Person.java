package com.realestate.app.model;

/**
 * Abstract base class representing any person in the system.
 * Demonstrates: Abstraction, Encapsulation, Information Hiding.
 */
public abstract class Person {

    // ── Private Fields (Encapsulation / Information Hiding) ──
    private String id;
    private String name;
    private String email;
    private String phone;
    private String password;

    // ── Constructors ──

    protected Person() {
    }

    protected Person(String id, String name, String email, String phone, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    // ── Abstract Method (Polymorphism) ──

    /**
     * Returns the role of the person in the system.
     * Each subclass must provide its own implementation.
     */
    public abstract String getRole();

    /**
     * Returns a CSV-formatted string representation for file storage.
     * Each subclass must provide its own format.
     */
    public abstract String toFileString();

    // ── Getters ──

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    // ── Setters ──

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPassword(String pass) {
        this.password = pass;
    }

    // ── toString (human-readable) ──

    @Override
    public String toString() {
        return getRole() + "[id=" + id + ", name=" + name + ", email=" + email + "]";
    }
}
