import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ParkingTicket {
    private Car car;
    private int spotNumber;
    private LocalDateTime entryTime;

    public ParkingTicket(Car car, int spotNumber, LocalDateTime entryTime) {
        this.car = car;
        this.spotNumber = spotNumber;
        this.entryTime = entryTime;
    }

    @Override
    public String toString() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "== Parking Ticket ==\n" +
               "Car Number: " + car.getNumber() + "\n" +
               "Car Color: " + car.getColor() + "\n" +
               "Car Type: " + car.getType() + "\n" +
               "Spot Number: " + spotNumber + "\n" +
               "Entry Time: " + dtf.format(entryTime);
    }
}
