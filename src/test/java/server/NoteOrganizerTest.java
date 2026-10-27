package server;

import server.categories.Category;
import server.categories.MediaType;
import server.note.*;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.*;

import static server.NoteTestHelper.*;
import static server.note.Contact.*;
import static org.junit.jupiter.api.Assertions.*;

// ToDo: Figure out JUnit assertThat() with matchers (and replace assertTrue())
// ToDo: Switch to TestNG Test annotations and framework (instead of JUnit)?
//   If so use the description attribute to document the tests
// Todo: Change the name to make this a unit test?
public class NoteOrganizerTest extends TestBase {

    @Test
    public void addNewNote() {
        addTestNote(main);
        assertEquals(1, main.getNotes().size(), "Number of notes");
    }

    @Test
    public void addNewNoteAtASpecifiedLocation() {
        // ToDo: Need new NoteOrganizer API for this
    }

    @Test
    public void addMultipleNotes() {
        int expectedCount = 3;
        addTestNotes(expectedCount, main);
        assertEquals(expectedCount, main.getNotes().size(), "Number of notes");
    }

    @Test
    public void addScheduleItem() {
        main.addNote(new ScheduleItem("Event 1", null, "2020-07-01 09:00:00 PM"));
        assertEquals(1, main.getNotes().size(), "Total number of notes");
        assertEquals(1, main.getNotesForType(ScheduleItem.class).size(), "Number of ScheduleItem notes");
    }

    @Test
    public void scheduleItemsAreAddedInTimestampOrder() {
        createScheduleItemTestBed(main);
        assertTrue(verifyScheduleItemOrder(main.getSchedule()), "Schedule items are in timestamp order");
    }

    @Test
    public void mostRecentScheduleItemIsAddedAtFront() {
        createScheduleItemTestBed(main);
        ScheduleItem newItem = new ScheduleItem("Most Recent Event", null, LocalDateTime.MAX);
        main.addNote(newItem);
        assertTrue(verifyScheduleItemOrder(main.getSchedule()), "Schedule items are in timestamp order");
        assertSame(newItem, main.getSchedule().getFirst(), "Most recent schedule item is at the front");
    }

    @Test
    public void leastRecentScheduleItemIsAddedAtEnd() {
        createScheduleItemTestBed(main);
        ScheduleItem newItem = new ScheduleItem("Least Recent Event", null, LocalDateTime.MIN);
        main.addNote(newItem);
        assertTrue(verifyScheduleItemOrder(main.getSchedule()), "Schedule items are in timestamp order");
        assertSame(newItem, main.getSchedule().getLast(), "Least recent schedule item is at the end");
    }

    @Test
    public void callGetScheduleWithNoScheduleItemsAdded() {
        assertEquals(0, main.getSchedule().size(), "Number of schedule items");
    }

    @Test
    public void addProduct() {
        main.addNote(new Product("Blind Alley", null,
                new Entity("Fanny", "Rock Band",  Entity.EntityType.ARTIST)).setType(Category.of(MediaType.SONG)));
        assertEquals(1, main.getNotes().size(), "Total number of notes");
        assertEquals(1, main.getNotesForType(Product.class).size(), "Number of Product notes");
    }

    @Test
    public void addContact() {
        main.addNote(newPerson("Jane", "Doe"));
        assertEquals(1, main.getNotes().size(), "Total number of notes");
        assertEquals(1, main.getNotesForType(Contact.class).size(), "Number of Contact notes");
    }

    @Test
    // Remove the only note in the list
    public void removeOnlyNote() {
        NoteBase newNote = NoteTestHelper.createGenericTestNote();
        main.addNote(newNote);
        NoteBase deletedNote = main.deleteNote(newNote);
        // ToDo: MP suggests using notes.size() directly and losing main.numberOfNotes()
        assertEquals(0, main.getNotes().size(), "Number of notes");
        assertSame(newNote, deletedNote, "Note removed");
    }

    @Test
    // ToDo: Verify that the SAME notes are being deleted that were added
    //   What this is REALLY testing is removing the note at index 0
    public void removeMultipleNotes() {
        int expectedCount = 3;
        addTestNotes(expectedCount, main);
        for (int i = 0; i < expectedCount; i++) {
            NoteBase noteToRemove = main.getNote(0);
            // ToDo: Test the index version too: deleteNote(int index)?
            NoteBase removedNote = main.deleteNote(noteToRemove);
            assertEquals(expectedCount - i - 1, main.getNotes().size(), "Number of notes");
            assertSame(noteToRemove, removedNote, "Note removed");
        }
    }

