package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// ToDo: Convert this class to be the application entry point
// ToDo: Should be able to remove list of Notes and category tracking here since we are storing Note in DB, not memory?
// ToDo: Add searchForContacts() API (can just search with noteType Contact)?
// ToDo: Track contacts by type (e.g. people and businesses)?
// ToDo: Have a mechanism to complete/deactivate notes and don't load them by default
// ToDo: Add Javadoc (this is the main program / entry point)!
//   E.g. user-defined versus internal categories
// ToDo: Add DEBUG logging (for exception context)
// ToDo: Can (or should) this class be a static singleton (how would that work with Hibernate)?
public class NoteOrganizer {
    // ToDo: Create one universal logger for the whole app?
    private static final Logger LOG = LogManager.getLogger(NoteOrganizer.class);

    public NoteOrganizer() {
    }

    public void initialize() {
        // ToDo: Set up standard Note categories (e.g. scheduled, purchase, etc.)
    }
}
