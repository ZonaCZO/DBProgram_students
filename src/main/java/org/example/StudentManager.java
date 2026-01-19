package org.example;

import java.util.List;
import java.util.Map;

/**
 * Student Management Interface.
 * Defines main CRUD operations, search, and analytics.
 */
public interface StudentManager {

    // Database Operations
    void addStudent(Student student);
    void removeStudent(String studentID);
    void updateStudent(String studentID, Student updatedStudent);

    // Search and Display
    List<Student> displayAllStudents();
    List<Student> searchStudents(String query);

    // Analytics
    double calculateAverageGrade();

    // Import / Export
    void exportStudentsToCSV(String filePath);
    void importStudentsFromCSV(String filePath);

    // Course Management
    Map<String, String> getAllCourses();
}