    @Test
    public void removeAllNotes() {
        int initialNoteCount = 5;
        addTestNotes(initialNoteCount, main);
        main.getNotes().clear();
        assertEquals(0, main.getNotes().size(), "Number of remaining notes after deletion");
    }

    @Test
    public void removeOneOfMultipleNotes() {
        int initialNoteCount = 5;
        addTestNotes(initialNoteCount,  main);
        // ToDo: Verify that I deleted the RIGHT note
        main.deleteNote(0);
        assertEquals(initialNoteCount - 1, main.getNotes().size(), "Number of remaining notes after deletion");;
    }

    @Test
    public void removeNoteWithCategories() {
        Note newNote = NoteTestHelper.createGenericTestNote();
        main.addNote(newNote);
        String[] categories = new String[] {"category1", "category2", "category3"};
        main.addNoteToCategories(new HashSet<>(Arrays.asList(categories)), newNote);
        main.addCategory("category4");
        main.deleteNote(newNote);
        // Confirm that the note has been deleted from all categories
        for (String name : main.getCategories()) {
            assertFalse(main.getNotesForCategory(name).contains(newNote),
                    "Category '" + name + "' contains note '" + newNote.getName() + "'");
        }
    }

    @Test
    public void deleteNoteRemovesItFromNoteTypes() {
        Contact contact = newPerson("Jane", "Doe");
        main.addNote(contact);
        main.addNote(new Note("Plain note"));
        assertEquals(List.of(contact), main.noteTypes.get(Contact.class), "Contacts tracked by type");
        main.deleteNote(contact);
        assertTrue(main.noteTypes.get(Contact.class).isEmpty(), "Contact removed from type list");
        assertEquals(1, main.getNotes().size(), "Number of notes");
    }

    @Test
    // ToDo: Is this dependent on previous test results?
    public void tryRemovingInvalidNoteByIndex() {
        Exception ex = assertThrows(RuntimeException.class, () -> main.deleteNote(1));
        assertEquals("Invalid index (1) for note list (size 0)", ex.getMessage(), "Exception message");
    }

    @Test
    public void tryRemovingInvalidNoteByObject() {
        Note newNote = NoteTestHelper.createGenericTestNote();
        Exception ex = assertThrows(RuntimeException.class, () -> main.deleteNote(newNote));
        assertEquals("Failed to locate note: '" + newNote.getName() + "'", ex.getMessage(), "Exception message");
    }

    @Test
    public void addNewCategory() {
        main.addCategory(defaultCategoryName);
        assertEquals(1, numberOfCategories(main), "Number of categories");
    }

    @Test
    public void addCategoryWithSpaceInName() {
        String categoryName = "Category name with spaces";
        main.addCategory(categoryName);
        Set<NoteBase> category = main.getNotesForCategory(categoryName);
        assertEquals(0, category.size(), "Number of notes in category");
    }

    @Test
    // This is also removing the *only* category?
    public void removeLastCategory() {
        main.addCategory(defaultCategoryName);
        main.deleteCategory(defaultCategoryName);
        assertEquals(0, main.getCategories().size(), "Number of categories");
    }

    // ToDo: addMultipleCategories()
    // ToDo: removeOneOfMultipleCategories()

    static final String invalidCategory = "invalidCategory";
    @Test
    public void tryGettingInvalidCategory() {
        Set<NoteBase> notes = main.getNotesForCategory(invalidCategory);
        assertEquals(0, notes.size(), "Number of notes in category " + invalidCategory);
     }

    @Test
    public void tryRemovingInvalidCategory() {
        Exception ex = assertThrows(RuntimeException.class, () -> main.deleteCategory(invalidCategory));
        String expectedMessage = "Unable to locate category '" + invalidCategory + "'";
        assertTrue(ex.getMessage().contains(expectedMessage),
                "Exception message contains '" + expectedMessage + "'");
    }

    @Test
    public void addNoteToCategory() {
        main.addNoteToCategory(defaultCategoryName, NoteTestHelper.createGenericTestNote());
        Set<NoteBase> category = main.getNotesForCategory(defaultCategoryName);
        assertEquals(1, category.size(),
                "Number of notes in category '" + defaultCategoryName + "'");
    }

