package org.example;

import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the StudentManager interface.
 * Handles database interactions using JDBC, transactions, and the Singleton pattern.
 */
public class StudentManagerImpl implements StudentManager {

    private static final Logger LOGGER = Logger.getLogger(StudentManagerImpl.class.getName());
    private static StudentManagerImpl instance;
    private static final String DB_URL = "jdbc:sqlite:student_management.db";

    /**
     * Private constructor to enforce Singleton pattern.
     * Initializes the database connection and schema.
     */
    private StudentManagerImpl() {
        initializeDatabase();
    }

    /**
     * Returns the singleton instance of the StudentManager.
     * @return The singleton instance.
     */
    public static synchronized StudentManagerImpl getInstance() {
        if (instance == null) instance = new StudentManagerImpl();
        return instance;
    }

    /**
     * Initializes the database schema.
     * Creates tables for students, courses, and enrollments if they do not exist.
     * Populates default courses if the course table is empty.
     */
    private void initializeDatabase() {
        String createStudents = "CREATE TABLE IF NOT EXISTS students (studentID TEXT PRIMARY KEY, name TEXT NOT NULL, age INTEGER, grade REAL, enrollmentDate TEXT);";
        String createCourses = "CREATE TABLE IF NOT EXISTS courses (courseCode TEXT PRIMARY KEY, courseName TEXT, credits INTEGER);";
        String createEnrollments = "CREATE TABLE IF NOT EXISTS enrollments (studentID TEXT, courseCode TEXT, enrollmentGrade REAL, PRIMARY KEY (studentID, courseCode), FOREIGN KEY (studentID) REFERENCES students(studentID) ON DELETE CASCADE, FOREIGN KEY (courseCode) REFERENCES courses(courseCode) ON DELETE CASCADE);";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createStudents);
            stmt.execute(createCourses);
            stmt.execute(createEnrollments);
            stmt.execute("PRAGMA foreign_keys = ON;");

