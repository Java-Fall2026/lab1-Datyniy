package ua.airline.model;

import ua.airline.util.AirlineUtils;
import ua.common.BaseEntity;

import java.util.Objects;

public class Airport extends BaseEntity {

    private final String iataCode;
    private final String cityName;

    private Airport(String iataCode, String cityName) {
        super();

        this.iataCode = AirlineUtils.validateIataCode(iataCode);
        this.cityName = AirlineUtils.requireNotBlank(
                cityName,
                "City name"
        );
    }

    public static Airport of(
            String iataCode,
            String cityName
    ) {
        return new Airport(iataCode, cityName);
    }

    public String getIataCode() {
        return iataCode;
    }

    public String getCityName() {
        return cityName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Airport airport = (Airport) o;

        return Objects.equals(iataCode, airport.iataCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iataCode);
    }

    @Override
    public String toString() {
        return "Airport{" +
                "iataCode='" + iataCode + '\'' +
                ", cityName='" + cityName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}