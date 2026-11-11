package server.note;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;
import server.categories.Category;
import server.categories.InternalCategory;

import static server.database.HibernateUtil.*;

/**
 * This is the base class for all Note objects. It can be used for leaf-type notes with no child elements.
 */
// ToDo: Provide separate logger here for Note classes (instead of server.NoteUtils.LOG)?
@Entity
@Table(name = "note_base")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Indexed
@DiscriminatorColumn(name = "note_type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("SIMPLE_NOTE")
public class NoteBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Self-reference back to the parent Note container row, required for Hibernate
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_note_id")
    private Note parent;
    @KeywordField
    protected String name;
    protected String description;
    // ToDo: Should a child Note inherit its parent's categories (maybe conceptually but not literally)?
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "categories", joinColumns = @JoinColumn(name = "note_id"))
    @IndexedEmbedded(structure = ObjectStructure.NESTED)
    protected Set<Category> categories = new HashSet<>();
    private final LocalDateTime created = LocalDateTime.now();
    private Boolean completed = false;

    // ToDo: Maybe require name and get rid of complicated "name or description" logic?
    public NoteBase(String newName, String newDescription) {
        // ToDo: Add validation method for name and description (i.e. not null/empty)
        this.name = setName(newName, newDescription);
        // ToDo: Set description to empty if we use it for the name?
        this.description = newDescription;
    }

    protected NoteBase() {}    // Required zero-arg constructor for Hibernate

    // Enforce that either name of description must be provided
    private static String setName(String newName, String newDescription) {
        if (newName == null || newName.isEmpty()) {
            if (newDescription == null || newDescription.isEmpty()) {
                throw new IllegalArgumentException("Must provide a name or description for a note");
            }
            return newDescription;
        }
        return newName;
    }

    public Note getParent() { return parent; }
    public void setParent(Note parent) { this.parent = parent; }
    public Long getId() { return id; }
    public String getName() { return name; }
    // ToDo: Move name validation to the constructor and throw an exception if for null/empty name?
    public NoteBase setName(String newName) { this.name = setName(newName, description); return this; }
    public String getDescription() { return description; }
    public NoteBase setDescription(String newDescription) { description = newDescription; return this; }
    // ToDo: Add Javadoc
    public Set<Category> getCategories() { return categories; }
    // Note that these category methods do NOT update the NoteOrganizer category tracker!
    public NoteBase addCategory(Category cat) { categories.add(cat); return this; }
    public NoteBase addCategory(String cat) { categories.add(Category.of(cat)); return this; }
    public NoteBase addCategory(InternalCategory cat) { categories.add(Category.of(cat)); return this; }
    public NoteBase addCategories(Set<Category> cats) { categories.addAll(cats); return this; }
    public NoteBase removeCategory(String cat) { categories.remove(Category.of(cat)); return this; }
    public LocalDateTime getDateCreated() { return created; }
    public boolean getCompleted() { return completed != null && completed; }
    public NoteBase setCompleted(boolean isCompleted) { completed = isCompleted; return this; }

    public boolean hasCategory(String category) { return categories.contains(category); }
    // ToDo: Need Javadoc (e.g. no categories automatically returns true)
    public boolean hasAllCategories(Set<String> cats) { return cats == null || categories.containsAll(cats); }
    public boolean hasAnyCategory(Set<String> cats) {
        return cats == null || cats.isEmpty() || cats.stream().anyMatch(cat -> categories.contains(cat));
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) { return equalsHelper(this, obj); }

    @Override
    // Apparently this is a standard Hibernate kludge (HashSet falls back to equals() for resolving collisions)
    public int hashCode() { return getClass().hashCode(); }
    // ToDo: Support toString() method?
}
