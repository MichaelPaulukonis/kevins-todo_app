package server.note;

import jakarta.persistence.DiscriminatorValue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// ToDo: Support recurring events?
@jakarta.persistence.Entity
@DiscriminatorValue("SCHEDULE_ITEM")
public class ScheduleItem extends Note {
    // 12-hour timestamp format with AM/PM
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
    private LocalDateTime date;
    private CompletionStatus status;

    public enum CompletionStatus { COMPLETED, UNFINISHED, CANCELED, MISSED }

    // ToDo: Support builder-type instantiation
    public ScheduleItem(String name, String description, LocalDateTime date) {
        super(name, description);
        this.date = date;
        this.status = CompletionStatus.UNFINISHED;
    }

    public ScheduleItem(String name, String description, String dateString) {
        this(name, description, LocalDateTime.parse(dateString, dateFormat));
    }

    protected ScheduleItem() {}   // No-arg constructor required by Hibernate

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime newDate) { this.date = newDate; }
    public void setDate(String dateString) { this.date = convertToDate(dateString); }
    public static LocalDateTime convertToDate(String dateString) { return LocalDateTime.parse(dateString, dateFormat); }

    public CompletionStatus getStatus() { return status; }
    public void setStatus(CompletionStatus newStatus) {
        this.status = newStatus;
        this.setCompleted(this.status != CompletionStatus.UNFINISHED);
    }

    @Override
    public String toString() {
        return "Scheduled Item:\n" + super.toString() + "\n" + dateFormat.format(date) + "\n" + status;
    }
}
