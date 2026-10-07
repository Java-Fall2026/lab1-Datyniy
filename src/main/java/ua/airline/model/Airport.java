package ua.airline.model;

import ua.airline.util.AirlineUtils;

public record Airport(
        String iataCode,
        String cityName
) {
    public Airport {
        iataCode = AirlineUtils.validateIataCode(iataCode);
        cityName = AirlineUtils.requireNotBlank(
                cityName,
                "City name"
        );
    }

    public static Airport of(String iataCode, String cityName) {
        return new Airport(iataCode, cityName);
    }
}