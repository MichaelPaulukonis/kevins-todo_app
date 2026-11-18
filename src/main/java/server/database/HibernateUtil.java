package server.database;

import jakarta.persistence.criteria.*;
import org.hibernate.*;
import org.jspecify.annotations.NonNull;
import server.SearchCriteria;
import server.categories.Category;
import server.note.*;
import org.hibernate.boot.*;
import org.hibernate.boot.registry.*;
import org.hibernate.cfg.Configuration;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static server.NoteUtils.*;

public class HibernateUtil {
    protected static TransactionManager trxMgr;

    public static SessionFactory getSessionFactory() { return getSessionFactory(false); }
    public static SessionFactory getTestSessionFactory() { return getSessionFactory(true); }

    private static SessionFactory getSessionFactory(boolean isTest) {
        MetadataSources sources = new MetadataSources(getServiceRegistry(isTest));
        // All @Entity classes must be included here (looks like I don't need to include @Embeddable classes)
        for (Class<?> entity : new Class<?>[]{ NoteBase.class, Note.class, Entity.class, Contact.class,
                Link.class, Product.class, ScheduleItem.class }) {
            sources.addAnnotatedClass(entity);
        }
        Metadata metadata = sources.getMetadataBuilder().build();
        SessionFactory factory = metadata.buildSessionFactory();
        trxMgr = new TransactionManager(factory);
        return factory;
    }

    private static @NonNull StandardServiceRegistry getServiceRegistry(boolean isTestDB) {
        try {
            return new StandardServiceRegistryBuilder()
                    .applySettings(getConfiguration(isTestDB).getProperties()).build();
        }
        catch (Exception ex) {
            LOG.error("Failed to create Hibernate service registry", ex);
            throw ex;
        }
    }

    private static @NonNull Configuration getConfiguration(boolean isTestDB) {
        Configuration config = new Configuration();    // Looks for hibernate.properties by default
        // Currently we assume a PostgreSQL DB
        config.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        config.setProperty("hibernate.current_session_context_class", "thread");
        boolean sqlDebug = false;    // ToDo: Make this a configuration parameter
        if (sqlDebug) {
            config.setProperty("hibernate.show_sql", "true");
            config.setProperty("hibernate.format_sql", "true");
            config.setProperty("logging.level.org.hibernate.orm.jdbc.bind", "trace");
        }
        trySettingConfigFromEnvironment(config, isTestDB);
        if (isTestDB) {
            // ToDo: Confirm we are NOT pointing at the production DB here!
            //  - confirm that DB name includes "test"?
            // This is required to avoid lazily initialization errors in the test code
            config.setProperty("hibernate.enable_lazy_load_no_trans", "true");
            // Create and drop the DB for each test run
            config.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        }
        else {
            String hbm2ddlAuto = config.getProperty("hibernate.hbm2ddl.auto");
            if (isSet(hbm2ddlAuto) && hbm2ddlAuto.equalsIgnoreCase("create-drop")) {
                throw new IllegalStateException("Cannot allow hibernate.hbm2ddl.auto=create-drop on production DB!");
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

    // The methods in this class persist the objects to the DB, and the TransactionManager handles commit/rollback.
    public static void commitNoteWithCategories(NoteBase note, Set<Category> categories) {
        trxMgr.runTransaction(session -> {
            if (!categories.isEmpty()) {
                note.addCategories(categories);
            }
            session.persist(note);
        });
    }

    public static Set<Category> retrieveCategoriesFromDB() {
        return trxMgr.runTransaction(session -> {
            return session.createQuery("SELECT DISTINCT c FROM NoteBase n JOIN n.categories c", Category.class)
                    .getResultStream().collect(Collectors.toSet());
        });
    }

    // ToDo: Add query for returning ScheduleItems in timestamp order
    // public List<ScheduleItem> retrieveScheduleFromDB() { }

    // ToDo: Rewrite categoryExists()

    public static void persistObjectToDB(Session session, Object obj) {
        session.persist(obj);
        session.flush();
    }

    public static void commitObjectToDB(Object obj) {
        trxMgr.runTransaction(session -> { session.persist(obj); });
    }

    public static Set<NoteBase> retrieveNotesForCategory(Category cat) {
        return trxMgr.runTransaction(session -> {
            return session.createQuery("SELECT N FROM NoteBase N JOIN N.categories c WHERE c.categoryName = :name", NoteBase.class)
                    .setParameter("name", cat.getName()).getResultStream().collect(Collectors.toSet());
        });
    }

    public static List<? extends NoteBase> retrieveNotes() {
        return trxMgr.runTransaction(session -> {
            return session.createQuery("SELECT n FROM NoteBase n", NoteBase.class).getResultList();
        });
    }

    // ToDo: Must give runTransaction() methods different names to avoid the two return calls
    public static <T extends NoteBase> T retrieveNoteFromDB(T note) {
        return trxMgr.runTransaction(session -> {
            return session.find(Hibernate.getClass(note), note.getId());
        });
    }

    // Note: getNoteClass() automatically includes subclasses in the search result due to the SINGLE_TABLE model
    public static <T extends NoteBase> List<T> findMatchingNotesInDB(SearchCriteria criteria) {
        return trxMgr.runTransaction(session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            @SuppressWarnings("unchecked")
            Class<T> targetClass = (Class<T>) criteria.getNoteClass();

            CriteriaQuery<T> cq = cb.createQuery(targetClass);
            Root<T> root = cq.from(targetClass);
            List<Predicate> predicates = new ArrayList<>();

            if (isSet(criteria.getNameMatch())) {    // 1. Name filter (Substring or Regex)
                if (criteria.isPatternMatch()) {
                    // Uses database regex function (syntax varies slightly by DB, e.g., REGEXP_LIKE in Oracle/MySQL/H2/Postgres)
                    predicates.add(cb.isTrue(
                            cb.function("REGEXP_LIKE", Boolean.class, root.get("name"), cb.literal(criteria.getNamePattern()))
                    ));
                } else {
                    // Standard case-insensitive LIKE '%match%'
                    predicates.add(cb.like(cb.lower(root.get("name")), criteria.getSQLNameMatch()));
                }
            }

            // 2. Category filter (AND vs OR)
            if (criteria.hasCategories()) {
                if (criteria.isJoinWithAnd()) {
                    for (String catName : criteria.getCategoryNames()) {   // Must match ALL specified categories
                        Join<T, Category> catJoin = root.join("categories");
                        predicates.add(cb.equal(catJoin.get("categoryName"), catName));
                    }
                } else {
                    // Must match AT LEAST ONE category
                    predicates.add(root.join("categories").get("categoryName").in(criteria.getCategoryNames()));
                }
            }
            // Apply all predicates (if empty, it automatically matches all rows of targetClass)
            cq.select(root).distinct(true).where(predicates.toArray(new Predicate[0]));
            return session.createQuery(cq).getResultList();
        });
    }

    // Helper class for overriding equals (required by Hibernate)
    public static boolean equalsHelper(Object self, Object other) {
        if (self == other) return true;
        if (other == null) return false;

        Class<?> selfClass = Hibernate.getClass(self);
        Class<?> objClass = Hibernate.getClass(other);
        if (selfClass != objClass) return false;

        try {
            Object selfId = selfClass.getMethod("getId").invoke(self);
            Object objId = objClass.getMethod("getId").invoke(other);
            return selfId != null && selfId.equals(objId);
        } catch (Exception e) {
            LOG.warn("Failed to locate getId() method on one or both arguments to equals()");
            return false;
        }
    }
}
