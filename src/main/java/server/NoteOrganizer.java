package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.categories.*;
import server.note.*;

import java.util.*;

// ToDo: Track contacts by type (e.g. people and businesses)?
// ToDo: Can just load all Notes from the DB each time (because presumably it's a small app)?
//  Have a mechanism to complete/deactivate notes and don't load them by default
// ToDo: Add Javadoc (this is the main program / entry point)!
//   E.g. user-defined versus internal categories
// ToDo: Add DEBUG logging (for exception context)
// ToDo: Can (or should) this class be a static singleton (how would that work with Hibernate)?
// ToDo: Add searchForContacts() API (can just search with noteType Contact)?
public class NoteOrganizer {
    // Note: using List because Set isn't ordered
    protected List<NoteBase> notes;   // This structure defines the note tree
    protected Map<Class<? extends NoteBase>, List<NoteBase>> noteTypes;
    // ToDo: Change this to Category type at some point (equals() and hashCode() are based directly on name)
    protected Map<String, Set<NoteBase>> categories;
    // ToDo: Create one universal logger for the whole app?
    private static final Logger LOG = LogManager.getLogger(NoteOrganizer.class);

    public NoteOrganizer() {
        notes = new ArrayList<>();
        noteTypes = new HashMap<>();
        categories = new HashMap<>();
    }

    public NoteBase getNote(int index) {
        checkForValidNoteIndex(index);
        return notes.get(index);
    }

    public List<NoteBase> getNotes() { return notes; }
    public Set<String> getCategories() { return categories.keySet(); }
    public Set<NoteBase> getNotesForCategory(String name) {
        // ToDo: Return copies instead of the actual internal category list?
        return categories.get(name) != null ? categories.get(name) : new HashSet<>();
    }
    public List<NoteBase> getNotesForType(Class<? extends NoteBase> type) {
        return noteTypes.get(type) != null ? noteTypes.get(type) : new ArrayList<>();
    }

    public void addNote(NoteBase newNote) {
        notes.add(newNote);
        updateTrackerProperties(newNote);
    }

    public void addChildNote(Note note, NoteBase newNote) {
        note.addChildNote(newNote);
        updateTrackerProperties(newNote);
    }

    private void updateTrackerProperties(NoteBase newNote) {
        updateNoteTypes(newNote);
        addNoteToCategories(newNote);
    }

    public void addScheduleItem(ScheduleItem item) {
        int index = 0;
        List<ScheduleItem> schedule = noteTypes.get(ScheduleItem.class).stream().map(n -> (ScheduleItem)n).toList();
        while (index < schedule.size() && schedule.get(index).getDate().isAfter(item.getDate())) { index++; }
        noteTypes.get(ScheduleItem.class).add(index, item);
    }

    // ToDo: This is kind of complicated: Need to sort contacts by last name but organizations by name
    public void addContact(Contact contact) {
        // ToDo: Order noteTypes.get(Contact.class) alphabetically (just adding Contact for now)
        noteTypes.get(Contact.class).add(contact);
    }

    public void addNotes(List<NoteBase> newNotes) {
        newNotes.forEach(this::addNote);
    }

    // ToDo: If I implement this I also need to update categories
    public void addNote(NoteBase newNote, int index) { }

    // ToDo: Throw error for duplicate category (even though they're stored in a Set)?
    public void addCategory(String name) { categories.put(name, new HashSet<>()); }

    // ToDo: Can some of this infrastructure for managing categories be made private?
    public void addNoteToCategory(String name, NoteBase newNote) {
        newNote.addCategory(name);
        if (!categories.containsKey(name)) {
            addCategory(name);
        }
        categories.get(name).add(newNote);
    }

    private void addNoteToCategories(NoteBase newNote) {
        newNote.getCategories().forEach(cat -> addNoteToCategory(cat, newNote));
    }

    public void addNoteToCategories(Set<String> categories, NoteBase newNote) {
        categories.forEach(cat -> addNoteToCategory(cat, newNote));
    }

    public void addNotesToCategory(String name, List<NoteBase> notes) {
        notes.forEach(nb -> addNoteToCategory(name, nb));
    }

