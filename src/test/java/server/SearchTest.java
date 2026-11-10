package server;

import server.categories.Category;
import server.note.*;
import server.categories.MediaType;
import org.junit.jupiter.api.*;
import java.util.*;

import static server.note.Contact.*;
import static server.NoteTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

// ToDo: Test scenarios to cover:
//  - Note with 2 sub-note levels
//  - Note with many sub-notes
//  - Notes with and without categories
//  - Sub-notes with and without categories
//  - Notes and sub-notes with different class types
// ToDo: Add tests for various note types, categories, and child notes (need to set up test data)
//  * Search in sub-notes
public class SearchTest extends TestBase {
    // ToDo: Set up data providers for search tests after moving to TestNG
    @Test
    public void commitSingleNoteWithCategory() {
        Category cat = Category.of(MediaType.SONG);
        Note newNote = NoteTestHelper.createGenericTestNote();
        newNote.addCategory(cat);
        commitObjectToDB(newNote);
        // ToDo: Change this to HibernateUtil.runTransaction()?
        runTestTransaction(session -> {
            Note fetchedNote = retrieveNoteFromDB(session, newNote);
            SearchCriteria criteria = new SearchCriteria().setCategory(cat);
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
        commitTestNotesWithCategories(numberOfNotes, Set.of(cat));
        runTestTransaction(session -> {
            SearchCriteria criteria = new SearchCriteria().setCategory(cat);
            List<? extends NoteBase> notes = findMatchingNotesInDB(session, criteria);
            assertEquals(numberOfNotes, notes.size(), "Number of notes with media type " + cat);
        });
    }

    @Test
    public void getMultipleNotesWithVariousCategories() {
        int numberOfNotes = 10;
        commitTestNotesWithCategories(numberOfNotes, null);   // This creates notes with random categories
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
    public void getCategorizedNotesFromDBContainingCategorizedAndUncategorizedNotes() {
        int numberOfCategorizedNotes = 10;
        int numberOfUncategorizedNotes = 5;
        commitTestNotesWithCategories(numberOfCategorizedNotes, null);    // Create notes with random categories
        // This is identical to the above test except for this line adding uncategorized notes
        commitTestNotesWithCategories(numberOfUncategorizedNotes, Set.of());        // Create uncategorized notes
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
        commitTestNotesWithCategories(10, Set.of());   // Create uncategorized notes
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
        Contact contact = newPerson("Jane", "Doe");
        commitObjectToDB(contact);
        commitNewTestNotes(3);
        runTestTransaction(session -> {
            SearchCriteria criteria = new SearchCriteria().setNoteClass(Contact.class);
            List<? extends NoteBase> result = findMatchingNotesInDB(session, criteria);
            assertEquals(List.of(contact), result, "Notes matching class Contact");
        });
    }

    // Defining static Note types for tests is dangerous because they are cached with Note IDs and cause duplicate errors
    static final List<String> testNoteNames = List.of("Grocery list", "Hardware list", "Birthday ideas");
    @Test
    public void getNotesByPartialName() {
        commitTestNotesWithNames(testNoteNames);
        // ToDo: This doesn't handle case-insensitive search
        SearchCriteria criteria = new SearchCriteria().setNameMatch("list");
        runTestTransaction(session -> {
            List<? extends NoteBase> result = findMatchingNotesInDB(session, criteria);
            assertEquals(2, result.size(), "Notes with name containing 'list' (case-insensitive)");
        });
    }

    @Test
    public void getNotesByPatternMatch() {
        String pattern = ".* list";
        commitTestNotesWithNames(testNoteNames);
        SearchCriteria criteria = new SearchCriteria().setNameMatch(pattern).doPatternMatch();
        runTestTransaction(session -> {
            List<? extends NoteBase> result = findMatchingNotesInDB(session, criteria);
            assertEquals(2, result.size(), "Notes with name matching pattern '" +  pattern + "'");
        });
    }

    @Test
    public void getMatchingChildNoteOfNonMatchingParent() {
        String mediaType = MediaType.SONG.toString();
        Note parent = new Note("Parent");
        Note child = new Note("Child");
        child.addCategory(mediaType);
        parent.addChildNote(child);
        commitObjectToDB(parent);
        SearchCriteria criteria = new SearchCriteria().setCategory(mediaType);
        runTestTransaction(session -> {
            assertEquals(List.of(child), findMatchingNotesInDB(session, criteria), "Child notes searched");
        });
    }

    @Test
    public void getNotesByNameAndClass() {
        createScheduleItemTestBed();
        String name = "Test Name 1";
        commitObjectToDB(new ScheduleItem(name, null, "2020-07-01 09:00:00 PM"));
        SearchCriteria criteria = new SearchCriteria().setNoteClass(ScheduleItem.class).setNameMatch(name);
        runTestTransaction(session -> {
            List<? extends NoteBase> andResult = findMatchingNotesInDB(session, criteria);
            assertEquals(1, andResult.size(),
                    "Number of ScheduleItem notes with name '" + name + "'");
        });
    }

    @Test
    public void getNotesByNamePatternAndClass() {
        createScheduleItemTestBed();
        runTestTransaction(session -> {
            SearchCriteria criteria = new SearchCriteria().setNoteClass(ScheduleItem.class);
            assertEquals(5, findMatchingNotesInDB(session, criteria).size(), "Total number of ScheduleItem notes");
            String pattern = "Event [1-3]";
            criteria.setNameMatch(pattern).doPatternMatch();
            assertEquals(3, findMatchingNotesInDB(session, criteria).size(),
                    "Number of ScheduleItem notes with name matching pattern '" + pattern + "'");
        });
    }

    @Test
    public void getNotesByNameAndCategory() {
        Set<Category> categories = commitNotesWithMultipleCategories();
        runTestTransaction(session -> {
            SearchCriteria criteria = new SearchCriteria().setCategories(categories).joinWithAnd()
                    .setNameMatch(defaultBaseName);
            List<? extends NoteBase> andResult = findMatchingNotesInDB(session, criteria);
            assertEquals(partitionCount, andResult.size(), "Number of notes with categories and name '"
                    + defaultBaseName + "'");
        });
    }

    @Test
    public void getNotesByNamePatternAndCategory() {
        String pattern = "T.* N.*";
        Set<Category> categories = commitNotesWithMultipleCategories();
        SearchCriteria criteria = new SearchCriteria().setCategories(categories).joinWithAnd()
                .setNameMatch(pattern).doPatternMatch();
        runTestTransaction(session -> {
            List<? extends NoteBase> andResult = findMatchingNotesInDB(session, criteria);
            assertEquals(partitionCount, andResult.size(), "Number of notes with categories and name matching pattern '"
                    + pattern + "'");
        });
    }

    static final Set<Category> testCategories = Set.of(Category.of("Category1"), Category.of("Category2"));
    static final String baseName = "Event";
    @Test
    public void getNotesByNameClassAndCategory() {
        createScheduleItemTestBed();
        // ToDo: It would be really nice to have some kind of generic Note creation factory
        //  - For now just create ScheduleItem notes in memory, add the categories, and then commit
        runTestTransaction(session -> {
            ScheduleItem item6 = new ScheduleItem(baseName + " 6", null, "2020-07-07 09:00:00 PM");
            item6.addCategories(testCategories);
            persistObjectToDB(session, item6);
            ScheduleItem item7 = new ScheduleItem(baseName + " 7", null, "2020-07-08 09:00:00 PM");
            item7.addCategories(testCategories);
            persistObjectToDB(session, item7);
            ScheduleItem item8 = new ScheduleItem(baseName + " 8", null, "2020-07-09 09:00:00 PM");
            item8.addCategory(Category.of("Category1"));
            persistObjectToDB(session, item8);
        });
        SearchCriteria criteria = new SearchCriteria().setNoteClass(ScheduleItem.class)
                .setCategories(testCategories).setNameMatch(baseName);
        runTestTransaction(session -> {
            List<? extends NoteBase> orResult = findMatchingNotesInDB(session, criteria);
            assertEquals(3, orResult.size(), "Number of ScheduleItem notes with categories "
                    + testCategories + " and name '" + baseName + "'");
        });
    }

    @Test
    public void getNotesByNamePatternClassAndCategory() {
        createScheduleItemTestBed();
        String pattern = "Event \\d";
        runTestTransaction(session -> {
            ScheduleItem item6 = new ScheduleItem(baseName + " 6", null, "2020-07-07 09:00:00 PM");
            item6.addCategories(testCategories);
            persistObjectToDB(session, item6);
            ScheduleItem item7 = new ScheduleItem(baseName + " 7", null, "2020-07-08 09:00:00 PM");
            item7.addCategories(testCategories);
            persistObjectToDB(session, item7);
            ScheduleItem item8 = new ScheduleItem(baseName + " 8", null, "2020-07-09 09:00:00 PM");
            item8.addCategory(Category.of("Category1"));
            persistObjectToDB(session, item8);
            ScheduleItem item9 = new ScheduleItem(baseName + " 9", null, "2020-07-10 09:00:00 PM");
            item9.addCategory(Category.of("Category2"));
            persistObjectToDB(session, item9);
        });
        SearchCriteria criteria = new SearchCriteria().setNoteClass(ScheduleItem.class)
                .setCategories(testCategories).joinWithAnd().setNameMatch(pattern).doPatternMatch();
        runTestTransaction(session -> {
            List<? extends NoteBase> andResult = findMatchingNotesInDB(session, criteria);
            assertEquals(2, andResult.size(), "Number of ScheduleItem notes with categories "
                    + testCategories + " and name matching pattern '" + pattern + "'");
        });
    }

    // @Test
    public void testTemplate() {
        throw new RuntimeException("Test not yet implemented");
    }
}
