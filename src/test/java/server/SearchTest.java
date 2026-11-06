package server;

import server.categories.Category;
import server.note.*;
import server.categories.MediaType;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static server.note.Contact.*;
import static server.NoteTestHelper.*;
import static server.database.HibernateUtil.*;
import static org.junit.jupiter.api.Assertions.*;

// ToDo: Test scenarios to cover:
//  - Note with 2 sub-note levels
//  - Note with many sub-notes
//  - Notes with and without categories
//  - Sub-notes with and without categories
//  - Notes and sub-notes with different class types
public class SearchTest extends TestBase {
    // ToDo: Set up data providers for search tests after moving to TestNG
    @Test
    public void commitSingleNoteWithCategory() {
        Note newNote = NoteTestHelper.createGenericTestNote();
        Category cat = Category.of(MediaType.SONG);
        commitNoteWithCategories(newNote, Set.of(cat));
        SearchCriteria criteria = new SearchCriteria().setCategory(cat);
        // ToDo: Change this to HibernateUtil.runTransaction()?
        runTestTransaction(session -> {
            Note fetchedNote = retrieveNoteFromDB(session, newNote);
            List<? extends NoteBase> notes = findMatchingNotesInDB(session, criteria);
            assertEquals(1, notes.size(), "Number of notes with media type " + cat);
            assertEquals(cat, fetchedNote.getCategories().iterator().next(), "Category for fetched Note");
            assertTrue(notes.contains(fetchedNote), "Note list contains new note with media type " + cat);
        });
    }

    @Test
    public void getMultipleNotesWithSameCategory() {
        Category cat = Category.of(MediaType.SONG);
        int numberOfNotes = 3;
        commitNewNotesWithCategories(numberOfNotes, Set.of(cat));
        runTestTransaction(session -> {
            SearchCriteria criteria = new SearchCriteria().setCategory(cat);
            List<? extends NoteBase> notes = findMatchingNotesInDB(session, criteria);
            assertEquals(numberOfNotes, notes.size(), "Number of notes with media type " + cat);
        });
    }

    @Test
    public void getMultipleNotesWithVariousCategories() {
        int numberOfNotes = 10;
        commitNewNotesWithCategories(numberOfNotes, null);   // This creates notes with random categories
        runTestTransaction(session -> {
            int total = 0;
            for (Category cat : retrieveCategoriesFromDB(session)) {
                SearchCriteria criteria = new SearchCriteria().setCategory(cat);
                List<? extends NoteBase> notes = findMatchingNotesInDB(session, criteria);
                assertEquals(retrieveNotesForCategory(session, cat).size(), notes.size(),
                        "Number of notes with media type " + cat);
                total += notes.size();
            }
            assertEquals(numberOfNotes, total, "Total number of categorized notes");
        });
    }

    @Test
    public void getCategorizedNotesFromListContainingCategorizedAndUncategorizedNotes() {
        int numberOfCategorizedNotes = 10;
        int numberOfUncategorizedNotes = 5;
        commitNewNotesWithCategories(numberOfCategorizedNotes, null);    // Create notes with random categories
        System.out.println("Number of Notes after first add: " + retrieveNotes(testSession).size());
        // This is identical to the above test except for this line adding uncategorized notes
        commitNewNotesWithCategories(numberOfUncategorizedNotes, Set.of());        // Create uncategorized notes
        System.out.println("Number of Notes after second add: " + retrieveNotes(testSession).size());
        runTestTransaction(session -> {
            assertEquals(numberOfCategorizedNotes + numberOfUncategorizedNotes, retrieveNotes(session).size(),
                    "Total number of notes");
            int total = 0;
            for (Category cat : retrieveCategoriesFromDB(session)) {
                SearchCriteria criteria = new SearchCriteria().setCategory(cat);
                List<? extends NoteBase> notes = findMatchingNotesInDB(session, criteria);
                assertEquals(notes.size(), retrieveNotesForCategory(session, cat).size(),
                        "Number of notes with media type " + cat);
                total += notes.size();
            }
            assertEquals(numberOfCategorizedNotes, total, "Total number of tagged notes");
        });
    }

