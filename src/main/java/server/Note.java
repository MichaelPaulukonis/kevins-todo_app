package server;

import server.categories.MediaType;
import server.element.*;
import server.categories.CategoryTag;

import javax.persistence.*;
import java.util.*;
import org.apache.logging.log4j.*;

// Need tags to indicate relevant search categories
// This is the base of the "Note tree"
/**
 * This is the base class for notes and to-do items.
 */
@Entity
@Table(name = "NOTES")
public class Note extends NoteElement<Note> {
    @Id
    @Column(name="NOTE_ID")
    @GeneratedValue
    private Long noteId;
    private ArrayList<NoteElement<?>> elements;
    // Tracks the location of a NoteElement in the elements list
    // ToDo: Not sure how/where this was supposed to be used
    // I get a compile error from updateElementLocatorAfterAdd() if I use NoteElement<?>>
    // ToDo: Explain what elementLocator is for
    private Map<Class<? extends NoteElement>, List<Integer>> elementLocator;
    private Set<String> categories;    // User-defined categories
    private Set<CategoryTag> tags;     // Internally defined categories
    private static final Logger LOG = LogManager.getLogger(Note.class);

    // ToDo: Will this update the created date every time a note is retrieved from the DB?
    protected Note() {
        super(null);
    }

    // ToDo: Replace this with a NoteBuilder class?
    public Note(String title, String desc) {
        super(title, desc);
        elements = new ArrayList<>();
        // This contains a map from Note element class to element indexes in the elements list
        elementLocator = new HashMap<>();
        this.categories = new HashSet<>();
        this.tags = new HashSet<>();
    }

    public Note(String title) { this(title, null); }

    public ArrayList<NoteElement<?>> getElements() { return elements; }

    /**
     * Add new Note element at the end of the current note.
     * @param newElement The new Note element to be added
     */
    public void addElement(NoteElement<?> newElement) {
        addElement(newElement, elements.size());
    }

    /**
     * Add new Note element at the specified location.
     * @param newElement The new Note element to be added
     * @param index The location (0-based) of the new element
     */
    public void addElement(NoteElement<?> newElement, int index) {
        elements.add(index, newElement);
        updateElementLocatorAfterAdd(newElement, index);
    }

    // ToDo: Explain how this works?
    // I get weird generic errors if I attempt to pass the class here
    private void updateElementLocatorAfterAdd(NoteElement<?> element, int index) {
        if (!elementLocator.containsKey(element.getClass())) {
            elementLocator.put(element.getClass(), new ArrayList<>());
        }
        elementLocator.get(element.getClass()).add(index);
    }

    // Return element that was removed?
    public void removeElement(NoteElement<?> element) {
        elements.remove(element);   // ToDo: Check for result of remove()?
        updateElementLocatorAfterDelete(element);
    }

    // ToDo: Update the element locator logic?
    private void updateElementLocatorAfterDelete(NoteElement<?> element) {
        if (elementLocator.containsKey(element.getClass())) {
	    // ToDo: Why am I removing the whole look-up list?
            elementLocator.remove(element.getClass());
        }
        else {
            LOG.warn("Element locator for Note '" + getName() + "' contains no element of type "
                    + element.getClass().getSimpleName());
        }
    }

    // ToDo: Javadoc - this assumes there is only one element of the specified type? Or just returns the first one?
    public NoteElement<?> getElement(Class<? extends NoteElement<?>> elementClass) {
        return getElement(elementClass, 0);
    }

    /**
     * Return the requested element from the list of {@link Note} elements.
     * The index refers to the position of the {@link Note} element in a list of elements
     * of the same type. So, for example, if the {@link Note} contains two Contacts index
     * 0 would refer to the first one and index 1 would refer to the second (regardless
     * of their position in the overall list of {@link Note} elements).
     *
     * @param elementClass The class of the {@link Note} element to be retrieved
     * @param index The 0-based index of the {@link Note} element to be retrieved
     *              (for elements of the same class type)
     * @return The requested {@link Note} element (or an exception)
     */
    public NoteElement<?> getElement(Class<? extends NoteElement<?>> elementClass, int index) {
        if (!elementLocator.containsKey(elementClass)) {
            throw new RuntimeException("No " + elementClass.getSimpleName() + " element found in this note");
        }
        if (index >= elementLocator.get(elementClass).size()) {
            throw new RuntimeException("Invalid index (" + index + ") for list of " + elementClass.getSimpleName()
                    + " elements (size " + elementLocator.get(elementClass).size() + ")");
        }
        int elementIndex = elementLocator.get(elementClass).get(index);
        if (elementIndex >= elements.size()) {
            LOG.info(elementClass.getSimpleName() + " element index " + index
                    + " maps to main element index " + elementIndex);
            throw new RuntimeException("Invalid index (" + elementIndex + ") for note element list (size "
                    + elements.size() + ")");
        }
        // ToDo: Do a sanity check that the returned element is the right class?
        return elements.get(elementIndex);
    }

    protected NoteElement<?> getElement(int index) { return elements.get(index); }

    // ToDo: Add Javadoc
    public boolean hasElement(Class< NoteElement<?>> elementClass) {
        return elementLocator.containsKey(elementClass) && !elementLocator.get(elementClass).isEmpty();
    }

    // ToDo: Add Javadoc
    public Set<String> getCategories() { return categories; }
    public Note addCategory(String category) { categories.add(category); return this; }
    // ToDo: Add removeCategory()?
    public Note addCategories(Set<String> cats) { categories.addAll(cats); return this; }

    public Set<CategoryTag> getTags() { return tags; }
    public Note addTag(CategoryTag tag) { tags.add(tag); return this; }
    public Note addTags(Set<CategoryTag> tagList) { tags.addAll(tagList); return this; }
    // ToDo: Need Javadoc!
    public boolean containsAllTags(Set<CategoryTag> tagList) { return tags.containsAll(tagList); }
    public boolean containsAnyTags(Set<CategoryTag> tagList) {
        return tagList.stream().anyMatch(tag -> tags.contains(tag));
    }

    @Override
    public String toString() {
        String result = "Note:\n" + getName();
        if (getDescription() != null && !getDescription().isEmpty()) {
            result += "\n" + getDescription();
        }
        for (NoteElement<?> elem : elements) {
            // ToDo: Warn if element is null?
            result = result.concat("\n\n" + elem);
        }
        result += "\n\nCategories:";
        for (String cat : categories) {
            result = result.concat("\n" + cat);
        }
        result += "\n\nTags:";
        for (CategoryTag tag : tags) {
            result = result.concat("\n" + tag);
        }
        return result;
    }

    // For testing purposes
    protected void clear() {
        elements.clear();
        elementLocator.clear();
    }
}
