package ua.airline.repository;

@FunctionalInterface
public interface IdentityExtractor<T> {

    String identityOf(T item);
}
