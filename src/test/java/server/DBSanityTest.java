package server;

import org.hibernate.Session;
import org.junit.jupiter.api.*;
import server.categories.Category;
import server.element.*;
import server.note.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static server.NoteTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Note: The DB must be running for these tests to work.
 * If it's not try: pg_ctl restart -D "C:/Program Files/PostgreSQL/18/data"
 * Note that you may have to try running this command more than once
 */
// ToDo: Figure out JUnit assertThat() with matchers (and replace assertTrue())
// ToDo: Switch to TestNG Test annotations and framework (instead of JUnit)?
//   If so use the description attribute to document the tests

// ToDo: Move into server.database package?
// ToDo: Add test for a 3-level note (i.e. a note with a child note that has a child)
// ToDo: Add tests for notes with one or more categories
// ToDo: Test removing Category from note (verify by searching for updated note)
// ToDo: Add Contact test with multiple Addresses, Phones, and, Emails (and assertEquals() on full lists)
// ToDo: Add tests for setting the completed property in ScheduleItem
// ToDo: Add DB configuration tests (combinations of config files and environment variables)
//  - Test non-existent config file
// ToDo: Try adding a duplicate category (looks like it's currently allowed)
// ToDo: Test deleting one child note from a list of multiple
// ToDo: Test cascading deletes (deleting parent entity also deletes children)
// ToDo: Add tests for CRUD operations on each entity type (Note, Contact, MediaItem, etc.)
// ToDo: Add deletion tests
// ToDo: Add tests for Note ordering
// ToDo: Come up with some negative tests (null Note, etc.)
public class DBSanityTest extends TestBase {

    @Test
    public void testDBConnection() {
        // This is a basic session sanity test
        try (Session testSession = testSessionFactory.openSession()) {
            assertTrue(testSession.isOpen(), "Test session is open");
            assertFalse(testSession.getProperties().isEmpty(), "Expected non-empty properties for open DB session");
        } catch (Exception e) {
            fail("Failed to open DB session: " + e.getMessage());
        }
    }

    @Test
    public void createSimpleNote() {
        NoteBase simpleNote = getSimpleNote();
        NoteBase fetchedNote = commitAndRetrieveTestNote(simpleNote);
        assertEquals(simpleNote.getName(), fetchedNote.getName(), "Simple Note name");
    }

    @Test
    public void createNote() {
        String noteName = "New Note 1";
        Note newNote = new Note(noteName, "Testing Note creation");;
        Note fetchedNote = commitAndRetrieveTestNote(newNote);
        assertEquals(newNote.getName(), fetchedNote.getName(), "New Note name");
    }

    // ToDo: Add static noteID and increment it to distinguish Note names?
    @Test
    public void createNoteWithOneChildNote() {
        Note newNote = new Note("NewNote2", "Testing adding a child note");
        Note childNote = new Note("ChildNote1", "Testing adding a child note");
        newNote.addChildNote(childNote);
        Note fetchedNote = commitAndRetrieveTestNote(newNote);
        assertEquals(newNote.getName(), fetchedNote.getName());
        assertEquals(1, fetchedNote.getChildNotes().size(), "Number of child notes");
        // ToDo: Verify child note name as well?
        // System.out.println("Child Note name: " + fetchedNote.getChildNote(0).getName());
    }

    @Test
    public void createNoteWithOneChildOfEachNoteType() {
        Note newNote = getTestNote();
        newNote.addChildNote(getSimpleNote());
        newNote.addChildNote(getTestNote());
        newNote.addChildNote(getTestPerson());
        newNote.addChildNote(getTestBusiness());
        newNote.addChildNote(getTestProduct());
        newNote.addChildNote(getTestArtist());
        newNote.addChildNote(getGoogleLink());
        newNote.addChildNote(getTestScheduleItem());
        List<? extends NoteBase> children = commitAndRetrieveTestNote(newNote).getChildNotes();
        // ToDo: Do a direct List compare here?
        assertEquals(newNote.getChildNotes().size(), children.size(), "Number of child notes");
        List<Class<? extends NoteBase>> childTypes = List.of(NoteBase.class, Note.class, Contact.class,
                Contact.class, Product.class, Entity.class, Link.class, ScheduleItem.class);
        for (int i = 0; i < childTypes.size(); i++) {
            assertEquals(childTypes.get(i), children.get(i).getClass(), "Type for child #" + (i + 1));
        }
    }

    // @Test
    public void createNoteWithCategory() {

    }

    // @Test
    public void addCategoryWithSpaceInName() {
/*
        String categoryName = "Category name with spaces";
        main.addCategory(categoryName);
        Set<NoteBase> category = main.getNotesForCategory(categoryName);
        assertEquals(0, category.size(), "Number of notes in category");
*/
    }


    @Test
    public void createNoteWithLink() {
        Note newNote = new Note("NewNote3", "Testing adding a link to a note");
        Link newLink = getGoogleLink();
        newNote.addLink(newLink);
        Note fetchedNote = commitAndRetrieveTestNote(newNote);
        assertEquals(1, fetchedNote.getLinks().size(), "Number of Note links");
        assertEquals(newLink.getUrl(), fetchedNote.getLink(0).getUrl(), "Link added to Note");
    }

    @Test
    public void createEntity() {
        Entity newEntity = getTestArtist();
        Entity fetchedNote = commitAndRetrieveTestNote(newEntity);
        assertEquals(newEntity.getType(), fetchedNote.getType(), "Entity type");
        assertEquals(newEntity.getName(), fetchedNote.getName(), "Artist name");
    }

