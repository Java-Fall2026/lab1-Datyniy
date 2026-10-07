package ua.airline.model;

public enum SeatClass {
    ECONOMY("Економ"),
    BUSINESS("Бізнес"),
    FIRST("Перший");

    private final String displayName;

    SeatClass(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}