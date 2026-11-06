package server;

import org.hibernate.Session;
import org.junit.jupiter.api.Assertions;
import server.categories.Category;
import server.categories.MediaType;
import server.database.HibernateUtil;
import server.element.Address;
import server.element.Phone;
import server.note.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static server.note.Contact.*;

public class NoteTestHelper extends HibernateUtil {
    private static final Random rand = new Random();
    private static final int defaultElementCount = 5;
    private static int noteNumber = 0;


    // ======================== Note Test Helper Methods ======================
    // ToDo: Will static variables work if the tests are run in parallel?
    public static void resetNoteNumber() {
        noteNumber = 0;
    }

    protected static Note createGenericTestNote() {
        return createGenericTestNote(null);
    }

    protected static Note createGenericTestNote(String baseName) {
        noteNumber++;
        // ToDo: MP: Use a GUID instead of adding noteNumber?
        String name = (baseName == null ? "Test Note" : baseName) + " #" + noteNumber;
        return new Note(name, "Description for " + name);
    }

    // ToDo: Create test API that can add a specified list of Element types?
    private static final int numberOfElementTypes = 5;
    private static int contactCount = 0, songCount = 0, linkCount = 0, eventCount = 0, noteCount = 0;

    // ToDo: Make sure all Note-type objects are covered
    // ToDo: Rename to getRandomNoteType()?
    public static NoteBase getRandomNoteElement() {
        int elementTypeIndex = rand.nextInt(numberOfElementTypes);
        return switch (elementTypeIndex) {
            case 0 -> {
                contactCount++;
                yield newPerson("First", "Contact" + contactCount).addPhoneNumber("345-5679", Phone.PhoneType.WORK)
                        .addAddress(new Address().setAddress1("222 2nd St.").setCity("Menlo Park"));
            }
            case 1 -> {
                songCount++;
                Entity owner = new Entity("The Who", null, Entity.EntityType.ARTIST);
                yield new Product("Who Song #" + songCount).setOwner(owner)
                        .setType(Category.of(MediaType.SONG)).setPrice(5.99);
            }
            case 2 -> {
                linkCount++;
                yield new Link("Web Site #" + linkCount, "http://website" + linkCount + ".com");
            }
            case 3 -> {
                eventCount++;
                yield new ScheduleItem("Event #" + eventCount, null, "2020-07-03 00:00:00 AM");
            }
            case 4 -> {
                noteCount++;
                yield new Note("Note #" + noteCount);
            }
            default -> throw new RuntimeException("Illegal note element index: " + elementTypeIndex
                    + " (maximum is " + (numberOfElementTypes - 1) + ")");
        };
    }

    protected static Note createGenericTestNoteWithRandomElements(int numberOfElements) {
        return createGenericTestNoteWithRandomElements(null, numberOfElements);
    }

    protected static Note createGenericTestNoteWithRandomElements(String baseName, int numberOfElements) {
        Note testNote = createGenericTestNote(baseName);
        for (int i = 0; i < numberOfElements; i++) {
            testNote.addChildNote(getRandomNoteElement());
        }
        return testNote;
    }

    protected static void clearChildNotes(Note note) {
        note.getChildNotes().clear();
    }

    protected static List<NoteBase> createTestNotes(int noteCount) {
        List<NoteBase> newNotes = new ArrayList<>(noteCount);
        for (int i = 0; i < noteCount; i++) {
            newNotes.add(createGenericTestNote());
        }
        return newNotes;
    }

    protected static NoteBase addTestNote(NoteOrganizer main) {
        NoteBase testNote = createGenericTestNote();
        main.addNote(testNote);
        return testNote;
    }

    protected static List<NoteBase> persistNewTestNotes(int noteCount, NoteOrganizer main) {
        List<NoteBase> newNotes = new ArrayList<>(noteCount);
        // ToDo: Use index in note name?
        for (int i = 0; i < noteCount; i++) {
            newNotes.add(createGenericTestNote());
        }
        main.addNotes(newNotes);
        return newNotes;
    }

    public static Set<String> getCategories(NoteOrganizer main) {
        return main.getCategories();
    }

    public static int numberOfCategories(NoteOrganizer main) {
        return getCategories(main).size();
    }

    // Assigns random categories if categories is null
    public static void commitNewNotesWithCategories(int numberOfNotes, Set<Category> categories) {
        for (int i = 0; i < numberOfNotes; i++) {
            Set<Category> testCats = categories == null ?
                    Set.of(Category.of(MediaType.get().getRandomTag())) : categories;
            commitNoteWithCategories(NoteTestHelper.createGenericTestNote(), testCats);
        }
    }

