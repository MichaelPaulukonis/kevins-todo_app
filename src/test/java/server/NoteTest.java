package server;

import server.element.*;
import server.note.*;
import server.categories.*;
import java.net.MalformedURLException;
import java.util.NoSuchElementException;

import static server.NoteTestHelper.*;
import static server.note.Contact.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

// Todo: Change the name to make this a unit test (and add to a test suite)?
public class NoteTest {
    // ToDo: MP: Why are these test objects static?
    private static final int defaultElementCount = 5;
    private static Note noteWithRandomElements;
    private static Note emptyTestNote;
    private static Note subNote1;
    private static Note subNote2;
    private static final Entity artist = new Entity("Grand Funk", null, Entity.EntityType.ARTIST);

    @BeforeAll
    public static void setUpTests() throws MalformedURLException {
        emptyTestNote = createGenericTestNote();
        noteWithRandomElements = createGenericTestNoteWithRandomElements(defaultElementCount);
        subNote1 = createGenericTestNote("Test sub-note");
        subNote1.addChildNote(new Product("I'm Your Captain", null, artist).setType(Category.of(MediaType.SONG)));
        subNote2 = createGenericTestNote("Test sub-note");
        subNote2.addChildNote(new Link("Google", "http://facebook.com"));
    }

    @AfterEach
    public void testCleanup() {
        clearChildNotes(emptyTestNote);
        noteWithRandomElements = createGenericTestNoteWithRandomElements(defaultElementCount);
    }

    // ToDo: Add a simple test for just one Note?
    // ToDo: Add test for each Note type

    // ToDo: Combine null/empty name tests
    @Test
    public void noteUsesDescriptionForNameIfNameIsNull() {
        String description = "Description for note with no name";
        Note note = new Note(null, description);
        assertEquals(description, note.getName(), "Null Note name is set to description");
        // ToDo: Not sure how the case below should behave
        // assertEquals("Description for note with no name", note.getDescription(), "Note description");
    }

    @Test
    public void noteUsesDescriptionForNameIfNameIsEmpty() {
        String description = "Description for note with empty name";
        Note note = new Note("", description);
        assertEquals(description, note.getName(), "Empty Note name is set to description");
        // ToDo: Not sure how the case below should behave
        // assertEquals("Description for note with no name", note.getDescription(), "Note description");
    }

    @Test
    public void noteCreationFailsIfBothNameAndDescriptionAreUnset() {
        String[] fields = { null, "" };
        for (String name : fields) {
            for (String description : fields) {
                assertThrows(IllegalArgumentException.class, () -> new Note(name, description));
            }
        }
    }

    // ToDo: Add null/empty tests for setName() (and setDescription()?)

    @Test
    public void ifFullNameIsMissingContactUsesFirstAndLastName() {
        Contact contact = newPerson("First", "Last");
        assertEquals("First Last", contact.getName(), "Contact name is first + last");
    }

    @Test
    public void contactCreationFailsIfAllNamesAreNullOrEmpty() {
        String[] names = { null, "" };
        for (String firstName : names) {
            for (String lastName : names) {
                assertThrows(IllegalArgumentException.class, () -> newPerson(firstName, lastName));
            }
        }
    }

    // ToDo: Add tests for setContactName()
    // ToDo: Add tests for phone numbers and addresses

    @Test
    public void contactCreationFailsIfTypeIsNull() {
        assertThrows(AssertionError.class, () -> new Contact("Name", null));
    }

    @Test
    // ToDo: Need separate tests for ordering
    public void createNoteWithEachElementTypeAndVerifyResult() throws MalformedURLException {
        emptyTestNote.addChildNote(newPerson("First1", "Last1").addPhoneNumber("234-5678", Phone.PhoneType.MOBILE)
                .addAddress(new Address().setAddress1("123 4th St.").setCity("Menlo Park")));
        emptyTestNote.addChildNote(new Product("Blind Alley", null,
                new Entity("Fanny", null, Entity.EntityType.ARTIST)).setType(Category.of(MediaType.SONG)));
        // emptyTestNote.addElement(new Price(5.00));
        emptyTestNote.addChildNote(new Link("Google", "http://google.com"));
        emptyTestNote.addChildNote(new ScheduleItem("Deadline", null, "2020-06-12 00:00:00 AM"));
        emptyTestNote.addChildNote(new Note("Note1"));
        assertEquals(5, emptyTestNote.getChildNotes().size(), "Number of child notes added");

        // System.out.println(emptyTestNote);
        // ToDo: Use fluent assertions (no need for Hamcrest matchers)?
        // assertThat("Expected Contact type at position 0", testNote.getElement(0));
        // ToDo: Rethink the the search tests (the ordering of the child-notes is less important now)
        // ToDo: MP points out that this implicitly tests the items ORDER as well (have separate tests for that)
        String[] noteTypes = new String[] {"Contact", "Product", "Link", "ScheduleItem", "Note"};
        for (int i = 0; i < emptyTestNote.getChildNotes().size(); i++) {
            assertEquals(noteTypes[i], emptyTestNote.getChildNotes().get(i).getClass().getSimpleName(),
                    "Expected type at position " + i);
        }
        clearChildNotes(emptyTestNote);
    }

