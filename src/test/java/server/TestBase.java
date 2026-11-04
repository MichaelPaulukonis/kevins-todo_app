package server;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.*;
import server.database.HibernateUtil;
import server.note.NoteBase;

import java.util.function.Consumer;

import static server.NoteTestHelper.*;
import static server.database.HibernateUtil.*;

// ToDo: Rename this to DBTestBase?
public class TestBase {
    private static StandardServiceRegistry testServiceRegistry;
    protected static NoteOrganizer main;
    protected static SessionFactory testSessionFactory;
    protected static Session testSession;
    protected static final String defaultCategoryName = "NewCategory";

    @BeforeAll
    public static void setUpTestSessionFactory() {
        // testServiceRegistry = getServiceRegistry(true);
        testSessionFactory = getTestSessionFactory();
    }

    @BeforeEach
    public void initialize() throws Exception {
        main = new NoteOrganizer();
        if (testSessionFactory == null) {
            throw new Exception("Failed to create Hibernate SessionFactory");
        }
        testSessionFactory.getSchemaManager().truncateMappedObjects();  // Clean up the DB before each test
        testSession = testSessionFactory.openSession();
        retrieveNotes(testSession);
        // ToDo: Don't call beginTransaction() here?
        testSession.beginTransaction();    // Used strictly for queries
        // System.out.println("Number of notes should be 0 at the start of the test: " + retrieveNotes(testSession).size());
    }

    @AfterEach
    public void testCleanup() {
        if (testSession != null && testSession.isOpen()) {
            if (testSession.getTransaction().isActive()) {
                testSession.getTransaction().rollback();   // Rollback transaction so test DB stays clean
            }
            testSession.close();
        }
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

    // ToDo: Figure out the production version later
    // ToDo: Define getSessionFactory() and getTransaction() in NoteOrganizer?
    //  - Have getSessionFactory() call getCurrentSession()
    protected static void runTestTransaction(Consumer<Session> testCode) {
        Session testSession = testSessionFactory.getCurrentSession();
        Transaction trx = testSession.beginTransaction();
        try {
            testCode.accept(testSession);
            trx.commit();
        }
        catch (Exception ex) {
            if (trx.isActive()) {
                trx.rollback();
            }
            throw ex;
        }
    }
}
