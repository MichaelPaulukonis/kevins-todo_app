package server;

import org.junit.jupiter.api.Test;
import server.database.HibernateUtil;
import org.hibernate.Session;
import static org.junit.jupiter.api.Assertions.*;

// ToDo: Change name to DBUnitTest?
public class DBSanityTest {
    @Test
    public void createDBConnection() {
        Session dbSession = HibernateUtil.getSession();
        // This is a basic session sanity test
        assertFalse(dbSession.getProperties().isEmpty(), "Expected non-empty properties from DB session");
        dbSession.close();
    }
}

// ToDo: Add tests for simple queries
// ToDo: Add tests for verifying managed entities (dbSession.getManagedEntities())
//       Verify there is at least one managed entity?
// ToDo: Add tests for transactions (dbSession.beginTransaction())
// ToDo: Add tests for creating and verifying each entity type (Note, Contact, MediaItem, etc.)
// ToDo: Add tests for CRUD operations on each entity type (Note, Contact, MediaItem, etc.)