    // ToDo: How is this different from the previous test? Do we need both (if I keep it add verification)?
    @Test
    public void createNoteWithTwoSubNotes() {
        emptyTestNote.addChildNote(subNote1);
        emptyTestNote.addChildNote(subNote2);

        // System.out.println(emptyTestNote);
        assertEquals(2, emptyTestNote.getChildNotes().size(), "Number of child notes");
        // ToDo: Rethink the search tests
/*
        assertSame(emptyTestNote.getElement(0), emptyTestNote.getElement(Note.class, 0),
                "Expected Contact type at position 0");
        assertSame(emptyTestNote.getElement(1), emptyTestNote.getElement(Note.class, 1),
                "Expected Contact type at position 0");
*/
    }

    @Test
    // ToDo: MP suggests callingClearRemovesAllElements() (i.e. we're testing the clear/delete function)
    public void removeAllElements() {
        // System.out.println(noteWithRandomElements);
        assertEquals(defaultElementCount, noteWithRandomElements.getChildNotes().size(), "Number of Note elements");
        clearChildNotes(noteWithRandomElements);
        assertEquals(0, noteWithRandomElements.getChildNotes().size(), "Number of Note elements");
    }

    @Test
    public void removeOneOfMultipleElements() {
        NoteBase note = noteWithRandomElements.getChildNotes().getFirst();
        noteWithRandomElements.removeChildNote(note);
        // This test has randomly failed here:
        assertFalse(noteWithRandomElements.getChildNotes().contains(note),
                "Note contains element '" + note.getName() + "'");
        assertEquals(defaultElementCount - 1, noteWithRandomElements.getChildNotes().size(),
                "Number of note elements after deletion");
    }

    // ToDo: Validate elementLocator after add and remove (make it protected?)

    @Test
    public void addElementAtASpecifiedLocation() {
        NoteBase testNoteElement = NoteTestHelper.getRandomNoteElement();
        int elementIndex = 3;
        noteWithRandomElements.addChildNote(testNoteElement, elementIndex);
        assertEquals(defaultElementCount + 1, noteWithRandomElements.getChildNotes().size(), "Number of Note elements");
        assertSame(testNoteElement, noteWithRandomElements.getChildNotes().get(elementIndex),
                "Expected note element '" + testNoteElement.getName() + "' at index " + elementIndex);
    }

    @Test
    // ToDo: Test this on a note with multiple sub-notes of different types (e.g. noteWithRandomElements)
    // ToDo: MP suggests gettingNonExistentNoteTypeThrowsError()
    public void tryGettingInvalidClassTypeFromElementList() {
        assertEquals(0, emptyTestNote.getChildNotes().size(), "Number of child notes in emptyTestNote");
        Exception ex = assertThrows(NoSuchElementException.class, () -> emptyTestNote.getChildNotes().getFirst());
        assertNull(ex.getMessage(), "For some reason the exception message is null");
    }

    @Test
    public void tryGettingInvalidElementIndexFromElementList() throws MalformedURLException {
        emptyTestNote.addChildNote(new Link("Google", "http://facebook.com"));
        // ToDo: Rethink the search tests
        // Exception ex = assertThrows(RuntimeException.class, () -> emptyTestNote.getElement(Link.class, 1));
        // System.out.println("Exception message: " + ex.getMessage());
        String expectedMessage = "Invalid index (1) for list of Link elements (size 1)";
        // assertEquals(expectedMessage, ex.getMessage(), "Exception message equals " + expectedMessage);
    }

    // ToDo: Test adding and removing more than one of the same kind of element
    // ToDo: Test adding and removing tags
    // ToDo: Test adding and removing categories (coupled with main program)?
    // ToDo: Test adding tags and categories?
    // ToDo: More negative tests?
    // ToDo: Test adding duplicate notes?
    // ToDo: Test adding a Link element with an invalid URL
}
