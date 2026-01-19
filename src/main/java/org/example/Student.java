package org.example;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;


 // Class representing a student.

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


     // Basic initialization. ID is generated automatically.

    public Student(String name, int age, double grade) {
        this.studentID = UUID.randomUUID().toString(); // Generate UUID
        this.enrollmentDate = LocalDate.now(); // Current date
        this.courses = new ArrayList<>();
        setName(name);
        setAge(age);
        setGrade(grade);
    }


    //Full initialization (e.g., when loading from DB).

    public Student(String studentID, String name, int age, double grade, LocalDate enrollmentDate, ArrayList<String> courses) {
        this.studentID = studentID;
        this.enrollmentDate = enrollmentDate;
        this.courses = courses != null ? courses : new ArrayList<>();
        setName(name);
        setAge(age);
        setGrade(grade);
    }

    // --- Getters and Setters with Validation ---

    public String getStudentID() {
        return studentID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        // Validation: only letters, spaces, and hyphens
        if (name == null || !name.matches("^[\\p{L} .-]+$")) {
            throw new StudentValidationException("Name contains invalid characters. Only letters, spaces, and hyphens are allowed.");
        }
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        // Validation: 18 - 100
        if (age < 18 || age > 100) {
            throw new StudentValidationException("Student age must be between 18 and 100.");
        }
        this.age = age;
    }

    public double getGrade() {
        return grade;
    }

    public void setGrade(double grade) {
        // Range validation 0.0 - 100.0
        if (grade < 0.0 || grade > 100.0) {
            throw new StudentValidationException("Grade must be between 0.0 and 100.0.");
        }
        // Precision validation (up to two decimal places)
        // Check if there is significant decimal part beyond 2 digits
        double scaleCheck = grade * 100;
        if (Math.abs(scaleCheck - Math.round(scaleCheck)) > 0.0001) {
            throw new StudentValidationException("Grade must have no more than two decimal places.");
        }
        this.grade = grade;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    // --- Course Management ---

    public ArrayList<String> getCourses() {
        return courses;
    }

    public void addCourse(String courseCode) {
        if (!courses.contains(courseCode)) {
            courses.add(courseCode);
        }
    }

    public void removeCourse(String courseCode) {
        courses.remove(courseCode);
    }

    // --- Business Logic Methods ---

    /**
     * Formats student details into a string.
     * Uses StringBuilder and Streams.
     */
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
                .map(String::toUpperCase) // Example processing
                .collect(Collectors.joining(", "));

        sb.append("Courses: [").append(coursesStr).append("]");
        return sb.toString();
    }

     //Calculates weighted GPA.

    public double calculateGPA(Map<String, Integer> courseCredits) {
        if (courses.isEmpty() || courseCredits == null) {
            return 0.0;
        }
        /* Logic can be extended. Simplified example:
        Assume 'grade' is overall grade, but if we wanted to calculate based on
        individual courses, we would need a grades table per course.
        Current structure has only overall 'grade'.
        This method returns grade converted to 4.0 scale (example logic).
        */
        return (grade / 20.0) - 1.0; // Approximate conversion of 100-point scale to GPA 4.0
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
    public int hashCode() {
        return Objects.hash(studentID);
    }
}