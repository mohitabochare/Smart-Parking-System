public class Car {
    private String number;
    private String color;
    private String type;

    public Car(String number, String color, String type) {
        this.number = number;
        this.color = color;
        this.type = type;
    }

    public String getNumber() { return number; }
    public String getColor() { return color; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return number + " | " + color + " | " + type;
    }
}
