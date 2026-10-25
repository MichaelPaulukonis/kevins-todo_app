package server.note;

import jakarta.persistence.*;
import server.element.*;
import java.util.ArrayList;
import java.util.List;

// Includes Person, Company etc.
// ToDo: Add list of email addresses (define Email class?)!
@jakarta.persistence.Entity
@DiscriminatorValue("CONTACT")
public class Contact extends Entity {
    private String firstName;
    private String lastName;
    @ElementCollection(targetClass = Address.class)
    @CollectionTable(name = "contact_addresses", joinColumns = @JoinColumn(name = "contact_id"))
    private List<Address> addresses;
    @ElementCollection(targetClass = Phone.class)
    @CollectionTable(name = "contact_phones", joinColumns = @JoinColumn(name = "contact_id"))
    private List<Phone> phoneNumbers;

    public Contact(String displayName, EntityType type) {
        super(displayName, null, type);
        this.addresses = new ArrayList<>();
        this.phoneNumbers = new ArrayList<>();
    }

    private Contact() {}    // No-arg constructor required by Hibernate

    // ToDo: Should type be final or do we need a setter?
    public String getFirstName() { return firstName; }
    public Contact setFirstName(String firstName) { this.firstName = firstName; return this; }
    public String getLastName() { return lastName; }
    public Contact setLastName(String lastName) { this.lastName = lastName; return this; }
    public Contact setContactName(String firstName, String lastName) {
        validateFirstNameOrLastNameIsSet(firstName, lastName);
        this.firstName = firstName;
        this.lastName = lastName;
        // ToDo: Is this case even possible (since a name is required to create a Contact)?
        if (name == null || name.isEmpty()) {
            setName(firstName + " " + lastName);
        }
        return this;
    }
    public List<Address> getAddresses() { return addresses; }
    public Contact addAddress(Address newAddress) { addresses.add(newAddress); return this; }
    public List<Phone> getPhoneNumbers() { return phoneNumbers; }
    public Contact addPhoneNumber(String number, Phone.PhoneType type) {
        phoneNumbers.add(new Phone().setNumber(number).setType(type)); return this;
    }
    // ToDo: Remove this version?
    public Contact addPhoneNumber(Phone newPhone) { phoneNumbers.add(newPhone); return this; }

    public static void validateFirstNameOrLastNameIsSet(String firstName, String lastName) {
        if ((firstName == null || firstName.isEmpty()) && (lastName == null || lastName.isEmpty())) {
            throw new IllegalArgumentException("At least one name is required for a contact");
        }
    }

    public static Contact newPerson(String firstName, String lastName) {
        validateFirstNameOrLastNameIsSet(firstName, lastName);
        return new Contact(firstName + " " + lastName, Entity.EntityType.PERSON).setContactName(firstName, lastName);
    }

    public static Contact newBusiness(String name, String description) {
        Contact newBusiness = new Contact(name, Entity.EntityType.BUSINESS);
        newBusiness.setDescription(description);
        return newBusiness;
    }

    // ToDo: Update to add address and phone info
    @Override
    public String toString() {
        String result = super.toString();
        if (description != null && !description.isEmpty()) {
            result += "\n" + description;
        }
/*
        // ToDo: Update to add multiple numbers
        if (address != null) {
            result += "\n" + address.toString();
        }
*/
        return result;
    }
}
