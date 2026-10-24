package server;

import server.categories.Category;
import server.categories.MediaType;
import server.element.Address;
import server.element.Phone;
import server.note.*;

import java.time.LocalDateTime;
import java.util.*;

import static server.note.Contact.*;

public class NoteTestHelper {
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

    // ToDo: Replace these methods with a NoteBuilder class?
    //  * Can probably delete these next four methods (but need a way to mass-produce test note data)
    //  * Will this work with polymorphism? Need a factory instead?
    //  * Define a constructor taking required parameters (title)
    //  * Define methods for adding other parameters
    //  * Define a build() method that calls the constructor and returns the note object
    //    Or maybe a factory class?
    protected static Note createGenericTestNoteWithRandomElements() {
        return createGenericTestNoteWithRandomElements(null, defaultElementCount);
    }

    protected static Note createGenericTestNoteWithRandomElements(String baseName) {
        return createGenericTestNoteWithRandomElements(baseName, defaultElementCount);
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

    protected static NoteBase addTestNote(NoteOrganizer main) {
        NoteBase testNote = createGenericTestNote();
        main.addNote(testNote);
        return testNote;
    }

    protected static List<NoteBase> addTestNotes(int noteCount, NoteOrganizer main) {
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

    public static int numberOfNotesForCategory(String cat, NoteOrganizer main) {
        return main.getNotesForCategory(cat).size();
    }

    public static void addNotesWithCategory(int numberOfNotes, String category, NoteOrganizer main) {
        for (int i = 0; i < numberOfNotes; i++) {
            // ToDo: Use index in note name?
            addNoteWithCategory(category, main);
        }
    }

    public static void addNoteWithCategory(String cat, NoteOrganizer main) {
        String newCat = cat == null ? MediaType.get().getRandomTag().toString() : cat;
        NoteBase newNote = NoteTestHelper.createGenericTestNote().addCategory(newCat);
        // System.out.println(newNote);
        main.addNote(newNote);
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
    public static Set<String> createNotesWithMultipleCategories(NoteOrganizer main) {
        int offset = main.getNotes().size();
        addTestNotes(partitionCount*3, main);
        for (int i = 0; i < partitionCount*3; i++) {
            NoteBase note = main.getNotes().get(i + offset);
            if (i < partitionCount*2) {
                main.addNoteToCategory(MediaType.BOOK.name(), note);
            }
            if (i >= partitionCount) {
                main.addNoteToCategory(MediaType.FILM.name(), note);
            }
        }
        return Set.of(MediaType.BOOK.name(), MediaType.FILM.name());
    }
}
