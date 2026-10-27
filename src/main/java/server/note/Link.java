package server.note;

import jakarta.persistence.DiscriminatorValue;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Represents a web link.
 */
// ToDo: Support entering a link as free text and storing and displaying it as a URL
@jakarta.persistence.Entity
@DiscriminatorValue("LINK")
public class Link extends NoteBase {
  // ToDo: Define this as a URI instead of URL?
  private URL linkURL;
  private boolean broken = false;   // True if the link is inaccessible/broken

  public Link(String linkName, String linkNote, String url) {
    super(linkName, linkNote);
    setURL(url);
  }

  public Link(String name, String url) { this(name, null, url); }

  protected Link() {}   // No-arg constructor required by Hibernate

  public URL getUrl() { return linkURL; }
  public void setURL(String linkURL) {
    try {
      this.linkURL = new URI(linkURL).toURL();
    } catch (URISyntaxException|MalformedURLException ex) {
      throw new RuntimeException("Failed to create link for URL: " + linkURL, ex);
    }
  }
  public String getURLString() { return linkURL != null ? linkURL.toString() : null; }

  // ToDo: Add testLink() and set broken = true if it can't be accessed?
  public boolean isBroken() { return broken; }
  public void setBroken(boolean value) { broken = value; }

  @Override
  public String toString() {
    return "Link:\n" + super.toString() + "\n" + linkURL.toString();
  }
}
