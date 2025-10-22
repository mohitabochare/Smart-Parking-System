public class ParkingSpot {
    private int number;
    private boolean occupied;
    private Car car;

    // Existing 9-field constructor
    String bookingId;
    String vehicleNumber;
    String spotNumber;
    String name;
    String phone;
    String inTime;
    String duration;
    String amount;
    String status;

    public ParkingSpot(String bookingId, String vehicleNumber, String spotNumber,
                       String name, String phone, String inTime,
                       String duration, String amount, String status) {
        this.bookingId = bookingId;
        this.vehicleNumber = vehicleNumber;
        this.spotNumber = spotNumber;
        this.name = name;
        this.phone = phone;
        this.inTime = inTime;
        this.duration = duration;
        this.amount = amount;
        this.status = status;
    }

    // ✅ New constructor for ParkingLot
    public ParkingSpot(int number) {
        this.number = number;
        this.occupied = false;
        this.car = null;
    }

    public int getNumber() { return number; }
    public boolean isOccupied() { return occupied; }
    public void parkCar(Car car) {
        this.car = car;
        this.occupied = true;
    }
    public Car getCar() { return car; }
    public void removeCar() {
        this.car = null;
        this.occupied = false;
    }
}
