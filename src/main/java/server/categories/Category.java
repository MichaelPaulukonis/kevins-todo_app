package server.categories;

import jakarta.persistence.*;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import java.util.Objects;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Note that name is required to be unique, since it is used to define equals() and hashCode().
    @KeywordField   // For exact matches - use @FullTextField to allow pattern matching
    @Column(name = "category_name", nullable = false, unique = true)
    private String categoryName;
    protected Category() {}  // Required zero-arg constructor for Hibernate

    // Factory method for user-defined string categories
    // ToDo: Add tests for both kinds of category
    public static Category of(String customName) {
        Category cat = new Category();
        cat.categoryName = customName;
        return cat;
    }

    // Factory method for internal typed enums
    public static Category of(InternalCategory type) { return of(type.getName()); }

    public String getName() {
        return categoryName;
    }
    public Long getId() { return id; }

    // A clean helper to check if this category matches an internal enum type
    public boolean validCategory(Enum<?> internalType) {
        return internalType != null && internalType.name().equalsIgnoreCase(this.categoryName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        return Objects.equals(categoryName, ((Category)o).categoryName);
    }

    @Override
    public int hashCode() { return Objects.hash(categoryName); }

    @Override
    public String toString() { return categoryName; }
}
