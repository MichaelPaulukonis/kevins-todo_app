package server.note;

import server.element.Address;

// Includes Contact, Person, Company, Artist, etc.
public class Entity extends Note {
    // ToDo: Support user-defined types (e.g. "Band", "Author", "Client", etc.)?
    public enum EntityType { PERSON, BUSINESS, ORGANIZATION, NON_PROFIT, ARTIST };
    private final EntityType type;
    // ToDo: Define getter/setter for contactInfo
    // ToDo: Move addresses and phoneNumbers here?
    private Address address;

    public Entity(String name, String description, EntityType type) {
        super(name, description);
        this.type = type;
    }

    public EntityType getType() { return type; }

    @Override
    public String toString() {
        String result = type.toString() + ": " + name;
        if (description != null && !description.isEmpty()) {
            result += "\n" + description;
        }
        return result;
    }
}
