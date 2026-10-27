package server.element;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;

@Embeddable
public class Email {
    public enum EmailType { PERSONAL, WORK, OTHER };
    private String address;
    @Enumerated(EnumType.STRING)
    private EmailType type;

    public Email() {}   // No-arg constructor required by Hibernate

    public String getAddress() { return address; }
    public Email setAddress(String address) { this.address = address; return this; }
    public EmailType getType() { return type; }
    public Email setType(EmailType type) { this.type = type; return this; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email e)) return false;
        return Objects.equals(address, e.getAddress()) && Objects.equals(type, e.getType());
    }

    @Override
    public int hashCode() { return Objects.hash(address, type); }
}