    @Test
    public void removeNoteFromCategory() {
        Note newNote = NoteTestHelper.createGenericTestNote();
        main.addNoteToCategory(defaultCategoryName, newNote);
        main.removeNoteFromCategory(defaultCategoryName, newNote);
        Set<NoteBase> category = main.getNotesForCategory(defaultCategoryName);
        assertFalse(newNote.hasCategory(defaultCategoryName), "Note '" + newNote.getName() +
                "' has category '" + defaultCategoryName + "'");
        assertEquals(0, category.size(),
                "Number of notes in category '" + defaultCategoryName + "'");
    }

    @Test
    public void addMultipleNotesToCategory() {
        String categoryName = "testCategory";
        int expectedCount = 3;
        List<NoteBase> noteList = addTestNotes(expectedCount,  main);
        main.addNotesToCategory(categoryName, noteList);
        assertEquals(expectedCount, main.getNotesForCategory(categoryName).size(),
                "Number of notes in '" + categoryName + "'");
        for (NoteBase n : noteList) {
            assertTrue(main.getNotesForCategory(categoryName).contains(n),
                    "Category " + categoryName + " contains note '" + n.getName() + "'");
        }
    }

    @Test
    public void removeMultipleNotesFromCategory() {
        int expectedCount = 3;
        List<NoteBase> noteList = addTestNotes(expectedCount, main);
        main.addNotesToCategory(defaultCategoryName, noteList);
        for (int i = 0; i < expectedCount; i++) {
            main.removeNoteFromCategory(defaultCategoryName, noteList.get(i));
            assertEquals(expectedCount - i - 1, main.getNotesForCategory(defaultCategoryName).size(),
                    "Number of notes in '" + defaultCategoryName + "'");
        }
    }

    // removeOneOfMultipleNotesFromCategory()

    @Test
    public void addNoteToMultipleCategories() {
        Note newNote = NoteTestHelper.createGenericTestNote();
        main.addNote(newNote);
        HashSet<String> categories = new HashSet<>(Arrays.asList("category1", "category2", "category3"));
        main.addNoteToCategories(categories, newNote);
        for (String category : categories) {
            assertTrue(main.getNotesForCategory(category).contains(newNote), "Category " + category + " contains note '" + newNote.getName() + "'");
        }
    }

    @Test
    public void removeNoteFromMultipleCategories() {
        Note newNote = NoteTestHelper.createGenericTestNote();
        main.addNote(newNote);
        HashSet<String> categories = new HashSet<>(Arrays.asList("category1", "category2", "category3"));
        main.addNoteToCategories(categories, newNote);
        for (String category : categories) {
            main.removeNoteFromCategory(category, newNote);
            assertFalse(main.getNotesForCategory(category).contains(newNote),
                    "Category " + category + " contains note '" + newNote.getName() + "'");
        }
    }

    @Test
    public void tryRemovingNoteFromInvalidCategory() {
        Note newNote = NoteTestHelper.createGenericTestNote();
        Exception ex = assertThrows(RuntimeException.class,
                () -> main.removeNoteFromCategory(invalidCategory, newNote));
        assertEquals("Unable to locate category '" + invalidCategory + "'", ex.getMessage(),
                "Exception message");
    }

    @Test
    public void tryRemovingInvalidNoteFromCategory() {
        main.addCategory(defaultCategoryName);
        NoteBase newNote = NoteTestHelper.createGenericTestNote();
        Exception ex = assertThrows(RuntimeException.class,
                () -> main.removeNoteFromCategory(defaultCategoryName, newNote));
        assertEquals("Unable to locate note '" + newNote.getName() + "' in category '" + defaultCategoryName + "'",
                ex.getMessage(), "Exception message");
    }

    // ToDo: Potential tests to add:
    //  - Added tests for setting the completed property in ScheduleItem
    //  - Add tests for the tracker properties in noteTypes (e.g. ScheduleItem, Contact)?
    //  - Test internal Note categories (set up in initialize())
    //  - Remove note by object and index
    //  - Test adding duplicate Categories (should have no effect?)
    //  - Confirm that new notes and categories (and notes in categories) are added at the end

    // @Test
    public void testTemplate() {
        throw new RuntimeException("Test not yet implemented");
    }
}
