package server.note;

// Includes Contact, Person, Company, Artist, etc.
public class Entity extends Note {
    // ToDo: Support String customType for user-defined types (e.g. like "Band", "Author", "Client", etc.)?
    public enum EntityType { PERSON, BUSINESS, ORGANIZATION, NON_PROFIT, ARTIST };
    private final EntityType type;

    public Entity(String name, String description, EntityType type) {
        super(name, description);
        // ToDo: Alternately provide a default type (e.g. OTHER) for generic entities
        assert type != null : "Entity type cannot be null";
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
