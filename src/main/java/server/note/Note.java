package server.note;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the base class for all Note elements.
 *
 * The setter methods return the "this" object to allow for chaining.
 */
@jakarta.persistence.Entity
@DiscriminatorValue("NOTE")
public class Note extends NoteBase {
    // Maps the relationship using the 'parent' field declared in NoteBase
    @OrderColumn
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    protected List<NoteBase> childNotes = new ArrayList<>();    // ToDo: Rename "children"?
    @OrderColumn
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "note_id")
    protected List<Link> links = new ArrayList<>();

    public Note(String newName, String newDescription) {
        super(newName, newDescription);
    }

    protected Note() {}   // No-arg constructor required by Hibernate

    public Note(String newName) { this(newName, null); }

    public List<NoteBase> getChildNotes() { return childNotes; }
    // ToDo: Add version that takes Note instead of index?
    public NoteBase getChildNote(int index) { return childNotes.get(index); }
    // We call these from wrapper methods in NoteOrganizer in order to update note tracking
    public Note addChildNote(NoteBase childNote) {
        childNote.setParent(this);
        childNotes.add(childNote);
        return this;
    }
    public Note addChildNote(NoteBase childNote, int index) { childNotes.add(index, childNote); return this; }
    // ToDo: Delete childNote from DB in this case?
    public NoteBase removeChildNote(NoteBase childNote) {
        childNote.setParent(null);
        childNotes.remove(childNote);
        return childNote;
    }
    public Note addLink(Link link) { links.add(link); return this; }
    public List<Link> getLinks() { return links; }
    public Link getLink(int index) { return links.get(index); }
    // ToDo: This doesn't seem to work?
    public Link getLink(Link link) { return links.stream().filter(l -> l == link).findFirst().orElse(null); }
    public boolean hasChildNotes() { return !childNotes.isEmpty(); }

    // ToDo: Add more info to this (e.g. categories, tags, elements)?
    // ToDo: Add loop over elements
    @Override
    public String toString() {
        String result = name;
        if (description != null && !description.isEmpty()) {
            result += "\n" + description;
        }
        return result;
    }
}
