package server;

import server.categories.Category;
import server.categories.InternalCategory;
import server.note.NoteBase;

import java.util.List;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Set;

import static server.NoteUtils.isSet;

// ToDo: Add option to search by date created or date modified (or date range)?
public class SearchCriteria {
    // ToDo: Add searchChildNotes property if I support it?
    private final Set<Category> categories = new HashSet<>();
    private Class<? extends NoteBase> noteClass = NoteBase.class;
    private String nameMatch;
    private boolean doPatternMatch = false;
    private Pattern matchPattern;
    // Currently this only applies to categories
    // Support a more lax search criteria (false/OR) by default
    boolean joinWithAnd = false;    // Whether to search using AND or OR criteria
    boolean completed = false;

    public SearchCriteria() {}

    // Builders/setters
    public SearchCriteria setCategory(Category cat) { categories.add(cat); return this; }
    public SearchCriteria setCategory(String cat) { setCategory(Category.of(cat)); return this; }
    public SearchCriteria setCategory(InternalCategory cat) { setCategory(Category.of(cat)); return this; }
    public SearchCriteria setCategories(Set<Category> newCategories) { categories.addAll(newCategories); return this; }
    public SearchCriteria setNoteClass(Class<? extends NoteBase> noteClass) { this.noteClass = noteClass; return this; }
    // Name match is case-insensitive
    public SearchCriteria setNameMatch(String nameMatch) {
        this.nameMatch = nameMatch;
        // ToDo: Only set matchPattern if doPatternMatch == true?
        this.matchPattern = !isSet(nameMatch) ? null : Pattern.compile(nameMatch, Pattern.CASE_INSENSITIVE);
        return this;
    }
    public SearchCriteria setJoinWithAnd(boolean joinWithAnd) { this.joinWithAnd = joinWithAnd; return this; }
    public SearchCriteria joinWithAnd() { return setJoinWithAnd(true); }
    public SearchCriteria setDoPatternMatch(boolean patternMatch) { this.doPatternMatch = patternMatch; return this; }
    public SearchCriteria doPatternMatch() { return setDoPatternMatch(true); }
    public SearchCriteria setCompleted(boolean value) { this.completed = value; return this; }
    public SearchCriteria isCompleted() { return setCompleted(true); }

    public String getNameMatch() { return nameMatch; }
    // Pre-processed for SQL LIKE queries
    public String getSQLNameMatch() { return isSet(nameMatch) ? "%" + nameMatch.toLowerCase() + "%" : null; }
    public String getNamePattern() { return matchPattern.pattern(); }
    public boolean isPatternMatch() { return doPatternMatch; }
    public boolean isJoinWithAnd() { return joinWithAnd; }
    public Class<? extends NoteBase> getNoteClass() { return noteClass; }
    public Set<Category> getCategories() { return categories; }
    // Helper method for SQL queries in findMatchingNotesInDB()
    public List<String> getCategoryNames() { return categories.stream().map(Category::getName).toList(); }
    public boolean hasCategories() { return !categories.isEmpty(); }
}
