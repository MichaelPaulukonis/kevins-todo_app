package server.database;

import org.hibernate.*;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.mapper.orm.Search;
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
        // Highly recommended for debugging your first test (ToDo: put under a DEBUG flag?)
        // config.setProperty("hibernate.show_sql", "true");
        // config.setProperty("hibernate.format_sql", "true");
        config.setProperty("logging.level.org.hibernate.search.query", "DEBUG");
        config.setProperty("logging.level.org.hibernate.search.backend.lucene.query", "DEBUG");
        trySettingConfigFromEnvironment(config, isTestDB);
        if (isTestDB) {
            // ToDo: Confirm we are NOT pointing at the production DB here!
            // Create and drop the DB for each test run
            config.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            // Cache the Lucene search results in memory instead of on disk
            // ToDo: Might not need this
            config.setProperty("hibernate.search.backend.directory.type", "local-heap");
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

    public static Set<Category> retrieveCategoriesFromDB(Session session) {
        return session.createQuery("SELECT DISTINCT c FROM NoteBase n JOIN n.categories c", Category.class)
                .getResultStream().collect(Collectors.toSet());
    }

    // ToDo: Rewrite categoryExists()

    public static void persistObjectToDB(Session session, Object obj) {
        session.persist(obj);
        session.flush();
    }

    public static void commitObjectToDB(Object obj) {
        trxMgr.runTransaction(session -> {
            session.persist(obj);
        });
    }

    public static Set<NoteBase> retrieveNotesForCategory(Session session, Category cat) {
        return session.createQuery("SELECT N FROM NoteBase N JOIN N.categories c WHERE c.categoryName = :name", NoteBase.class)
                .setParameter("name", cat.getName()).getResultStream().collect(Collectors.toSet());
    }

    public static List<? extends NoteBase> retrieveNotes(Session session) {
        return session.createQuery("SELECT n FROM NoteBase n", NoteBase.class).getResultList();
    }

    @SuppressWarnings("unchecked")
    public static <T extends NoteBase> T retrieveNoteFromDB(Session session, T note) {
        Class <T> noteClass = (Class<T>) Hibernate.getClass(note);
        return session.find(noteClass, note.getId());
    }

    // ToDo: If I'm desperate I could just load the Notes from the DB and search in memory
    // ToDo: Can I use the NoteBase hasCategory(), hasAllCategories(), and hasAnyCategory() methods here?
    // ToDo: The name search is currently case-sensitive (make it case-insensitive?)
    // Note: getNoteClass() automatically includes subclasses in the search result due to the SINGLE_TABLE model
    public static List<? extends NoteBase> findMatchingNotesInDB(Session session, SearchCriteria criteria) {
        // Extracted query variable for debugging and added .extension(LuceneExtension.get())
        var searchQuery = Search.session(session).search(criteria.getNoteClass()).extension(LuceneExtension.get()).where(
                factory -> factory.bool().with(predicate -> {
            if (isSet(criteria.getNameMatch())) {
                var namePredicate = criteria.isPatternMatch()
                        ? factory.regexp().field("name").matching(criteria.getNamePattern())
                        : factory.wildcard().field("name").matching("*" + criteria.getNameMatch() + "*");

                predicate.must(namePredicate.toPredicate());
            }
            if (!criteria.getCategories().isEmpty()) {
                if (criteria.isJoinWithAnd()) {
                    for (Category cat : criteria.getCategories()) {
                        predicate.must(factory.match().field("categories.categoryName").matching(cat.getName()));
                    }
                }
                else {
                    System.out.println("Criteria class = " + criteria.getNoteClass());
                    predicate.must(factory.bool().with(outerBool -> {
                        for (Category cat : criteria.getCategories()) {
                            // 2. Spawn a separate ToParentBlockJoinQuery for EACH distinct category
                            // This means: "Find a note with a BOOK child" OR "Find a note with a FILM child"
                            outerBool.should(factory.nested("categories").add(factory.match()
                                    .field("categories.categoryName").matching(cat.getName()).toPredicate()
                            ));
                        }
                        outerBool.minimumShouldMatchNumber(1);
                    }));
                }
            }
            // If name and categories aren't set this just uses class (which defaults to NoteBase)
            if (!isSet(criteria.getNameMatch()) && criteria.getCategories().isEmpty()) {
                predicate.must(factory.matchAll());
            }
        })).toQuery();
        System.out.println("=== GENERATED NATIVE LUCENE QUERY ===");
        System.out.println(searchQuery.queryString());
        System.out.println("=====================================");
        return searchQuery.fetchAllHits();
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
