package server.categories;

import java.util.*;
import java.util.stream.Collectors;

// ToDo: Implement extensible enums like at Shutterfly (do I really need this)?
//  * Define a category class with data fields (name, other?) and getters/setters
//  * Define an interface with a get() method for returning a data class object
// Interface for internal category enums
public interface InternalCategory {
    Random rand = new Random();

    // Used for testing
    default InternalCategory getRandomTag() {
        int itemNumber = rand.nextInt(getCategories().size());
        return getCategories().stream().skip(itemNumber).findFirst().orElse(null);
    }
    default Set<String> convertToStrings() {
        return getCategories().stream().map(InternalCategory ::toString).collect(Collectors.toSet());
    }
    String getName();
    Set<InternalCategory> getCategories();
}
