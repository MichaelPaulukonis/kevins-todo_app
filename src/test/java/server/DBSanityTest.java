package server;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.*;
import org.hibernate.Session;
import server.categories.Category;
import server.categories.MediaType;
import server.note.*;

import static server.database.HibernateUtil.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Note: The DB must be running for these tests to work.
 * If it's not try: pg_ctl restart -D "C:/Program Files/PostgreSQL/18/data"
 * Note that you may have to try running this command more than once
 */
// ToDo: Move into server.database package?
public class DBSanityTest {
    private static StandardServiceRegistry testServiceRegistry;
    private static SessionFactory testSessionFactory;
    private Session testSession;

    // ToDo: Move all this setup and teardown code into TestBase?
    @BeforeAll
    public static void setUpTestSessionFactory() {
        testServiceRegistry = buildServiceRegistry(true);
        testSessionFactory = getSessionFactory(testServiceRegistry);
    }

    @BeforeEach
    public void openNewSession() throws Exception {
        if (testSessionFactory == null) {
            throw new Exception("Failed to create Hibernate SessionFactory");
        }
        testSession = testSessionFactory.openSession();
        testSession.beginTransaction();
    }

    @AfterEach
    public void closeSession() {
        if (testSession != null && testSession.isOpen()) {
            if (testSession.getTransaction().isActive()) {
                testSession.getTransaction().rollback();   // Rollback transaction so test DB stays clean
            }
            testSession.close();
        }
    }

    @AfterAll
    public static void closeSessionFactory() {
        if (testSessionFactory != null) {
            testSessionFactory.close();
        }
        if (testServiceRegistry != null) {
            StandardServiceRegistryBuilder.destroy(testServiceRegistry);
        }
    }

    @Test
    public void testDBConnection() {
        // This is a basic session sanity test
        assertTrue(testSession.isOpen(), "Test session in open");
        assertFalse(testSession.getProperties().isEmpty(), "Expected non-empty properties for open DB session");
        testSession.close();
        assertFalse(testSession.isOpen(), "Test session in open");
    }

    private static final String simpleNoteName = "Simple Note 1";
    private static final NoteBase simpleNote = new NoteBase(simpleNoteName, "Simple NoteBase object");
    @Test
    public void createSimpleNote() {
        NoteBase fetchedNote = saveAndRetrieveNote(testSession, simpleNote);
        assertEquals(simpleNoteName, fetchedNote.getName(), "Simple Note name");
    }

    @Test
    public void createNote() {
        String noteName = "New Note 1";
        Note newNote = new Note(noteName, "Testing Note creation");;
        Note fetchedNote = saveAndRetrieveNote(testSession, newNote);
        assertEquals(newNote.getName(), fetchedNote.getName(), "New Note name");
    }

    // ToDo: Add static noteID and increment it to distinguish Note names?
    @Test
    public void createNoteWithOneChildNote() {
        Note newNote = new Note("NewNote2", "Testing adding a child note");
        Note childNote = new Note("ChildNote1", "Testing adding a child note");
        newNote.addChildNote(childNote);
        Note fetchedNote = saveAndRetrieveNote(testSession, newNote);
        assertEquals(newNote.getName(), fetchedNote.getName());
        assertEquals(1, fetchedNote.getChildNotes().size(), "Number of child notes");
        // ToDo: Verify child note name as well?
        // System.out.println("Child Note name: " + fetchedNote.getChildNote(0).getName());
    }

    // ToDo: createNoteWithMultipleChildNotes()
    // ToDo: Create static test Link (like simpleNote)?
    @Test
    public void createNoteWithLink() {
        String url = "http://google.com";    // ToDo: Make this a static constant
        Note newNote = new Note("NewNote3", "Testing adding a link to a note");
        newNote.addLink(new Link("Google", null, url));
        Note fetchedNote = saveAndRetrieveNote(testSession, newNote);
        assertEquals(1, fetchedNote.getLinks().size(), "Number of Note links");
        assertEquals(url, fetchedNote.getLink(0).getUrl().toString(), "Link added to Note");
    }

    @Test
    public void createEntity() {
        // ToDo: Factor out static final Entity type for testing?
        Entity artist = new Entity("Grand Funk", null, Entity.EntityType.ARTIST);
        Entity fetchedNote = saveAndRetrieveNote(testSession, artist);
        assertEquals(artist.getType(), fetchedNote.getType(), "Entity type");
    }

    @Test
    public void createContact() {
        String firstName = "Person1";
        String lastName = "LastName";
        Contact person = Contact.newPerson(firstName, lastName);
        Note fetchedNote = saveAndRetrieveNote(testSession, person);
        assertEquals(firstName + " " + lastName, fetchedNote.getName(), "Person name");
    }

    @Test
    public void createProduct() {
        Entity owner = new Entity("Fanny", null, Entity.EntityType.ARTIST);
        Product newProduct = new Product("Blind Alley", null, null).setType(Category.of(MediaType.SONG));
        newProduct.setOwner(owner);
        Product fetchedNote = saveAndRetrieveNote(testSession, newProduct);
        // ToDo: Also assert on owner ID?
        assertEquals(newProduct.getType(), fetchedNote.getType(), "Product category");
    }

    @Test
    public void createLink() {
        Link webLink = new Link("Google", null, "http://google.com");
        Link fetchedNote = saveAndRetrieveNote(testSession, webLink);
        assertEquals(webLink.getUrl(), fetchedNote.getUrl(), "Link URL");
    }

    @Test
    public void createScheduleItem() {
        ScheduleItem event = new ScheduleItem("Deadline", null, ScheduleItem.convertToDate("2020-06-12 00:00:00 AM"));
        ScheduleItem fetchedNote = saveAndRetrieveNote(testSession, event);
        assertEquals(event.getDate(), fetchedNote.getDate(), "Event date");
    }
}

// ToDo: Test adding addresses and phone numbers to Contact
// ToDo: Test adding links to a note
// ToDo: Add deletion tests
// ToDo: Test deleting one child note from a list of multiple
// ToDo: Test cascading deletes (deleting parent entity also deletes children)
// ToDo: Add tests for CRUD operations on each entity type (Note, Contact, MediaItem, etc.)
// ToDo: Add tests for complex queries
// ToDo: Add tests for verifying managed entities (dbSession.getManagedEntities())
//       Verify there is at least one managed entity?
// ToDo: Add commit tests (using separate sessions for create and retrieve) - Put them in a separate class?
// ToDo: Come up with some negative tests (null Note, etc.)
