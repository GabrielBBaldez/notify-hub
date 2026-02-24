package io.notifyhub.demo;

import io.notifyhub.core.Notifiable;

/**
 * Simulates a JPA User entity implementing Notifiable.
 * In a real project, this would be your @Entity User class.
 */
public class FakeUser implements Notifiable {

    private final String name;
    private final String email;
    private final String phone;

    public FakeUser(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    @Override
    public String getNotifyEmail() { return email; }

    @Override
    public String getNotifyPhone() { return phone; }

    @Override
    public String getNotifyName() { return name; }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}
