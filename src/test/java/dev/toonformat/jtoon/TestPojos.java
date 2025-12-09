package dev.toonformat.jtoon;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Test POJOs (records) for JToon encoding tests.
 * These records cover various scenarios including simple fields, nested structures,
 * collections, and Jackson annotations.
 */
public class TestPojos {

    // ===== Simple Records =====

    /**
     * Simple person record with basic fields.
     */
    public record Person(String name, int age, boolean active) {
    }

    /**
     * Simple product record with various numeric types.
     */
    public record Product(int id, String name, double price, boolean inStock) {
    }

    /**
     * Record with nullable fields to test null handling.
     */
    public record NullableData(String text, Integer count, Boolean flag) {
    }

    // ===== Nested Records =====

    /**
     * Address record for nested structure tests.
     */
    public record Address(String street, String city, String zipCode) {
    }

    /**
     * Employee record containing a nested Address.
     */
    public record Employee(String name, int id, Address address) {
    }

    /**
     * Deeply nested structure for testing multiple levels.
     */
    public record Company(String name, Employee manager) {
    }

    // ===== Collection Records =====

    /**
     * Record with a list of primitives.
     */
    public record Skills(String owner, List<String> skillList) {
    }

    /**
     * Record with a list of objects (for tabular format testing).
     */
    public record Team(String name, List<Person> members) {
    }

    /**
     * Record with Map fields.
     */
    public record Configuration(String name, Map<String, Object> settings) {
    }

    /**
     * Record with empty collections.
     */
    public record EmptyCollections(List<String> emptyList, Map<String, String> emptyMap) {
    }

    /**
     * Record with multiple collection types.
     */
    public record MultiCollection(List<Integer> numbers, List<String> tags, Map<String, Integer> counts) {
    }

    // ===== Annotated Records =====

    /**
     * Record with @JsonProperty annotation for field name mapping.
     */
    public record AnnotatedProduct(
            @JsonProperty("product_id") int id,
            @JsonProperty("product_name") String name,
            double price) {
    }

    /**
     * Record with @JsonIgnore annotation to exclude fields.
     */
    public record SecureData(
            String publicField,
            @JsonIgnore String secretField,
            int version) {
    }

    /**
     * Record with multiple Jackson annotations.
     */
    public record ComplexAnnotated(
            @JsonProperty("user_id") int id,
            String name,
            @JsonIgnore String internal,
            @JsonProperty("is_active") boolean active) {
    }

    /**
     * Record combining nested structure with annotations.
     */
    public record AnnotatedEmployee(
            @JsonProperty("emp_id") int id,
            @JsonProperty("full_name") String name,
            Address address,
            @JsonIgnore String ssn) {
    }

    /**
     * Record for checking the field order in the output.
     */
    public record HotelInfoLlmRerankDTO(String no,
                                        String hotelId,
                                        String hotelName,
                                        String hotelBrand,
                                        String hotelCategory,
                                        String hotelPrice,
                                        String hotelAddressDistance) {}
}

