package server;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import server.categories.Category;

import java.util.Set;

import static server.NoteTestHelper.*;

// ToDo: Rename this to DBTestBase?
// ToDo: Add a static testTrxMgr property in this class (don't need it since it's available in NoteTestHleper)?
public class TestBase {
    static SessionFactory testSessionFactory;
    static final String defaultCategoryName = "NewCategory";
    static final Set<Category> testCategories = Set.of(Category.of("Category1"), Category.of("Category2"));

    @BeforeAll
    public static void setUpTestSessionFactory() {
        testSessionFactory = getTestSessionFactory();
    }

    @BeforeEach
    public void initialize() throws Exception {
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
    }
}
