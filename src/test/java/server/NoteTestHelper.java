package server;

import server.categories.MediaType;
import server.element.*;

import java.util.*;

public class NoteTestHelper {
    private static final Random rand = new Random();
    private static final int defaultElementCount = 5;
    private static int noteNumber = 0;

    // ======================== Note Test Helper Methods ======================
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
    private static final int numberOfElementTypes = 6;
    private static int contactCount = 0, songCount = 0, linkCount = 0, priceCount = 0, eventCount = 0, noteCount = 0;
    public static NoteElement<?> getRandomNoteElement() {
        int elementTypeIndex = rand.nextInt(numberOfElementTypes);
        switch (elementTypeIndex) {
            case 0:
                contactCount++;
                return new Contact().setName("Contact" + contactCount).setPhoneNumber("345-5679")
                        .setAddress1("222 2nd St.").setCity("Menlo Park");
            case 1:
                songCount++;
                return new MediaItem("Who Song #" + songCount, "The Who", MediaType.SONG);
            case 2:
                linkCount++;
                return new Link("Web Site #" + linkCount, "http://website" + linkCount + ".com");
            case 3:
                priceCount++;
                return new Price("Item" + priceCount, null, 10.00);
            case 4:
                eventCount++;
                return new EventInfo("Event #" + eventCount, null, "2020-07-03 00:00:00");
            case 5:
                noteCount++;
                return new Note("Note Element #" + noteCount);
            default:
                throw new RuntimeException("Illegal note element index: " + elementTypeIndex
                        + " (maximum is " + (numberOfElementTypes - 1) + ")");
        }
    }

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
            testNote.addElement(getRandomNoteElement());
        }
        return testNote;
    }
}
