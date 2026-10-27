package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NoteUtils {
    public static final Logger LOG = LogManager.getLogger("Universal Logger");
    public static boolean isSet(String value){ return value != null && !value.isEmpty(); }
}
