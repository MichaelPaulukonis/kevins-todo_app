package server.element;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import jakarta.persistence.Embeddable;

/**
 * Represents an item price.
 */
// ToDo: Make this an internal class in Product?
// ToDo: Move to notes (and extend NoteBase)?
@Embeddable
public class Price {
    private BigDecimal price;

    protected Price() { }   // No-arg constructor required by Hibernate

    public Price(BigDecimal itemPrice) {
        this.price = itemPrice.setScale(2, RoundingMode.UNNECESSARY);
    }

    public Price(double itemPrice) {
        this(new BigDecimal(itemPrice).setScale(2, RoundingMode.HALF_DOWN));
    }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal newPrice) { this.price = newPrice; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Price)) return false;
        return Objects.equals(price, ((Price)o).getPrice());
    }

    @Override
    public int hashCode() { return Objects.hash(price); }

    @Override
    // ToDo: Consider I18N (i.e. other currencies)
    public String toString() { return super.toString() + "\n$" + price; }
}
