package com.roger.redis.exception;

/**
 * Exception thrown when a requested resource cannot be found.
 * Typically results in an HTTP 404 response.
 *
 * <p>This exception carries metadata about the resource that was not found,
 * including the resource type name, the field used for lookup, and the
 * value that was searched for.</p>
 *
 * @author Roger
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    /**
     * Constructs a new {@code ResourceNotFoundException} with a detailed message.
     *
     * @param resourceName the type of resource that was not found (e.g., "Country")
     * @param fieldName    the field used for the lookup (e.g., "id", "code")
     * @param fieldValue   the value that was searched for
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    /**
     * Returns the name of the resource type that was not found.
     *
     * @return the resource name
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * Returns the field name used for the lookup.
     *
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the value that was searched for.
     *
     * @return the field value
     */
    public Object getFieldValue() {
        return fieldValue;
    }
}
