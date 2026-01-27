package server;

import server.element.*;
import server.categories.MediaType;
import java.net.MalformedURLException;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class NoteTest {
    // ToDo: MP: Why are these test objects static?
    private static final int defaultElementCount = 5;
    private static Note noteWithRandomElements;
    private static Note emptyTestNote;
    private static Note subNote1;
    private static Note subNote2;

    @BeforeAll
    public static void setUpTests() throws MalformedURLException {
        emptyTestNote = NoteTestHelper.createGenericTestNote();
        noteWithRandomElements = NoteTestHelper.createGenericTestNoteWithRandomElements(defaultElementCount);
        subNote1 = NoteTestHelper.createGenericTestNote("Test sub-note");
        subNote1.addElement(new MediaItem("I'm Your Captain", "Grand Funk", MediaType.SONG));
        subNote2 = NoteTestHelper.createGenericTestNote("Test sub-note");
        subNote2.addElement(new Link("Google", "http://facebook.com"));
    }

    @AfterEach
    public void testCleanup() {
        emptyTestNote.clear();
        noteWithRandomElements = NoteTestHelper.createGenericTestNoteWithRandomElements(defaultElementCount);
    }

    @Test
    // ToDo: MP points out that this implicitly tests the items ORDER as well
    //  Need separate tests for ordering
    public void createNoteWithEachElementType() throws MalformedURLException {
        emptyTestNote.addElement(new Contact().setName("Contact1")
                .setPhoneNumber("234-5678").setAddress1("123 4th St.").setCity("Menlo Park"));
/*
        testNote.addElement(new Contact("Contact1", "")
                .setPhoneNumber("234-5678").setAddress1("123 4th St.").setCity("Menlo Park"));
*/
        // testNote.addElement(new Contact("Contact1", "234-5678", "123 4th St.", "Menlo Park"));
        emptyTestNote.addElement(new MediaItem("Blind Alley", "Fanny", MediaType.SONG));
        emptyTestNote.addElement(new Price(5.00));
        emptyTestNote.addElement(new Link("Google", "http://google.com"));
        emptyTestNote.addElement(new EventInfo("Deadline", null, "2020-06-12 00:00:00"));
        emptyTestNote.addElement(new Note("Note1"));

        System.out.println(emptyTestNote);
        // ToDo: Use fluent assertions (no need for Hamcrest matchers)?
        // assertThat("Expected Contact type at position 0", testNote.getElement(0));
        assertSame(emptyTestNote.getElement(0), emptyTestNote.getElement(Contact.class, 0),
                "Expected Contact type at position 0");
        assertSame(emptyTestNote.getElement(1), emptyTestNote.getElement(MediaItem.class, 0),
                "Expected MediaItem type at position 1");
        assertSame(emptyTestNote.getElement(2), emptyTestNote.getElement(Price.class, 0),
                "Expected Price type at position 2");
        assertSame(emptyTestNote.getElement(3), emptyTestNote.getElement(Link.class, 0),
                "Expected Link type at position 3");
        assertSame(emptyTestNote.getElement(4), emptyTestNote.getElement(EventInfo.class, 0),
                "Expected EventInfo type at position 4");
    }

    @Test
    public void createNoteWithTwoSubNotes() {
        emptyTestNote.addElement(subNote1);
        emptyTestNote.addElement(subNote2);

        // System.out.println(emptyTestNote);
        assertSame(emptyTestNote.getElement(0), emptyTestNote.getElement(Note.class, 0),
                "Expected Contact type at position 0");
        assertSame(emptyTestNote.getElement(1), emptyTestNote.getElement(Note.class, 1),
                "Expected Contact type at position 0");
        // ToDo: Assert size of sub-note list = 2
    }

    @Test
    // ToDo: MP suggests callingClearRemovesAllElements()
    public void removeAllElements() {
        // System.out.println(noteWithRandomElements);
        assertEquals(defaultElementCount, noteWithRandomElements.getElements().size(), "Number of Note elements");
        noteWithRandomElements.getElements().clear();
        assertEquals(0, noteWithRandomElements.getElements().size(), "Number of Note elements");
    }

    @Test
    public void removeOneOfMultipleElements() {
        NoteElement<?> element = noteWithRandomElements.getElements().get(0);
        noteWithRandomElements.removeElement(element);
        // This test has randomly failed here:
        assertFalse(noteWithRandomElements.getElements().contains(element), "Note contains element '" + element.getName() + "'");
        assertEquals(defaultElementCount - 1, noteWithRandomElements.getElements().size(),
                "Number of note elements after deletion");
    }

    // ToDo: Validate elementLocator after add and remove (make it protected?)

    @Test
    public void addElementAtASpecifiedLocation() {
        NoteElement<?> testNoteElement = NoteTestHelper.getRandomNoteElement();
        int elementIndex = 3;
        noteWithRandomElements.addElement(testNoteElement, 3);
        assertEquals(defaultElementCount + 1, noteWithRandomElements.getElements().size(), "Number of Note elements");
        assertSame(testNoteElement, noteWithRandomElements.getElement(3),
                "Expected note element '" + testNoteElement.getName() + "' at index " + elementIndex);
    }

    @Test
    // ToDo: Test this on a note with multiple sub-notes of different types (e.g. noteWithRandomElements)
    // ToDo: MP suggests gettingNonExistentNoteTypeThrowsError()
    public void tryGettingInvalidClassTypeFromElementList() {
        Exception ex = assertThrows(RuntimeException.class, () -> emptyTestNote.getElement(Note.class, 0));
        // System.out.println("Exception message: " + ex.getMessage());
        String expectedMessage = "No Note element found";
        assertTrue(ex.getMessage().contains(expectedMessage), "Exception message contains '" + expectedMessage + "'");
    }

    @Test
    public void tryGettingInvalidElementIndexFromElementList() throws MalformedURLException {
        emptyTestNote.addElement(new Link("Google", "http://facebook.com"));

        Exception ex = assertThrows(RuntimeException.class, () -> emptyTestNote.getElement(Link.class, 1));
        // System.out.println("Exception message: " + ex.getMessage());
        String expectedMessage = "Invalid index (1) for list of Link elements (size 1)";
        assertEquals(expectedMessage, ex.getMessage(), "Exception message equals " + expectedMessage);
    }

    // ToDo: Test adding and removing more than one of the same kind of element
    // ToDo: Test adding and removing tags
    // ToDo: Test adding and removing categories (coupled with main program)?
    // ToDo: Test adding tags and categories?
    // ToDo: More negative tests?
    // ToDo: Test adding duplicate notes?
    // ToDo: Test adding a Link element with an invalid URL
}
