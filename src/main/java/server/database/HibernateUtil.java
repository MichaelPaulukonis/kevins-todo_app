package server.database;

import org.hibernate.*;
import org.jspecify.annotations.NonNull;
import server.note.*;
import org.hibernate.boot.*;
import org.hibernate.boot.registry.*;
import org.hibernate.cfg.Configuration;

import java.io.InputStream;
import java.util.Properties;

import static server.NoteUtils.*;

public class HibernateUtil {

    // ToDo: Define getTestServiceRegistry()?
    public static @NonNull StandardServiceRegistry getServiceRegistry(boolean isTestDB) throws Exception {
        try {
            return new StandardServiceRegistryBuilder()
                    .applySettings(getConfiguration(isTestDB).getProperties()).build();
        }
        catch (Exception ex) {
            LOG.error("Failed to create Hibernate service registry", ex);
            throw ex;
        }
    }

    public static SessionFactory getSessionFactory(StandardServiceRegistry registry) {
        MetadataSources sources = new MetadataSources(registry);
        // All @Entity classes must be included here (looks like I don't need to include @Embeddable classes)
        for (Class<?> entity : new Class<?>[]{ NoteBase.class, Note.class, Entity.class, Contact.class, Link.class,
                Product.class, ScheduleItem.class}) {
            sources.addAnnotatedClass(entity);
        }
        return sources.buildMetadata().buildSessionFactory();
    }

    private static @NonNull Configuration getConfiguration(boolean isTestDB) throws Exception {
        Configuration config = new Configuration();    // Looks for hibernate.properties by default
        // Currently we assume a PostgreSQL DB
        config.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        // Highly recommended for debugging your first test (ToDo: put under a DEBUG flag?)
        config.setProperty("hibernate.show_sql", "true");
        config.setProperty("hibernate.format_sql", "true");
        trySettingConfigFromEnvironment(config, isTestDB);
        if (isTestDB) {
            config.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            // ToDo: Confirm we are NOT pointing at the production DB if create-drop is set!
        }
        else {
            String hbm2ddlAuto = System.getProperty("hibernate.hbm2ddl.auto");
            if (isSet(hbm2ddlAuto) && hbm2ddlAuto.equalsIgnoreCase("create-drop")) {
                throw new Exception("Cannot allow hibernate.hbm2ddl.auto=create-drop for production DB!");
            }
        }
        LOG.info("Hibernate: Connecting to database using connection string {}",
                config.getProperty("hibernate.connection.url"));
        return config;
    }

    private static void trySettingConfigFromEnvironment(Configuration config, boolean isTestDB) {
        // ToDo: Should environment variables override config settings?
        if (setConfigFromLocalResourceFile(config)) {
            return;
        }
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        if (isSet(username) && isSet(password)) {
            config.setCredentials(username, password);
            LOG.info("Set DB_USERNAME and DB_PASSWORD from environment variables");
        }
        String dbPort = System.getenv("DB_PORT");
        String database = System.getenv(isTestDB ? "DB_TEST_NAME" : "DB_PROD_NAME");
        if (isSet(dbPort) && isSet(database)) {
            String jdbcUrl = "jdbc:postgresql://localhost:" + dbPort + "/" + database;
            config.setJdbcUrl(jdbcUrl);
            // ToDo: Am I logging this twice?
            LOG.info("Set the JDBC URL to {}", jdbcUrl);
        }
    }

    private static boolean setConfigFromLocalResourceFile(Configuration config) {
        String hibernateConfigFile = System.getenv("HIBERNATE_CONFIG_FILE");
        if (!isSet(hibernateConfigFile)) {
            return false;
        }
        try (InputStream input = HibernateUtil.class.getClassLoader().getResourceAsStream(hibernateConfigFile)) {
            if (input == null) {
                return false;
            }
            LOG.info("Loading Hibernate properties from config file {}", hibernateConfigFile);
            Properties configProperties = new Properties();
            configProperties.load(input);
            config.addProperties(configProperties);
            return true;
        }
        catch (Exception ex) {
            LOG.error("Failed to load Hibernate config file '{}'", hibernateConfigFile, ex);
        }
        return false;
    }

    // ToDo: This should be moved into a file under the test package
    @SuppressWarnings("unchecked")
    public static <T extends NoteBase> T saveAndRetrieveNote(Session testSession, NoteBase noteType) {
        try {
            // ToDo: We are assuming that a transaction has been opened here (confirm that?)
            testSession.persist(noteType);
            testSession.flush();    // Forces a write to the DB
            testSession.clear();    // Empties the cache to force retrieval from the DB
            Class<T> noteClass = (Class<T>) Hibernate.getClass(noteType);
            T fetchedNote = testSession.find(noteClass, noteType.getId());
            assert fetchedNote != null : "Failed to fetch " + noteType.getClass() + " with ID " + noteType.getId();
            return fetchedNote;
        }
        catch (Exception ex) {
            if (testSession.getTransaction() != null && testSession.getTransaction().isActive()) {
                testSession.getTransaction().rollback();
            }
            throw ex;  // Fail test
        }
    }
}