            // Populate default courses if table is empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM courses");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO courses (courseCode, courseName, credits) VALUES " +
                        "('CS101', 'Intro to Java', 5), " +
                        "('MATH101', 'Calculus I', 4), " +
                        "('HIST101', 'World History', 3), " +
                        "('PHYS101', 'Physics', 4);");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database initialization error", e);
        }
    }

    /**
     * Retrieves all available courses from the database.
     * @return A map where Key is CourseCode and Value is CourseName.
     */
    @Override
    public Map<String, String> getAllCourses() {
        Map<String, String> courses = new java.util.HashMap<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT courseCode, courseName FROM courses")) {
            while (rs.next()) courses.put(rs.getString("courseCode"), rs.getString("courseName"));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error loading courses", e); }
        return courses;
    }

    /**
     * Adds a new student and their course enrollments to the database.
     * Uses transactions to ensure data integrity.
     * @param student The student object to add.
     */
    @Override
    public void addStudent(Student student) {
        String sqlStudent = "INSERT INTO students(studentID, name, age, grade, enrollmentDate) VALUES(?,?,?,?,?)";
        String sqlEnroll = "INSERT INTO enrollments(studentID, courseCode) VALUES(?,?)";

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false); // Start transaction

            try (PreparedStatement pstmt = conn.prepareStatement(sqlStudent)) {
                pstmt.setString(1, student.getStudentID());
                pstmt.setString(2, student.getName());
                pstmt.setInt(3, student.getAge());
                pstmt.setDouble(4, student.getGrade());
                pstmt.setString(5, student.getEnrollmentDate().toString());
                pstmt.executeUpdate();
            }

            if (!student.getCourses().isEmpty()) {
                try (PreparedStatement pstmtEnroll = conn.prepareStatement(sqlEnroll)) {
                    for (String code : student.getCourses()) {
                        pstmtEnroll.setString(1, student.getStudentID());
                        pstmtEnroll.setString(2, code);
                        pstmtEnroll.addBatch();
                    }
                    pstmtEnroll.executeBatch();
                }
            }
            conn.commit(); // Commit transaction
            LOGGER.info("Student added: " + student.getName());
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Error adding student: " + e.getMessage());
        } finally { closeQuietly(conn); }
    }

    /**
     * Updates an existing student's personal info and course enrollments.
     * Old enrollments are removed and replaced with the new list.
     * @param studentID The ID of the student to update.
     * @param updatedStudent The object containing updated data.
     */
    @Override
    public void updateStudent(String studentID, Student updatedStudent) {
        String sqlUpdateInfo = "UPDATE students SET name = ?, age = ?, grade = ? WHERE studentID = ?";
        String sqlDeleteEnroll = "DELETE FROM enrollments WHERE studentID = ?";
        String sqlInsertEnroll = "INSERT INTO enrollments(studentID, courseCode) VALUES(?,?)";

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdateInfo)) {
                pstmt.setString(1, updatedStudent.getName());
                pstmt.setInt(2, updatedStudent.getAge());
                pstmt.setDouble(3, updatedStudent.getGrade());
                pstmt.setString(4, studentID);
                pstmt.executeUpdate();
            }

            // Remove old courses
            try (PreparedStatement pstmtDel = conn.prepareStatement(sqlDeleteEnroll)) {
                pstmtDel.setString(1, studentID);
                pstmtDel.executeUpdate();
            }

            // Add new courses
            if (!updatedStudent.getCourses().isEmpty()) {
                try (PreparedStatement pstmtIns = conn.prepareStatement(sqlInsertEnroll)) {
                    for (String courseCode : updatedStudent.getCourses()) {
                        pstmtIns.setString(1, studentID);
                        pstmtIns.setString(2, courseCode);
                        pstmtIns.addBatch();
                    }
                    pstmtIns.executeBatch();
                }
            }

            conn.commit();
            LOGGER.info("Student updated: " + studentID);
        } catch (SQLException e) {
            rollbackQuietly(conn);
            LOGGER.log(Level.SEVERE, "Update error", e);
            throw new RuntimeException("Database error during update.");
        } finally { closeQuietly(conn); }
    }

    /**
     * Removes a student from the database.
     * Related enrollments are automatically deleted via CASCADE.
     * @param studentID The ID of the student to remove.
     */
    @Override
    public void removeStudent(String studentID) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM students WHERE studentID = ?")) {
            conn.createStatement().execute("PRAGMA foreign_keys = ON;");
            pstmt.setString(1, studentID);
            pstmt.executeUpdate();
            LOGGER.info("Student removed: " + studentID);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Deletion error", e); }
    }

    @Override
    public List<Student> displayAllStudents() {
        return getStudentsByQuery("SELECT * FROM students ORDER BY name");
    }

    @Override
    public List<Student> searchStudents(String query) {
        String sql = "SELECT * FROM students WHERE name LIKE '%" + query + "%' OR studentID LIKE '%" + query + "%'";
        return getStudentsByQuery(sql);
    }

    private List<Student> getStudentsByQuery(String sql) {
        List<Student> students = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) students.add(mapRowToStudent(conn, rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error retrieving list", e); }
        return students;
    }

    @Override
    public double calculateAverageGrade() {
        return displayAllStudents().stream().mapToDouble(Student::getGrade).filter(g -> g > 0).average().orElse(0.0);
    }

    /**
     * Exports student data to a CSV file.
     * Format: ID,Name,Age,Grade,Date,Courses(semicolon separated)
     * FIX: Uses Locale.US to ensure dot is used as decimal separator, preventing CSV corruption.
     * @param filePath The destination file path.
     */
    @Override
    public void exportStudentsToCSV(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("ID,Name,Age,Grade,Date,Courses\n");
            for (Student s : displayAllStudents()) {
                String coursesStr = String.join(";", s.getCourses());
                // Force US locale to ensure dot (.) is used for decimals instead of comma (,)
                writer.write(String.format(java.util.Locale.US, "%s,%s,%d,%.2f,%s,%s\n",
                        s.getStudentID(), s.getName(), s.getAge(), s.getGrade(), s.getEnrollmentDate(), coursesStr));
            }
        } catch (IOException e) { throw new RuntimeException("Export error: " + e.getMessage()); }
    }

    /**
     * Imports student data from a CSV file.
     * Handles parsing of course lists and skips invalid lines or duplicates.
     * @param filePath The source file path.
     */
    @Override
    public void importStudentsFromCSV(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length >= 6) {
                    try {
                        String id = p[0];
                        String name = p[1];
                        int age = Integer.parseInt(p[2]);
                        double grade = Double.parseDouble(p[3]);
                        LocalDate date = LocalDate.parse(p[4]);

                        ArrayList<String> courses = new ArrayList<>();
                        if (!p[5].isEmpty()) {
                            for (String c : p[5].split(";")) {
                                if (!c.trim().isEmpty()) courses.add(c.trim());
                            }
                        }
                        // Add student with specific ID from CSV
                        addStudent(new Student(id, name, age, grade, date, courses));
                    } catch (Exception ex) {
                        LOGGER.warning("Skipping invalid line or duplicate ID during import: " + line);
                    }
                }
            }
        } catch (Exception e) { LOGGER.log(Level.SEVERE, "Import error", e); }
    }

    /**
     * Helper method to map a ResultSet row to a Student object.
     * Also fetches the list of enrolled courses.
     */
    private Student mapRowToStudent(Connection conn, ResultSet rs) throws SQLException {
        String id = rs.getString("studentID");
        ArrayList<String> courses = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT courseCode FROM enrollments WHERE studentID = ?")) {
            pstmt.setString(1, id);
            try (ResultSet rsC = pstmt.executeQuery()) {
                while (rsC.next()) courses.add(rsC.getString("courseCode"));
            }
        }
        return new Student(id, rs.getString("name"), rs.getInt("age"), rs.getDouble("grade"), LocalDate.parse(rs.getString("enrollmentDate")), courses);
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) try { conn.rollback(); } catch (SQLException e) {}
    }
    private void closeQuietly(Connection conn) {
        if (conn != null) try { conn.close(); } catch (SQLException e) {}
    }
}