package server;

import org.junit.jupiter.api.*;
import server.categories.CategoryTag;
import server.categories.MediaType;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

// ToDo: Figure out JUnit assertThat() with matchers (and replace assertTrue())
// ToDo: Switch to TestNG Test annotations (instead of JUnit)?
//   If so use the description attribute to document the tests
// ToDo: Is it an issue that tags and categories are handled separately but are conceptually the same?
// ToDo: Per MP should categories be statically defined and Tags be user defined (i.e. switch them)?
// ToDo: Get the Gradle stuff figured out (why the test config claims to be effed up)
// Confirm that new notes and categories (and notes in categories) are added at the end
// Add multiple notes to a category at a specified position
// Test standard Note categories (set up in initialize())
// Test adding duplicate Category or Tags
// ToDo: MP objects to testing the TestOrganizer instead of the NoteOrganizer
//  Suggests creating a new NoteOrganizer before each test (removing the need for clear() methods)
public class NoteOrganizerTest {
    private static final TestOrganizer main = new TestOrganizer();
    private static final Map<CategoryTag, Integer> tagTracker = new HashMap<>();
    private static final String defaultCategoryName = "NewCategory";

    @BeforeAll
    public static void setUpTests() { }    // Currently doesn't seem to be needed
    // ToDo: Put in @BeforeEach?
    // Note newNote = NoteTestHelper.createGenericTestNote();

    @AfterEach
    public void testCleanup() {
        main.clearNotes();
        main.clearCategories();
        tagTracker.clear();
        NoteTestHelper.resetNoteNumber();
    }

    @Test
    public void addNewNote() {
        main.addTestNote();
        assertEquals(1, main.numberOfNotes(), "Number of notes");
    }

    @Test
    public void addNewNoteAtASpecifiedLocation() {
        // ToDo: Need new API for this
    }

    @Test
    // Remove the only note in the list
    public void removeOnlyNote() {
        Note newNote = NoteTestHelper.createGenericTestNote();
        main.addNote(newNote);
        Note deletedNote = main.deleteNote(newNote);
        // ToDo: MP suggests using notes.size() directly and losing main.numberOfNotes()
        assertEquals(0, main.numberOfNotes(), "Number of notes");
        assertSame(newNote, deletedNote, "Note removed");
    }

    @Test
    public void addMultipleNotes() {
        int expectedCount = 3;
        main.addTestNotes(expectedCount);
        assertEquals(expectedCount, main.numberOfNotes(), "Number of notes");
    }

    @Test
    // ToDo: Verify that the SAME notes are being deleted that were added
    //   What this is REALLY testing is removing the note at index 0
    public void removeMultipleNotes() {
        int expectedCount = 3;
        main.addTestNotes(expectedCount);
        for (int i = 0; i < expectedCount; i++) {
            Note noteToRemove = main.getNote(0);
            // ToDo: Test the index version too: deleteNote(int index)?
            Note removedNote = main.deleteNote(noteToRemove);
            assertEquals(expectedCount - i - 1, main.numberOfNotes(), "Number of notes");
            assertSame(noteToRemove, removedNote, "Note removed");
        }
    }


    @Test
    public void removeAllNotes() {
        int initialNoteCount = 5;
        main.addTestNotes(initialNoteCount);
        main.getNotes().clear();
        assertEquals(0, main.numberOfNotes(), "Number of remaining notes after deletion");
    }

