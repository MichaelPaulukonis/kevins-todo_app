package server;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.*;

import static server.NoteTestHelper.*;

// ToDo: Rename this to DBTestBase?
// ToDo: Add a static testTrxMgr property in this class (don't need it since it's available in NoteTestHleper)?
public class TestBase {
    private static StandardServiceRegistry testServiceRegistry;
    protected static NoteOrganizer main;
    protected static SessionFactory testSessionFactory;
    protected static final String defaultCategoryName = "NewCategory";

    @BeforeAll
    public static void setUpTestSessionFactory() {
        testSessionFactory = getTestSessionFactory();
    }

    @BeforeEach
    public void initialize() throws Exception {
        main = new NoteOrganizer();
        if (testSessionFactory == null) {
            throw new Exception("Failed to create Hibernate SessionFactory");
        }
        testSessionFactory.getSchemaManager().truncateMappedObjects();  // Clean up the DB before each test
    }

    @AfterEach
    public void testCleanup() {
        // ToDo: Do I need to clean up testSessionFactory here (currently done in @AfterAll)?
        resetNoteNumber();
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
}