    // Add 4 ScheduleItem notes in non-date order (should be automatically sorted)
    public static void createScheduleItemTestBed(NoteOrganizer main) {
        main.addNote(new ScheduleItem("Event 4", null, "2020-07-03 10:00:01 AM"));
        main.addNote(new ScheduleItem("Event 1", null, "2020-07-01 09:00:00 PM"));
        main.addNote(new ScheduleItem("Event 3", null, "2020-07-03 10:00:00 AM"));
        main.addNote(new ScheduleItem("Event 2", null, "2020-07-02 05:00:00 PM"));
        main.addNote(new ScheduleItem("Event 5", null, "2020-07-03 10:00:01 AM"));
    }

    // ToDo: Add negative test case for this (ScheduleItem list out of order)?
    public static boolean verifyScheduleItemOrder(List<ScheduleItem> schedule) {
        LocalDateTime lastDate = LocalDateTime.MAX;
        for (ScheduleItem item : schedule) {
            if (lastDate.isBefore(item.getDate())) {
                return false;
            }
            lastDate = item.getDate();
        }
        return true;
    }

    // ToDo: Document this method (I think the idea is to return notes with BOOK, FILM, both, or no tags)
    static final int partitionCount = 4;
    // Creates notes for testing AND and OR category queries
    public static Set<Category> createNotesWithMultipleCategories(NoteOrganizer main) {
        int offset = main.getNotes().size();
        persistNewTestNotes(partitionCount*3, main);
        for (int i = 0; i < partitionCount*3; i++) {
            NoteBase note = main.getNotes().get(i + offset);
            if (i < partitionCount*2) {
                main.addNoteToCategory(MediaType.BOOK.name(), note);
            }
            if (i >= partitionCount) {
                main.addNoteToCategory(MediaType.FILM.name(), note);
            }
        }
        return Set.of(Category.of(MediaType.BOOK.name()), Category.of(MediaType.FILM.name()));
    }

    /**
     * This creates and commits the categories to the DB, adds them to the notes, and then commits the notes.
     *
     * @return
     */
    public static Set<Category> commitNotesWithMultipleCategories() {
        AtomicReference<List<Category>> categoriesRef = new AtomicReference<>();
        trxMgr.runTransaction(testSession -> {
            Set<Category> categories = Set.of(Category.of(MediaType.BOOK), Category.of(MediaType.FILM));
            List<Category> fetchedCategories = persistCategoriesIfNew(testSession, categories)
                    .stream().toList();
            categoriesRef.set(fetchedCategories);
            List<? extends NoteBase> notes = createTestNotes(partitionCount*3);
            for (int i = 0; i < partitionCount*3; i++) {
                NoteBase note = notes.get(i);
                if (i < partitionCount * 2) {
                    note.addCategory(fetchedCategories.getFirst());
                }
                if (i >= partitionCount) {
                    note.addCategory(fetchedCategories.get(1));
                }
                testSession.persist(note);
            }
        });
        return Set.copyOf(categoriesRef.get());
    }

    public static NoteBase getSimpleNote() {
        return new NoteBase("Simple Note 1", "Simple NoteBase object");
    }

    public static Note getTestNote() {
        return new Note("Note#" + noteNumber, "Testing note #" + noteNumber++);
    }

    private static int personNumber = 1;
    protected static Contact getTestPerson() {
        return Contact.newPerson("FirstName" + personNumber, "LastName" + personNumber++);
    }

    private static int businessNumber = 1;
    public static Contact getTestBusiness() {
        return Contact.newBusiness("Business#" + businessNumber, "Description for business #" + businessNumber++);
    }

    public static Link getGoogleLink() {
        return new Link("Google", null, "http://google.com");
    }

    public static Entity getTestArtist() {
        return new Entity("Grand Funk", null, Entity.EntityType.ARTIST);
    }

    private static int productNumber = 1;
    public static Product getTestProduct() {
        return new Product("Product#" + productNumber++)
                .setOwner(getTestArtist()).setPrice(5.99).setType(MediaType.SONG);
    }

    private static int eventNumber = 1;
    public static ScheduleItem getTestScheduleItem() {
        return new ScheduleItem("Event#" + eventNumber, null, LocalDateTime.now().plusDays(eventNumber++));
    }

    public static <T extends NoteBase> T commitAndRetrieveNote(T note) {
        AtomicReference<T> fetchedNote = new AtomicReference<>();
        trxMgr.runTransaction(testSession -> {
            testSession.persist(note);
            testSession.flush();
            fetchedNote.set(retrieveNoteFromDB(testSession, note));
        });
        assert fetchedNote.get() != null : "Failed to fetch " + note.getClass() + " with ID " + note.getId();
        return fetchedNote.get();
    }
}
