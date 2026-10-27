package server;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.*;
import org.hibernate.Session;
import server.categories.MediaType;
import server.element.*;
import server.note.*;

import java.time.LocalDateTime;
import java.util.List;

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
    public static void setUpTestSessionFactory() throws Exception {
        testServiceRegistry = getServiceRegistry(true);
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

    @Test
    public void createSimpleNote() {
        NoteBase simpleNote = getSimpleNote();
        NoteBase fetchedNote = saveAndRetrieveNote(testSession, simpleNote);
        assertEquals(simpleNote.getName(), fetchedNote.getName(), "Simple Note name");
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
        Note fetchedNote = saveAndRetrieveNote(testSession, newNote);
        List<? extends NoteBase> children = fetchedNote.getChildNotes();
        assertEquals(8, children.size(), "Number of child notes");
        List<Class<? extends NoteBase>> childTypes = List.of(NoteBase.class, Note.class, Contact.class,
                Contact.class, Product.class, Entity.class, Link.class, ScheduleItem.class);
        for (int i = 0; i < childTypes.size(); i++) {
            assertEquals(childTypes.get(i), children.get(i).getClass(), "Type for child #" + (i + 1));
        }
    }

    @Test
    public void createNoteWithLink() {
        Note newNote = new Note("NewNote3", "Testing adding a link to a note");
        Link newLink = getGoogleLink();
        newNote.addLink(newLink);
        Note fetchedNote = saveAndRetrieveNote(testSession, newNote);
        assertEquals(1, fetchedNote.getLinks().size(), "Number of Note links");
        assertEquals(newLink.getUrl(), fetchedNote.getLink(0).getUrl(), "Link added to Note");
    }

    @Test
    public void createEntity() {
        Entity newEntity = getTestArtist();
        Entity fetchedNote = saveAndRetrieveNote(testSession, newEntity);
        assertEquals(newEntity.getType(), fetchedNote.getType(), "Entity type");
        assertEquals(newEntity.getName(), fetchedNote.getName(), "Artist name");
    }

    @Test
    public void createContact() {
        Contact person = Contact.newPerson("Person1", "LastName1");
        Contact fetchedNote = saveAndRetrieveNote(testSession, person);
        assertEquals(person.getName(), fetchedNote.getName(), "Person name");
    }

    @Test
    public void createContactWithAddressPhoneAndEmail() {
        Contact person = Contact.newPerson("Person2", "LastName2");
        person.addAddress(new Address().setAddress1("123 4th St.").setCity("Menlo Park"));
        person.addPhoneNumber("775-3433", Phone.PhoneType.HOME);
        person.addEmail("user1@gmail.com", Email.EmailType.PERSONAL);
        Contact fetchedNote = saveAndRetrieveNote(testSession, person);
        assertEquals(1, fetchedNote.getAddresses().size(), "Number of addresses");
        assertEquals(1, fetchedNote.getPhoneNumbers().size(), "Number of Phone numbers");
        assertEquals(1, fetchedNote.getEmails().size(), "Number of Emails");
    }

    @Test
    public void createProduct() {
        Product newProduct = getTestProduct();
        Product fetchedNote = saveAndRetrieveNote(testSession, newProduct);
        assertEquals(newProduct.getType(), fetchedNote.getType(), "Product category");
        assertEquals(newProduct.getOwner().getName(), fetchedNote.getOwner().getName(), "Owner name");
        assertEquals(newProduct.getPrice(), fetchedNote.getPrice(), "Product price");
    }

    @Test
    public void createLink() {
        Link newLink = getGoogleLink();
        Link fetchedNote = saveAndRetrieveNote(testSession, newLink);
        assertEquals(newLink.getUrl(), fetchedNote.getUrl(), "Link URL");
    }

    @Test
    public void createScheduleItem() {
        ScheduleItem event = new ScheduleItem("Deadline", null, ScheduleItem.convertToDate("2020-06-12 00:00:00 AM"));
        ScheduleItem fetchedNote = saveAndRetrieveNote(testSession, event);
        assertEquals(event.getDate(), fetchedNote.getDate(), "Event date");
    }

    // ================ Helper methods ================ (ToDo: Move these into a base test class)
    // ToDo: Support these methods for each Note type
    protected static NoteBase getSimpleNote() {
        return new NoteBase("Simple Note 1", "Simple NoteBase object");
    }

    private static int noteNumber = 1;
    protected static Note getTestNote() {
        return new Note("Note#" + noteNumber, "Testing note #" + noteNumber++);
    }

    private static int personNumber = 1;
    protected static Contact getTestPerson() {
        return Contact.newPerson("FirstName" + personNumber, "LastName" + personNumber++);
    }

    private static int businessNumber = 1;
    private static Contact getTestBusiness() {
        return Contact.newBusiness("Business#" + businessNumber, "Description for business #" + businessNumber++);
    }

    protected static Link getGoogleLink() {
        return new Link("Google", null, "http://google.com");
    }

    protected static Entity getTestArtist() {
        return new Entity("Grand Funk", null, Entity.EntityType.ARTIST);
    }

    private static int productNumber = 1;
    protected static Product getTestProduct() {
        return new Product("Product#" + productNumber++)
                .setOwner(getTestArtist()).setPrice(5.99).setType(MediaType.SONG);
    }

    protected static int eventNumber = 1;
    protected static ScheduleItem getTestScheduleItem() {
        return new ScheduleItem("Event#" + eventNumber, null, LocalDateTime.now().plusDays(eventNumber++));
    }
}

// ToDo: Add Contact test with multiple Addresses, Phones, and, Emails (and assertEquals() on full lists)
// ToDo: Add DB configuration tests (combinations of config files and environment variables)
//  - Test non-existent config file
// ToDo: Add deletion tests
// ToDo: Test deleting one child note from a list of multiple
// ToDo: Test cascading deletes (deleting parent entity also deletes children)
// ToDo: Add tests for CRUD operations on each entity type (Note, Contact, MediaItem, etc.)
// ToDo: Add tests for complex queries
// ToDo: Add commit tests (using separate sessions for create and retrieve) - Put them in a separate class?
// ToDo: Come up with some negative tests (null Note, etc.)
// ToDo: Add tests for verifying managed entities (dbSession.getManagedEntities())
//       Verify there is at least one managed entity?