    private void updateNoteTypes(NoteBase newNote) {
        if (!noteTypes.containsKey(newNote.getClass())) {
            noteTypes.put(newNote.getClass(), new ArrayList<>());
        }
        if (newNote instanceof ScheduleItem) {
            addScheduleItem((ScheduleItem)newNote);
        }
        else if (newNote instanceof Contact) {
            addContact((Contact)newNote);
        }
        else { noteTypes.get(newNote.getClass()).add(newNote); }
    }

    public NoteBase deleteNote(int index) {
        checkForValidNoteIndex(index);
        return deleteNote(notes.get(index));
    }

    // ToDo: Will this work for notes that have been retrieved from the DB?
    public NoteBase deleteNote(NoteBase note) {
        checkForValidNoteObject(note);
        noteTypes.get(note.getClass()).remove(note);
        removeNoteFromAllCategories(note);
        notes.remove(note);
        return note;
    }

    // Used when deleting a note
    private void removeNoteFromAllCategories(NoteBase note) {
        // We make a copy of note categories to avoid ConcurrentModificationException
        new HashSet<>(note.getCategories()).forEach(cat -> removeNoteFromCategory(cat, note));
    }

    public void deleteCategory(String name) {
        checkForValidCategory(name);
        // ToDo: Only allow this if category is empty (doesn't contain any notes)?
        if (!categories.get(name).isEmpty()) {
            LOG.warn("Deleting category '{}' will remove it from all notes", name);
        }
        categories.get(name).forEach(n -> n.getCategories().remove(name));
        categories.remove(name);
    }

    public List<ScheduleItem> getSchedule() {
        List<NoteBase> schedule = noteTypes.get(ScheduleItem.class);
        return schedule != null ? schedule.stream().map(n -> (ScheduleItem)n).toList() : new ArrayList<>();
    }

    // ToDo: Add version that takes a note index? Also return the note?
    public void removeNoteFromCategory(String name, NoteBase note) {
        checkForValidCategory(name);
        if (!categories.get(name).contains(note)) {
            throw new RuntimeException("Unable to locate note '" + note.getName() + "' in category '" + name + "'");
        }
        System.out.println("Removing note '" + note.getName() + "' from category '" + name + "'");
        categories.get(name).remove(note);
        note.removeCategory(name);
    }

    public List<NoteBase> findMatchingNotes(List<NoteBase> list, SearchCriteria criteria) {
        List<NoteBase> matchingNotes = new ArrayList<>();
        for (NoteBase note : list) {
            if (criteria.match(note)) {
                matchingNotes.add(note);
            }
            // Search child notes, if any
            if (note instanceof Note && ((Note)note).hasChildNotes()) {
                matchingNotes.addAll(findMatchingNotes(((Note)note).getChildNotes(), criteria));
            }
        }
        return matchingNotes;
    }

    // Search from the main note list
    // ToDo: Should this return Set or List (if a note is in the list more than once should we return it multiple times)?
    public List<NoteBase> findMatchingNotes(SearchCriteria criteria) {
        List<NoteBase> searchNotes = criteria.hasCategories() ? criteria.getCategories().stream()
                .flatMap(cat -> getNotesForCategory(cat).stream()).distinct().toList() : notes;
        // ToDo: Also filter on class type?
        return findMatchingNotes(searchNotes, criteria);
    }

    public void initialize() {
        // ToDo: Set up standard Note categories (e.g. scheduled, purchase, etc.)
    }

    private void checkForValidNoteIndex(int index) {
        if (index >= notes.size()) {
            throw new RuntimeException("Invalid index (" + index + ") for note list (size " + notes.size() + ")");
        }
    }

    private void checkForValidNoteObject(NoteBase note) {
        if (!notes.contains(note)) {
            throw new RuntimeException("Failed to locate note: '" + note.getName() + "'");
        }
    }

    // ToDo: Can I remove this (if the category doesn't exist we just create it)?
    //  But may still be useful when deleting categories
    private void checkForValidCategory(String name) {
        if (!categories.containsKey(name)) {
            throw new RuntimeException("Unable to locate category '" + name + "'");
        }
    }
}
