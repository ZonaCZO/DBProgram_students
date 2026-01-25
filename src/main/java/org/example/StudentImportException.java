package org.example;

/**
 * Custom exception for CSV import errors.
 */
public class StudentImportException extends RuntimeException {
    public StudentImportException(String message) {
        super(message);
    }
}