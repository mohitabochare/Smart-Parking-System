import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ParkingLot {
    private List<ParkingSpot> spots;

    public ParkingLot(int totalSpots) {
        spots = new ArrayList<>();
        for (int i = 1; i <= totalSpots; i++) {
            spots.add(new ParkingSpot(i));
        }
    }

    public ParkingTicket parkCar(Car car) {
        for (ParkingSpot spot : spots) {
            if (!spot.isOccupied()) {
                spot.parkCar(car);
                ParkingTicket ticket = new ParkingTicket(car, spot.getNumber(), LocalDateTime.now());

                // ✅ Generate QR Code
               String qrData = ticket.toString();
               String qrFileName = "Car_" + car.getNumber() + ".png";
               int width = 300;   // QR code width
               int height = 300;  // QR code height
               QRGenerator.generateQRCode(qrData, qrFileName, width, height);

                return ticket;
            }
        }
        return null; // Parking full
    }

    public Car removeCar(String carNumber) {
        for (ParkingSpot spot : spots) {
            if (spot.isOccupied() && spot.getCar().getNumber().equalsIgnoreCase(carNumber)) {
                Car c = spot.getCar();
                spot.removeCar();
                return c;
            }
        }
        return null;
    }

    public String listCars() {
        StringBuilder sb = new StringBuilder("== Parked Cars ==\n");
        for (ParkingSpot spot : spots) {
            if (spot.isOccupied()) {
                Car c = spot.getCar();
                sb.append("Spot ").append(spot.getNumber())
                  .append(": ").append(c.getNumber())
                  .append(" | ").append(c.getColor())
                  .append(" | ").append(c.getType()).append("\n");
            }
        }
        return sb.toString();
    }

    public int getTotalParkedCars() {
        int count = 0;
        for (ParkingSpot spot : spots) {
            if (spot.isOccupied()) count++;
        }
        return count;
    }
}
