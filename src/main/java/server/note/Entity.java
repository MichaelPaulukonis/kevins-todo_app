package server.note;

import jakarta.persistence.DiscriminatorValue;

// Includes Contact, Person, Company, Artist, etc.
// Need to use fully-qualified name to distinguish the annotation from this class
// ToDo: Override equals() and hashCode() for this class?
@jakarta.persistence.Entity
@DiscriminatorValue("ENTITY")
public class Entity extends Note {
    // ToDo: Support String customType for user-defined types (e.g. like "Band", "Author", "Client", etc.)?
    // ToDo: Should EntityType extend InternalCategory?
    public enum EntityType { PERSON, BUSINESS, ORGANIZATION, NON_PROFIT, ARTIST };
    private EntityType type;

    public Entity(String name, String description, EntityType type) {
        super(name, description);
        // ToDo: Alternately provide a default type (e.g. OTHER) for generic entities
        assert type != null : "Entity type cannot be null";
        this.type = type;
    }

    protected Entity() {}   // No-arg constructor required by Hibernate

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