    @Test
    public void createContact() {
        Contact person = Contact.newPerson("Person1", "LastName1");
        Contact fetchedNote = commitAndRetrieveTestNote(person);
        assertEquals(person.getName(), fetchedNote.getName(), "Person name");
        assertEquals(1, findMatchingNotesInDB(new SearchCriteria().setNoteClass(Contact.class)).size(),
                "Number of Contact notes in DB");
    }

    @Test
    public void createContactWithAddressPhoneAndEmail() {
        Contact person = Contact.newPerson("Person2", "LastName2");
        person.addAddress(new Address().setAddress1("123 4th St.").setCity("Menlo Park"));
        person.addPhoneNumber("775-3433", Phone.PhoneType.HOME);
        person.addEmail("user1@gmail.com", Email.EmailType.PERSONAL);
        Contact fetchedNote = commitAndRetrieveTestNote(person);
        assertEquals(1, fetchedNote.getAddresses().size(), "Number of addresses");
        assertEquals(1, fetchedNote.getPhoneNumbers().size(), "Number of Phone numbers");
        assertEquals(1, fetchedNote.getEmails().size(), "Number of Emails");
    }

    @Test
    public void createProduct() {
        Product newProduct = getTestProduct();
        Product fetchedNote = commitAndRetrieveTestNote(newProduct);
        assertEquals(newProduct.getType(), fetchedNote.getType(), "Product category");
        assertEquals(newProduct.getOwner().getName(), fetchedNote.getOwner().getName(), "Owner name");
        assertEquals(newProduct.getPrice(), fetchedNote.getPrice(), "Product price");
        assertEquals(1, findMatchingNotesInDB(new SearchCriteria().setNoteClass(Product.class)).size(),
                "Number of Product notes in DB");
    }

    @Test
    public void createLink() {
        Link newLink = getGoogleLink();
        Link fetchedNote = commitAndRetrieveTestNote(newLink);
        assertEquals(newLink.getUrl(), fetchedNote.getUrl(), "Link URL");
    }

    @Test
    public void createScheduleItem() {
        ScheduleItem event = new ScheduleItem("Deadline", null, ScheduleItem.convertToDate("2020-06-12 00:00:00 AM"));
        ScheduleItem fetchedNote = commitAndRetrieveTestNote(event);
        assertEquals(1, findMatchingNotesInDB(new SearchCriteria().setNoteClass(ScheduleItem.class)).size(),
                "Number of ScheduleItem notes in DB");
        assertEquals(event.getDate(), fetchedNote.getDate(), "Event date");
    }

    // @Test
    public void scheduleItemsAreRetrievedInTimestampOrder() {

    }

    // @Test
    // ToDo: Need some provision for returning data for a schedule view from the DB
    public void callGetScheduleWithNoScheduleItemsAdded() {
        // assertEquals(0, main.getSchedule().size(), "Number of schedule items");
    }

    @Test
    public void createMultipleNotes() {
        int expectedCount = 3;
        List<NoteBase> fetchedNotes = commitAndRetrieveTestNotes(expectedCount);
        assertEquals(expectedCount, fetchedNotes.size(), "Number of notes created");
    }

    // ToDo: Add tests for updating notes and convert deletion tests, below

    // @Test
    public void removeOnlyNoteFromDB() {
        // assertEquals(0, main.getNotes().size(), "Number of notes");
        // assertSame(newNote, deletedNote, "Note removed");
    }

    // @Test
    public void removeMultipleNotesFromDB() {
        // ToDo: Verify that the SAME notes are being deleted that were added
/*
        int expectedCount = 3;
        persistNewTestNotes(expectedCount, main);
        for (int i = 0; i < expectedCount; i++) {
            NoteBase noteToRemove = main.getNote(0);
            // ToDo: Test the index version too: deleteNote(int index)?
            NoteBase removedNote = main.deleteNote(noteToRemove);
            assertEquals(expectedCount - i - 1, main.getNotes().size(), "Number of notes");
            assertSame(noteToRemove, removedNote, "Note removed");
        }
*/
    }

    // @Test
    public void removeAllNotesFromDB() {
/*
        int initialNoteCount = 5;
        persistNewTestNotes(initialNoteCount, main);
        main.getNotes().clear();
        assertEquals(0, main.getNotes().size(), "Number of remaining notes after deletion");
*/
    }

    // @Test
    public void removeOneOfMultipleNotesFromDB() {
/*
        int initialNoteCount = 5;
        persistNewTestNotes(initialNoteCount,  main);
        // ToDo: Verify that I deleted the RIGHT note
        main.deleteNote(0);
        assertEquals(initialNoteCount - 1, main.getNotes().size(), "Number of remaining notes after deletion");;
*/
    }

    // @Test
    public void removeNoteWithCategoriesFromDB() {
/*
        Note newNote = NoteTestHelper.createGenericTestNote();
        main.addNote(newNote);
        Category[] categories = new Category[] {
                Category.of("category1"), Category.of("category2"), Category.of("category3")};
        main.addNoteToCategories(new HashSet<>(Arrays.asList(categories)), newNote);
        main.addCategory("category4");
        main.deleteNote(newNote);
        // Confirm that the note has been deleted from all categories
        for (String name : main.getCategories()) {
            assertFalse(main.getNotesForCategory(name).contains(newNote),
                    "Category '" + name + "' contains note '" + newNote.getName() + "'");
        }
*/
    }

}