    @Test
    public void tryGettingCategorizedNotesFromDBWithOnlyUncategorizedNotes() {
        commitNewNotesWithCategories(10, Set.of());   // Create uncategorized notes
        runTestTransaction(session -> {
            assertEquals(0, retrieveCategoriesFromDB(session).size(), "Number of test categories tracked");
            for (String cat : MediaType.get().convertToStrings()) {
                SearchCriteria criteria = new SearchCriteria().setCategory(cat);
                List<? extends NoteBase> notes = findMatchingNotesInDB(session, criteria);
                assertEquals(0, notes.size(), "Number of notes with media type " + cat);
            }
        });
    }

    // ToDo: Not sure how useful this test is
    @Test
    public void tryGettingCategorizedNotesFromEmptyDB() {
        runTestTransaction(session -> {
            // The DB is empty by default
            assertEquals(0, retrieveCategoriesFromDB(session).size(), "Number of test categories tracked");
            for (String cat : MediaType.get().convertToStrings()) {
                SearchCriteria criteria = new SearchCriteria().setCategory(cat);
                List<? extends NoteBase> notes = findMatchingNotesInDB(session, criteria);
                assertEquals(0, notes.size(), "Number of notes with media type " + cat);
            }
        });
    }

    // ToDo: Add test for trying to get tagged note from list that doesn't contain the tag

    @Test
    public void getCategorizedNotesWithOrRelationship() {
        Set<Category> categories = commitNotesWithMultipleCategories();
        runTestTransaction(session -> {
            // SearchCriteria does an "OR" match by default
            SearchCriteria criteria = new SearchCriteria().setCategories(categories).setNoteClass(Note.class);
            List<? extends NoteBase> orResult = findMatchingNotesInDB(session, criteria);
            // System.out.println("Categories: " + categories.stream().map(Category::getName).collect(Collectors.toSet()));
            assertEquals(partitionCount*3, orResult.size(), "Number of notes with any queried categories");
        });
    }

    @Test
    public void getCategorizedNotesWithAndRelationship() {
        Set<Category> categories = commitNotesWithMultipleCategories();
        runTestTransaction(session -> {
            SearchCriteria criteria = new SearchCriteria().setCategories(categories).joinWithAnd();
            List<? extends NoteBase> andResult = findMatchingNotesInDB(session, criteria);
            assertEquals(partitionCount, andResult.size(), "Number of notes with all queried categories");
        });
    }

    @Test
    public void getNotesByClass() {
        persistNewTestNotes(3, main);
        Contact contact = newPerson("Jane", "Doe");
        main.addNote(contact);
        SearchCriteria criteria = new SearchCriteria().setNoteClass(Contact.class);
        List<NoteBase> result = main.findMatchingNotes(criteria);
        assertEquals(List.of(contact), result, "Notes matching class Contact");
    }

    @Test
    public void getNotesByPartialName() {
        main.addNote(new Note("Grocery list"));
        main.addNote(new Note("Hardware list"));
        main.addNote(new Note("Birthday ideas"));
        SearchCriteria criteria = new SearchCriteria().setNameMatch("LIST");
        List<NoteBase> result = main.findMatchingNotes(criteria);
        assertEquals(2, result.size(), "Notes with name containing 'list' (case-insensitive)");
    }

    @Test
    public void getNotesByPatternMatch() {
        String pattern = ".* LIST$";
        main.addNote(new Note("Grocery list"));
        main.addNote(new Note("Hardware list"));
        main.addNote(new Note("Birthday ideas"));
        SearchCriteria criteria = new SearchCriteria().setNameMatch(pattern).doPatternMatch();
        List<NoteBase> result = main.findMatchingNotes(criteria);
        assertEquals(2, result.size(), "Notes with name matching pattern '" +  pattern + "'");
    }

    @Test
    public void getMatchingChildNoteOfNonMatchingParent() {
        String mediaType = MediaType.SONG.toString();
        Note parent = new Note("Parent");
        Note child = new Note("Child");
        child.addCategory(mediaType);
        main.addNote(parent);
        main.addChildNote(parent, child);
        SearchCriteria criteria = new SearchCriteria().setCategory(mediaType);
        assertEquals(List.of(child), main.findMatchingNotes(criteria), "Child notes searched");
    }

