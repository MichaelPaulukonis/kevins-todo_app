package server;

import server.note.*;
import server.categories.MediaType;
import org.junit.jupiter.api.*;

import java.util.*;
import static server.NoteTestHelper.*;
import static server.note.Contact.*;
import static org.junit.jupiter.api.Assertions.*;

// ToDo: Add initializeTestData() for more complex test scenarios
//  - Note with 2 sub-note levels
//  - Note with many sub-notes
//  - Notes with and without categories
//  - Sub-notes with and without categories
//  - Notes and sub-notes with different class types
public class SearchTest extends TestBase {
    // ToDo: Set up data providers for search tests after moving to TestNG
    @Test
    public void getSingleNoteWithCategory() {
        Note newNote = NoteTestHelper.createGenericTestNote();
        String mediaType = MediaType.SONG.toString();
        newNote.addCategory(mediaType);
        main.addNote(newNote);
        SearchCriteria criteria = new SearchCriteria().setCategory(mediaType);
        List<NoteBase> notes = main.findMatchingNotes(criteria);
        assertEquals(1, notes.size(), "Number of notes with media type " + mediaType);
        assertTrue(notes.contains(newNote), "Note list contains new note with media type " + mediaType);
        // ToDo: Why do I have this remove assertion here?
        assertTrue(notes.remove(newNote), "New note with media type " + mediaType + " is in list");
    }

    @Test
    public void getMultipleNotesWithSameCategory() {
        String mediaType = MediaType.SONG.name();
        int numberOfNotes = 3;
        addNotesWithCategory(numberOfNotes, mediaType, main);
        SearchCriteria criteria = new SearchCriteria().setCategory(mediaType);
        List<NoteBase> notes = main.findMatchingNotes(criteria);
        assertEquals(numberOfNotes, notes.size(), "Number of notes with media type " + mediaType);
    }

    @Test
    public void getMultipleNotesWithVariousCategories() {
        int numberOfNotes = 10;
        int total = 0;
        addNotesWithCategory(numberOfNotes, null, main);
        for (String cat : getCategories(main)) {
            SearchCriteria criteria = new SearchCriteria().setCategory(cat);
            List<NoteBase> notes = main.findMatchingNotes(criteria);
            assertEquals(numberOfNotesForCategory(cat, main), notes.size(),
                    "Number of notes with media type " + cat);
            total += notes.size();
        }
        assertEquals(numberOfNotes, total, "Total number of categorized notes");
    }

    @Test
    public void getCategorizedNotesFromListContainingCategorizedAndUncategorizedNotes() {
        int numberOfCategorizedNotes = 10;
        int numberOfUncategorizedNotes = 5;
        int total = 0;
        addNotesWithCategory(numberOfCategorizedNotes, null, main);
        // This is identical to the above test except for this line adding uncategorized notes
        addTestNotes(numberOfUncategorizedNotes, main);
        assertEquals(numberOfCategorizedNotes + numberOfUncategorizedNotes, main.getNotes().size(),
                "Total number of notes");
        for (String cat : getCategories(main)) {
            SearchCriteria criteria = new SearchCriteria().setCategory(cat);
            List<NoteBase> notes = main.findMatchingNotes(criteria);
            assertEquals(numberOfNotesForCategory(cat, main), notes.size(),
                    "Number of notes with media type " + cat);
            total += notes.size();
        }
        assertEquals(numberOfCategorizedNotes, total, "Total number of tagged notes");
    }

    @Test
    public void tryGettingCategorizedNotesFromListContainingOnlyUncategorizedNotes() {
        addTestNotes(10, main);     // These are uncategorized notes
        assertEquals(0, NoteTestHelper.numberOfCategories(main), "Number of test categories tracked");
        for (String cat : MediaType.get().convertToStrings()) {
            SearchCriteria criteria = new SearchCriteria().setCategory(cat);
            List<NoteBase> notes = main.findMatchingNotes(criteria);
            assertEquals(0, notes.size(), "Number of notes with media type " + cat);
        }
    }

    @Test
    public void tryGettingCategorizedNotesFromEmptyList() {
        // The list of test notes is empty by default
        assertEquals(0, numberOfCategories(main), "Number of test categories tracked");
        for (String cat : MediaType.get().convertToStrings()) {
            SearchCriteria criteria = new SearchCriteria().setCategory(cat);
            List<NoteBase> notes = main.findMatchingNotes(criteria);
            assertEquals(0, notes.size(), "Number of notes with media type " + cat);
        }
    }

    // ToDo: Add test for trying to get tagged note from list that doesn't contain the tag

    @Test
    public void getCategorizedNotesWithOrRelationship() {
        Set<String> categories = createNotesWithMultipleCategories(main);
        SearchCriteria criteria = new SearchCriteria().setCategories(categories);
        List<NoteBase> orResult = main.findMatchingNotes(criteria);    // Matching is done via OR" by default
        assertEquals(partitionCount*3, orResult.size(), "Number of notes with any queried categories");
    }

    @Test
    public void getCategorizedNotesWithAndRelationship() {
        Set<String> categories = createNotesWithMultipleCategories(main);
        SearchCriteria criteria = new SearchCriteria().setCategories(categories).joinWithAnd();
        List<NoteBase> andResult = main.findMatchingNotes(criteria);
        assertEquals(partitionCount, andResult.size(), "Number of notes with all queried categories");
    }

    @Test
    public void getNotesByClass() {
        addTestNotes(3, main);
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
        Set<String> categories = createNotesWithMultipleCategories(main);
        String name = "Test Note";   // This is the default name for test notes
        SearchCriteria criteria = new SearchCriteria().setCategories(categories).joinWithAnd().setNameMatch(name);
        List<NoteBase> andResult = main.findMatchingNotes(criteria);
        assertEquals(partitionCount, andResult.size(), "Number of notes with categories and name '" + name + "'");
    }

    @Test
    public void getNotesByNamePatternAndCategory() {
        String pattern = "T.* N.*";
        Set<String> categories = createNotesWithMultipleCategories(main);
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
        Set<String> categories = Set.of("Category1", "Category2");
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
        Set<String> categories = Set.of("Category1", "Category2");
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
