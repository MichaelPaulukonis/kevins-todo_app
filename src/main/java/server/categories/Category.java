package server.categories;

import jakarta.persistence.*;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

@Embeddable
public class Category {
    // Note that name is required to be unique, since it is used to define equals() and hashCode().
    @KeywordField   // For exact matches - use @FullTextField to allow pattern matching
    @NotBlank(message = "Category name cannot be blank")
    @Column(name = "category_name")    // Note: this can't have "unique = true"
    private String categoryName;
    protected Category() {}  // Required zero-arg constructor for Hibernate

    /*
    ToDo: Gemini recommends storing a normalized name for searches and deduplication
    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;
        public Category(String name) {
        this.name = name;
        this.normalizedName = normalize(name);
    }
    private static String normalize(String input) {
        if (input == null) return "";
        return input.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-").replaceAll("-+", "-").trim();
    }
     */
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

    // A clean helper to check if this category matches an internal enum type
    // ToDo: Change name to validInternalCategory()
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
