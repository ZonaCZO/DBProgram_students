package org.example;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Class representing a student.
 */
public class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    // Attributes
    private String name;
    private int age;
    private double grade; // Overall average grade (0.0 - 100.0)
    private final String studentID; // Unique ID
    private LocalDate enrollmentDate; // Enrollment date
    private ArrayList<String> courses; // List of course codes

    // --- Constructors ---

    public Student(String name, int age, double grade) {
        this.studentID = UUID.randomUUID().toString();
        this.enrollmentDate = LocalDate.now();
        this.courses = new ArrayList<>();
        setName(name);
        setAge(age);
        setGrade(grade); // This will now trigger the validation check
    }

    public Student(String studentID, String name, int age, double grade, LocalDate enrollmentDate, ArrayList<String> courses) {
        this.studentID = studentID;
        this.enrollmentDate = enrollmentDate;
        this.courses = courses != null ? courses : new ArrayList<>();
        setName(name);
        setAge(age);
        setGrade(grade);
    }

    // --- Getters and Setters ---

    public String getStudentID() { return studentID; }

    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || !name.matches("^[\\p{L} .-]+$")) {
            throw new StudentValidationException("Name contains invalid characters.");
        }
        this.name = name;
    }

    public int getAge() { return age; }
    public void setAge(int age) {
        if (age < 18 || age > 100) throw new StudentValidationException("Age must be between 18 and 100.");
        this.age = age;
    }

    public double getGrade() { return grade; }

    public void setGrade(double grade) {
        // 1. Range validation
        if (grade < 0.0 || grade > 100.0) {
            throw new StudentValidationException("Grade must be 0-100.");
        }

        // 2. Precision validation (Restored logic)
        // Checks if there are more than 2 decimal places
        double scaleCheck = grade * 100;
        if (Math.abs(scaleCheck - Math.round(scaleCheck)) > 0.0001) {
            throw new StudentValidationException("Grade must have no more than two decimal places.");
        }

        this.grade = grade;
    }

    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }

    public ArrayList<String> getCourses() { return courses; }
    public void addCourse(String courseCode) {
        if (!courses.contains(courseCode)) courses.add(courseCode);
    }
    public void removeCourse(String courseCode) { courses.remove(courseCode); }

    // --- Business Logic ---

    public String displayInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Student Details:\n");
        sb.append("----------------\n");
        sb.append("ID: ").append(studentID).append("\n");
        sb.append("Name: ").append(name).append("\n");
        sb.append("Age: ").append(age).append("\n");
        sb.append("Average Grade: ").append(String.format("%.2f", grade)).append("\n");
        sb.append("Enrolled: ").append(enrollmentDate).append("\n");

        // Use Stream API to process course list
        String coursesStr = courses.stream()
                .map(String::toUpperCase)
                .collect(Collectors.joining(", "));

        sb.append("Courses: [").append(coursesStr).append("]");
        return sb.toString();
    }

    public double calculateGPA(Map<String, Integer> courseCredits) {
        if (courses.isEmpty() || courseCredits == null) return 0.0;

        double totalWeightedPoints = 0;
        int totalCredits = 0;
        double gpaPoints = (grade / 20.0) - 1.0;
        if (gpaPoints < 0) gpaPoints = 0;

        for (String course : courses) {
            Integer credit = courseCredits.getOrDefault(course, 1);
            totalWeightedPoints += gpaPoints * credit;
            totalCredits += credit;
        }

        return totalCredits == 0 ? 0.0 : totalWeightedPoints / totalCredits;
    }

    // --- Equals and HashCode ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return Objects.equals(studentID, student.studentID);
    }

    @Override
    public int hashCode() { return Objects.hash(studentID); }
}