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
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "category_id", nullable = true)
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
    // ToDo: Need to call persistCategoryIfNew() (in same session/transaction) to save type to DB
    public Product setType(Category type) {
        this.type = type;
        if (categories.contains(type)) {
            LOG.warn("Category already exists for Product '{}' (cannot add): {}", getName(), type.getName());
        }
        else {
            // ToDo: Save Category to DB as well and update Product in DB
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
