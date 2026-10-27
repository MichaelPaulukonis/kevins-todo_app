package server.note;

import jakarta.persistence.*;
import server.categories.Category;
import server.categories.MediaType;
import server.element.Price;

import static server.NoteUtils.LOG;

@jakarta.persistence.Entity
@DiscriminatorValue("PRODUCT")
public class Product extends Note {
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "owner_id")
    private Entity owner;
    private Category type;
    private Price productPrice;

    // ToDo: Include ProductType (extends InternalCategory) in constructor
    public Product(String name, String description, Entity owner) {
        super(name, description);
        this.owner = owner;
    }

    public Product(String name) { super(name, null); }

    protected Product() {}    // No-arg constructor required by Hibernate

    public Entity getOwner() { return owner; }
    public Product setOwner(Entity newOwner) { owner = newOwner; return this; }
    public Category getType() { return type; }
    // ToDo: Do I need to update Category tracking in NoteOrganizer for this?
    public Product setType(Category type) {
        this.type = type;
        if (categories.contains(type.getName())) {
            LOG.warn("Product category already exists (cannot add): {}", type.getName());
        }
        else {
            this.addCategory(type.getName());
        }
        return this;
    }
    public Product setType(MediaType type) { setType(Category.of(type)); return this; }
    public Price getPrice() { return productPrice; }
    public Product setPrice(Price productPrice) { this.productPrice = productPrice; return this; }
    public Product setPrice(double price) { this.productPrice = new Price(price); return this; }

/*
    // ToDo: Define toString()
    @Override
    public String toString() { }
*/
}
