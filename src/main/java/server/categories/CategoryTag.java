package server.categories;

import java.util.*;

// ToDo: Implement extensible enums like at Shutterfly
//  * Define a category class with data fields (name, other?) and getters/setters
//  * Define an interface with a get() method for returning a data class object
//  * Define classes for internal and external (user-defined) categories?
// Interface for enum category tags
public interface CategoryTag {
    static final Random rand = new Random();

    // Used for testing
    default CategoryTag getRandomTag() {
        return getTagValues().get(rand.nextInt(getTagValues().size()));
    }

    abstract public List<CategoryTag> getTagValues();
}