    @Test
    public void removeOneOfMultipleNotes() {
        int initialNoteCount = 5;
        main.addTestNotes(initialNoteCount);
        // ToDo: Verify that I deleted the RIGHT note
        main.deleteNote(0);
        assertEquals(initialNoteCount - 1, main.numberOfNotes(), "Number of remaining notes after deletion");
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
            assertFalse(main.getCategory(name).contains(newNote),
                    "Category '" + name + "' contains note '" + newNote.getName() + "'");
        }
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
        assertEquals(1, main.numberOfCategories(), "Number of categories");
    }

    @Test
    public void addCategoryWithSpaceInName() {
        String categoryName = "Category name with spaces";
        main.addCategory(categoryName);
        List<Note> category = main.getCategory(categoryName);
        assertEquals(0, category.size(), "Number of notes in category");
    }

    @Test
    // This is also removing the *only* category?
    public void removeLastCategory() {
        main.addCategory(defaultCategoryName);
        main.deleteCategory(defaultCategoryName);
        assertEquals(0, main.numberOfCategories(), "Number of categories");
    }

    // ToDo: addMultipleCategories()
    // ToDo: removeOneOfMultipleCategories()

    static final String invalidCategory = "invalidCategory";
    @Test
    public void tryGettingInvalidCategory() {
        Exception ex = assertThrows(RuntimeException.class, () -> main.getCategory(invalidCategory));
        assertEquals("Unable to locate category '" + invalidCategory + "'", ex.getMessage(),
                "Exception message");
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
        List<Note> category = main.getCategory(defaultCategoryName);
        assertEquals(1, category.size(),
                "Number of notes in category '" + defaultCategoryName + "'");
    }

    @Test
    public void removeNoteFromCategory() {
        Note newNote = NoteTestHelper.createGenericTestNote();
        main.addNoteToCategory(defaultCategoryName, newNote);
        main.removeNoteFromCategory(defaultCategoryName, newNote);
        List<Note> category = main.getCategory(defaultCategoryName);
        assertEquals(0, category.size(),
                "Number of notes in category '" + defaultCategoryName + "'");
    }

    @Test
    public void addMultipleNotesToCategory() {
        String categoryName = "testCategory";
        int expectedCount = 3;
        List<Note> noteList = main.addTestNotes(expectedCount);
        main.addNotesToCategory(categoryName, noteList);
        assertEquals(expectedCount, main.getCategory(categoryName).size(),
                "Number of notes in '" + categoryName + "'");
        for (Note n : noteList) {
            assertTrue(main.getCategory(categoryName).contains(n),
                    "Category " + categoryName + " contains note '" + n.getName() + "'");
        }
    }

    @Test
    public void removeMultipleNotesFromCategory() {
        int expectedCount = 3;
        List<Note> noteList = main.addTestNotes(expectedCount);
        main.addNotesToCategory(defaultCategoryName, noteList);
        for (int i = 0; i < expectedCount; i++) {
            main.removeNoteFromCategory(defaultCategoryName, noteList.get(i));
            assertEquals(expectedCount - i - 1, main.getCategory(defaultCategoryName).size(),
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
            assertTrue(main.getCategory(category).contains(newNote), "Category " + category + " contains note '" + newNote.getName() + "'");
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
            assertFalse(main.getCategory(category).contains(newNote),
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
        Note newNote = NoteTestHelper.createGenericTestNote();
        Exception ex = assertThrows(RuntimeException.class,
                () -> main.removeNoteFromCategory(defaultCategoryName, newNote));
        assertEquals("Unable to locate note '" + newNote.getName() + "' in category '" + defaultCategoryName + "'",
                ex.getMessage(), "Exception message");
    }

    @Test
    public void getSingleNoteWithTag() {
        Note newNote = NoteTestHelper.createGenericTestNote();
        CategoryTag mediaType = MediaType.SONG;
        newNote.addTag(mediaType);
        main.addNote(newNote);
        List<Note> taggedNotes = main.getNotesWithTag(mediaType);
        assertEquals(1, taggedNotes.size(), "Number of notes with media type " + mediaType);
        assertSame(newNote, taggedNotes.get(0), "Note with media type " + mediaType);
    }

    @Test
    public void getMultipleNotesWithSameTag() {
        CategoryTag mediaType = MediaType.SONG;
        int numberOfNotes = 3;
        createNotesWithTags(numberOfNotes, mediaType, main.getNotes());
        List<Note> taggedNotes = main.getNotesWithTag(mediaType);
        assertEquals(numberOfNotes, taggedNotes.size(), "Number of notes with media type " + mediaType);
    }

    @Test
    public void getMultipleNotesWithVariousTags() {
        int numberOfNotes = 10;
        int total = 0;
        createNotesWithTags(numberOfNotes);
        for (CategoryTag tag : tagTracker.keySet()) {
            List<Note> taggedNotes = main.getNotesWithTag(tag);
            assertEquals(tagTracker.get(tag).intValue(), taggedNotes.size(),
                    "Number of notes with media type " + tag);
            total += taggedNotes.size();
        }
        assertEquals(numberOfNotes, total, "Total number of tagged notes");
    }

    @Test
    public void getTaggedNotesFromListContainingTaggedAndUntaggedNotes() {
        int numberOfTaggedNotes = 10;
        int numberOfUntaggedNotes = 5;
        int total = 0;
        createNotesWithTags(numberOfTaggedNotes);
        // This is identical to the above test except for this line adding untagged notes
        main.addTestNotes(numberOfUntaggedNotes);
        assertEquals(numberOfTaggedNotes + numberOfUntaggedNotes, main.numberOfNotes(), "Total number of notes");
        for (CategoryTag tag : tagTracker.keySet()) {
            List<Note> taggedNotes = main.getNotesWithTag(tag);
            assertEquals(tagTracker.get(tag).intValue(), taggedNotes.size(),
                    "Number of notes with media type " + tag);
            total += taggedNotes.size();
        }
        assertEquals(numberOfTaggedNotes, total, "Total number of tagged notes");
    }

    @Test
    public void tryGettingTaggedNotesFromListContainingOnlyUntaggedNotes() {
        main.addTestNotes(10);    // These are untagged notes
        assertEquals(tagTracker.size(), 0, "Number of test tags tracked");
        for (CategoryTag tag : MediaType.get().getTagValues()) {
            List<Note> taggedNotes = main.getNotesWithTag(tag);
            assertEquals(0, taggedNotes.size(), "Number of notes with media type " + tag);
        }
    }

    @Test
    public void tryGettingTaggedNotesFromEmptyList() {
        // The list of test notes is empty by default
        assertEquals(tagTracker.size(), 0, "Number of test tags tracked");
        for (CategoryTag tag : MediaType.get().getTagValues()) {
            List<Note> taggedNotes = main.getNotesWithTag(tag);
            assertEquals(taggedNotes.size(), 0, "Number of notes with media type " + tag);
        }
    }

    // ToDo: Add test for trying to get tagged note from list that doesn't contain the tag

    @Test
    public void getTaggedNotesWithOrRelationship() {
        createNotesWithMultipleTags();
        List<Note> orResult = main.getNotesWithAnyTags(tagTracker.keySet());
        assertEquals(partitionCount*3, orResult.size(), "Number of notes with any queried tags");
    }

    @Test
    public void getTaggedNotesWithAndRelationship() {
        createNotesWithMultipleTags();
        List<Note> andResult = main.getNotesWithAllTags(tagTracker.keySet());
        assertEquals(partitionCount, andResult.size(), "Number of notes with all queried tags");
    }

    // Re-run (at least some) tagging tests with category lists instead of main

    // @Test
    public void testTemplate() {
        throw new RuntimeException("Test not yet implemented");
    }

    // ====================================== Helper Methods ======================================
    // ToDo: Move these to NoteTestHelper
    // ToDo: Change this to a builder?
    public void createNotesWithTags(int numberOfNotes) { createNotesWithTags(numberOfNotes, null, main.getNotes()); }
    public void createNotesWithTags(int numberOfNotes, CategoryTag tag) {
        createNotesWithTags(numberOfNotes, tag, main.getNotes());
    }
    // ToDo: Add Category parameter to pass in and test other lists
    public void createNotesWithTags(int numberOfNotes, CategoryTag tag, List<Note> noteList) {
        for (int i = 0; i < numberOfNotes; i++) {
            createNoteWithTag(tag, noteList);
        }
    }
    public void createNoteWithTag(CategoryTag tag) { createNoteWithTag(tag, main.getNotes()); }
    public void createNoteWithTag(CategoryTag tag, List<Note> noteList) {
        CategoryTag newTag = tag == null ? MediaType.get().getRandomTag() : tag;
        Note newNote = NoteTestHelper.createGenericTestNote().addTag(newTag);
        updateTagTracker(newTag);
        System.out.println(newNote);
        noteList.add(newNote);
    }

    // ToDo: Could pass in the list of notes to this method as well
    // ToDo: Create addNote() API that automatically updates tagTracker
    // Creates notes for testing AND and OR tag queries
    static final int partitionCount = 4;
    public void createNotesWithMultipleTags() {
        main.addTestNotes(partitionCount*3);
        for (int i = 0; i < partitionCount*3; i++) {
            if (i < partitionCount*2) {
                main.getNote(i).addTag(MediaType.BOOK);
                updateTagTracker(MediaType.BOOK);
            }
            if (i >= partitionCount) {
                main.getNote(i).addTag(MediaType.FILM);
                updateTagTracker(MediaType.FILM);
            }
        }
    }

    private void updateTagTracker(CategoryTag newTag) {
        if (!tagTracker.containsKey(newTag)) {
            tagTracker.put(newTag, 0);
        }
        tagTracker.put(newTag, tagTracker.get(newTag) + 1);
    }
}