    @Test
    public void getNotesByNameAndClass() {
        createScheduleItemTestBed(main);
        String name = "Test Name 1";
        main.getNote(0).setName(name);
        SearchCriteria criteria = new SearchCriteria().setNoteClass(ScheduleItem.class).setNameMatch(name);
        List<NoteBase> andResult = main.findMatchingNotes(criteria);
        assertEquals(1, andResult.size(),
                "Number of ScheduleItem notes with name '" + name + "'");
    }

    @Test
    public void getNotesByNamePatternAndClass() {
        createScheduleItemTestBed(main);
        assertEquals(5, main.getNotesForType(ScheduleItem.class).size(), "Number of ScheduleItem notes");
        String pattern = "Event [1-3]";
        SearchCriteria criteria = new SearchCriteria().setNoteClass(ScheduleItem.class).setNameMatch(pattern).doPatternMatch();
        List<NoteBase> andResult = main.findMatchingNotes(criteria);
        assertEquals(3, andResult.size(), "Number of ScheduleItem notes with name matching pattern '" + pattern + "'");
    }

    @Test
    public void getNotesByNameAndCategory() {
        Set<Category> categories = createNotesWithMultipleCategories(main);
        String name = "Test Note";   // This is the default name for test notes
        SearchCriteria criteria = new SearchCriteria().setCategories(categories).joinWithAnd().setNameMatch(name);
        List<NoteBase> andResult = main.findMatchingNotes(criteria);
        assertEquals(partitionCount, andResult.size(), "Number of notes with categories and name '" + name + "'");
    }

    @Test
    public void getNotesByNamePatternAndCategory() {
        String pattern = "T.* N.*";
        Set<Category> categories = createNotesWithMultipleCategories(main);
        SearchCriteria criteria = new SearchCriteria().setCategories(categories).joinWithAnd()
                .setNameMatch(pattern).doPatternMatch();
        List<NoteBase> andResult = main.findMatchingNotes(criteria);
        assertEquals(partitionCount, andResult.size(), "Number of notes with categories and name matching pattern '"
                + pattern + "'");
    }

    @Test
    public void getNotesByNameClassAndCategory() {
        createScheduleItemTestBed(main);
        String name = "Event";
        Set<Category> categories = Set.of(Category.of("Category1"), Category.of("Category2"));
        main.addNoteToCategories(categories, main.getNote(0));
        main.addNoteToCategories(categories, main.getNote(1));
        main.addNoteToCategory("Category1", main.getNote(2));
        SearchCriteria criteria = new SearchCriteria().setNoteClass(ScheduleItem.class)
                .setCategories(categories).setNameMatch(name);
        List<NoteBase> orResult = main.findMatchingNotes(criteria);
        assertEquals(3, orResult.size(), "Number of ScheduleItem notes with categories "
                + categories + " and name '" + name + "'");
    }

    @Test
    public void getNotesByNamePatternClassAndCategory() {
        createScheduleItemTestBed(main);
        String pattern = "Event \\d";
        Set<Category> categories = Set.of(Category.of("Category1"), Category.of("Category2"));
        main.addNoteToCategories(categories, main.getNote(0));
        main.addNoteToCategories(categories, main.getNote(1));
        main.addNoteToCategory("Category1", main.getNote(2));
        main.addNoteToCategory("Category2", main.getNote(3));
        SearchCriteria criteria = new SearchCriteria().setNoteClass(ScheduleItem.class)
                .setCategories(categories).joinWithAnd().setNameMatch(pattern).doPatternMatch();
        List<NoteBase> andResult = main.findMatchingNotes(criteria);
        assertEquals(2, andResult.size(), "Number of ScheduleItem notes with categories "
                + categories + " and name matching pattern '" + pattern + "'");
    }

    // @Test
    public void testTemplate() {
        throw new RuntimeException("Test not yet implemented");
    }

    // ToDo: Add tests for various note types, categories, and child notes (need to set up test data)
    //  * Search in sub-notes
}
