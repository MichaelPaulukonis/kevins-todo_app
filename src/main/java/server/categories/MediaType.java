package server.categories;

import java.util.Set;

public enum MediaType implements InternalCategory {
    RECORD, SONG, BOOK, FILM, TV_SHOW, VIDEO, ARTICLE, QUOTE;

    public String getName() { return this.name(); }
    public Set<InternalCategory> getCategories() { return Set.of(values()); }

    // Hides the fact you have to call enum methods through an arbitrary enum value
    public static MediaType get() { return values()[0]; }
}
