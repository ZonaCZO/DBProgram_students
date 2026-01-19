package org.example;

import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Реализация менеджера студентов.
 * Использует Singleton, JDBC и транзакции.
 */
public class StudentManagerImpl implements StudentManager {

    // Логгер для записи операций и ошибок 
    private static final Logger LOGGER = Logger.getLogger(StudentManagerImpl.class.getName());
    
    // Singleton экземпляр [cite: 36]
    private static StudentManagerImpl instance;
    
    // Строка подключения (SQLite)
    private static final String DB_URL = "jdbc:sqlite:student_management.db";

    // Приватный конструктор
    private StudentManagerImpl() {
        initializeDatabase();
    }

    /**
     * Возвращает единственный экземпляр класса (Thread-safe).
     */
    public static synchronized StudentManagerImpl getInstance() {
        if (instance == null) {
            instance = new StudentManagerImpl();
        }
        return instance;
    }

    /**
     * Инициализация таблиц и включение внешних ключей.
     */
    /**
     * Инициализация таблиц и включение внешних ключей.
     * Создает таблицы, если их еще нет.
     */
    private void initializeDatabase() {
        // SQL для создания таблицы студентов
        String createStudentsTable = "CREATE TABLE IF NOT EXISTS students (" +
                "studentID TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "age INTEGER CHECK (age >= 18 AND age <= 100), " +
                "grade REAL CHECK (grade >= 0.0 AND grade <= 100.0), " +
                "enrollmentDate TEXT" +
                ");";

        // SQL для создания таблицы курсов
        // (Добавим базовую структуру, даже если пока не используем активно)
        String createCoursesTable = "CREATE TABLE IF NOT EXISTS courses (" +
                "courseCode TEXT PRIMARY KEY, " +
                "courseName TEXT, " +
                "credits INTEGER" +
                ");";

        // SQL для таблицы связей
        String createEnrollmentsTable = "CREATE TABLE IF NOT EXISTS enrollments (" +
                "studentID TEXT, " +
                "courseCode TEXT, " +
                "enrollmentGrade REAL, " +
                "PRIMARY KEY (studentID, courseCode), " +
                "FOREIGN KEY (studentID) REFERENCES students(studentID) ON DELETE CASCADE, " +
                "FOREIGN KEY (courseCode) REFERENCES courses(courseCode) ON DELETE CASCADE" +
                ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // 1. Создаем таблицы
            stmt.execute(createStudentsTable);
            stmt.execute(createCoursesTable);
            stmt.execute(createEnrollmentsTable);

            // 2. Включаем поддержку внешних ключей (для SQLite это нужно делать при каждом подключении,
            // но здесь проверим, что команда проходит)
            stmt.execute("PRAGMA foreign_keys = ON;");

            LOGGER.info("База данных успешно инициализирована/проверена.");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Ошибка инициализации БД", e);
        }
    }

    // --- Реализация методов интерфейса ---

    @Override
    public void addStudent(Student student) {
        String sqlStudent = "INSERT INTO students(studentID, name, age, grade, enrollmentDate) VALUES(?,?,?,?,?)";
        String sqlEnroll = "INSERT INTO enrollments(studentID, courseCode) VALUES(?,?)";

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false); // Начало транзакции [cite: 27, 38]

            // 1. Вставка студента
            try (PreparedStatement pstmt = conn.prepareStatement(sqlStudent)) {
                pstmt.setString(1, student.getStudentID());
                pstmt.setString(2, student.getName());
                pstmt.setInt(3, student.getAge());
                pstmt.setDouble(4, student.getGrade());
                pstmt.setString(5, student.getEnrollmentDate().toString());
                pstmt.executeUpdate();
            }

            // 2. Вставка курсов (если есть)
            if (!student.getCourses().isEmpty()) {
                try (PreparedStatement pstmtEnroll = conn.prepareStatement(sqlEnroll)) {
                    for (String courseCode : student.getCourses()) {
                        pstmtEnroll.setString(1, student.getStudentID());
                        pstmtEnroll.setString(2, courseCode);
                        pstmtEnroll.addBatch(); // Пакетная вставка [cite: 65]
                    }
                    pstmtEnroll.executeBatch();
                }
            }

            conn.commit(); // Фиксация транзакции
            LOGGER.info("Студент добавлен: " + student.getName());

        } catch (SQLException e) {
            rollbackQuietly(conn); // Откат при ошибке [cite: 108]
            LOGGER.log(Level.SEVERE, "Ошибка добавления студента", e);
            throw new RuntimeException("Не удалось добавить студента: " + e.getMessage());
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public void removeStudent(String studentID) {
        // Благодаря ON DELETE CASCADE в схеме БД, удаление из enrollments произойдет автоматически [cite: 28]
        String sql = "DELETE FROM students WHERE studentID = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            conn.createStatement().execute("PRAGMA foreign_keys = ON;"); // Убеждаемся, что каскад работает
            pstmt.setString(1, studentID);
            int affected = pstmt.executeUpdate();
            
            if (affected > 0) {
                LOGGER.info("Студент удален: " + studentID);
            } else {
                LOGGER.warning("Студент с ID " + studentID + " не найден.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Ошибка удаления студента", e);
        }
    }

    @Override
    public void updateStudent(String studentID, Student updatedStudent) {
        // Обновляем основные поля. Курсы обновлять сложнее (удалить старые -> добавить новые),
        // здесь реализуем обновление основных данных[cite: 29].
        String sql = "UPDATE students SET name = ?, age = ?, grade = ? WHERE studentID = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, updatedStudent.getName());
            pstmt.setInt(2, updatedStudent.getAge());
            pstmt.setDouble(3, updatedStudent.getGrade());
            pstmt.setString(4, studentID);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                LOGGER.info("Данные студента обновлены: " + studentID);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Ошибка обновления", e);
            throw new RuntimeException("Ошибка БД при обновлении.");
        }
    }

    @Override
    public List<Student> displayAllStudents() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students ORDER BY name"; // Сортировка по умолчанию [cite: 30]

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                students.add(mapRowToStudent(conn, rs));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Ошибка загрузки списка студентов", e);
        }
        return students;
    }

    @Override
    public List<Student> searchStudents(String query) {
        List<Student> results = new ArrayList<>();
        // Нечеткий поиск по Имени или ID [cite: 32]
        String sql = "SELECT * FROM students WHERE name LIKE ? OR studentID LIKE ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + query + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToStudent(conn, rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Ошибка поиска", e);
        }
        return results;
    }

    @Override
    public double calculateAverageGrade() {
        List<Student> allStudents = displayAllStudents();
        if (allStudents.isEmpty()) return 0.0;

        // Использование Stream API для расчета 
        // Фильтрация аномалий (например, grade > 0, хотя валидация уже есть в классе)
        return allStudents.stream()
                .mapToDouble(Student::getGrade)
                .filter(g -> g > 0.0) // Игнорируем нулевые оценки, если считаем их "отсутствием данных"
                .average()
                .orElse(0.0);
    }

    @Override
    public void exportStudentsToCSV(String filePath) {
        List<Student> students = displayAllStudents();
        
        // try-with-resources для безопасного I/O [cite: 33]
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("ID,Name,Age,Grade,Date\n"); // Заголовок
            for (Student s : students) {
                String line = String.format("%s,%s,%d,%.2f,%s",
                        s.getStudentID(), s.getName(), s.getAge(), s.getGrade(), s.getEnrollmentDate());
                writer.write(line);
                writer.newLine();
            }
            LOGGER.info("Экспорт в CSV успешен: " + filePath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Ошибка экспорта CSV", e);
            throw new RuntimeException("Ошибка записи файла.");
        }
    }

    @Override
    public void importStudentsFromCSV(String filePath) {
        List<Student> importedStudents = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // Пропуск заголовка
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    // Валидация происходит внутри конструктора Student [cite: 34]
                    try {
                        String name = parts[1];
                        int age = Integer.parseInt(parts[2]);
                        double grade = Double.parseDouble(parts[3].replace(",", ".")); // Замена , на .
                        
                        Student s = new Student(name, age, grade);
                        importedStudents.add(s);
                    } catch (Exception e) {
                        LOGGER.warning("Пропуск некорректной строки CSV: " + line);
                    }
                }
            }
            
            // Пакетное добавление в БД
            for(Student s : importedStudents) {
                addStudent(s);
            }
            LOGGER.info("Импортировано студентов: " + importedStudents.size());

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Ошибка импорта CSV", e);
        }
    }

    // --- Вспомогательные методы ---

    /**
     * Преобразует строку ResultSet в объект Student.
     * Также подгружает курсы для студента.
     */
    private Student mapRowToStudent(Connection conn, ResultSet rs) throws SQLException {
        String id = rs.getString("studentID");
        String name = rs.getString("name");
        int age = rs.getInt("age");
        double grade = rs.getDouble("grade");
        LocalDate date = LocalDate.parse(rs.getString("enrollmentDate"));

        // Загрузка курсов (отдельный запрос для простоты)
        ArrayList<String> courses = new ArrayList<>();
        String courseSql = "SELECT courseCode FROM enrollments WHERE studentID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(courseSql)) {
            pstmt.setString(1, id);
            try (ResultSet rsCourses = pstmt.executeQuery()) {
                while (rsCourses.next()) {
                    courses.add(rsCourses.getString("courseCode"));
                }
            }
        }

        return new Student(id, name, age, grade, date, courses);
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                LOGGER.severe("Ошибка отката транзакции: " + e.getMessage());
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) { /* игнорируем */ }
        }
    }
}