package server.element;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
** Contains contact information.
*/
@Embeddable
public class Address {
  private String address1;
  private String address2;
  private String city;
  private String state;
  private Integer postalCode;

  public Address() {}   // No-arg constructor required by Hibernate

  public String getAddress1() { return address1; }
  public Address setAddress1(String addr1) { this.address1 = addr1; return this; }

  public String getAddress2() { return address2; }
  public Address setAddress2(String addr2) { this.address2 = addr2; return this; }

  public String getCity() { return city; }
  public Address setCity(String newCity) { this.city = newCity; return this; }

  public String getState() { return state; }
  public Address setState(String newState) { this.state = newState; return this; }

  public Integer getPostalCode() { return postalCode; }
  public Address setPostalCode(Integer code) { this.postalCode = code; return this; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Address)) return false;
    Address a = (Address)o;
    return Objects.equals(address1, a.getAddress1()) && Objects.equals(address2, a.getAddress2())
            && Objects.equals(city, a.getCity()) && Objects.equals(state, a.getState())
            && Objects.equals(postalCode, a.getPostalCode());
  }

  @Override
  public int hashCode() { return Objects.hash(address1, address2, city, state, postalCode); }

  @Override
  public String toString() {
    // ToDo: Could turn this into a toString() utility (takes a list of Strings)
    String result = "Contact:\n" + super.toString();
    String pc = postalCode == null ? null : postalCode.toString();
    for (String item : new String[] {address1, address2, city, state, pc}) {
      if (item != null && !item.isEmpty()) {
        result = result.concat("\n" + item);
      }
    }
    return result;
  }
}
