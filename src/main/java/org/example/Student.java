package org.example;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Класс, представляющий студента.
 * [cite: 7]
 */
public class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    // Атрибуты [cite: 9]
    private String name;
    private int age;
    private double grade; // Общий средний балл (0.0 - 100.0)
    private final String studentID; // Уникальный ID
    private LocalDate enrollmentDate; // Дата зачисления
    private ArrayList<String> courses; // Список кодов курсов

    // --- Конструкторы  ---

    /**
     * Базовая инициализация. ID генерируется автоматически.
     */
    public Student(String name, int age, double grade) {
        this.studentID = UUID.randomUUID().toString(); // Генерация UUID
        this.enrollmentDate = LocalDate.now(); // Текущая дата
        this.courses = new ArrayList<>();
        setName(name);
        setAge(age);
        setGrade(grade);
    }

    /**
     * Полная инициализация (например, при загрузке из БД).
     */
    public Student(String studentID, String name, int age, double grade, LocalDate enrollmentDate, ArrayList<String> courses) {
        this.studentID = studentID;
        this.enrollmentDate = enrollmentDate;
        this.courses = courses != null ? courses : new ArrayList<>();
        setName(name);
        setAge(age);
        setGrade(grade);
    }

    // --- Геттеры и Сеттеры с валидацией [cite: 18] ---

    public String getStudentID() {
        return studentID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        // Валидация: только буквы, пробелы и дефисы
        if (name == null || !name.matches("^[\\p{L} .-]+$")) {
            throw new StudentValidationException("Имя содержит недопустимые символы. Разрешены только буквы, пробелы и дефисы.");
        }
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        // Валидация: от 18 до 100
        if (age < 18 || age > 100) {
            throw new StudentValidationException("Возраст студента должен быть в диапазоне от 18 до 100 лет.");
        }
        this.age = age;
    }

    public double getGrade() {
        return grade;
    }

    public void setGrade(double grade) {
        // Валидация диапазона 0.0 - 100.0
        if (grade < 0.0 || grade > 100.0) {
            throw new StudentValidationException("Оценка должна быть от 0.0 до 100.0.");
        }
        // Валидация точности (до двух знаков)
        // Проверяем, есть ли значимая дробная часть за пределами 2 знаков
        double scaleCheck = grade * 100;
        if (Math.abs(scaleCheck - Math.round(scaleCheck)) > 0.0001) {
            throw new StudentValidationException("Оценка может иметь не более двух знаков после запятой.");
        }
        this.grade = grade;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    // --- Управление курсами---

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

    // --- Методы бизнес-логики ---

    /**
     * Форматирует детали студента в строку.
     * Использует StringBuilder и Streams.
     *
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

        // Использование Stream API для обработки списка курсов
        String coursesStr = courses.stream()
                .map(String::toUpperCase) // Пример обработки
                .collect(Collectors.joining(", "));

        sb.append("Courses: [").append(coursesStr).append("]");
        return sb.toString();
    }

    /**
     * Вычисляет взвешенный GPA.
     * @param courseCredits карта, где ключ - код курса, значение - кредиты (вес).
     *
     */
    public double calculateGPA(Map<String, Integer> courseCredits) {
        if (courses.isEmpty() || courseCredits == null) {
            return 0.0;
        }
        // Логика может быть расширена. Здесь упрощенный пример:
        // Предполагаем, что 'grade' - это общая оценка, но если мы хотим считать
        // на основе отдельных курсов, нам нужна была бы таблица оценок по курсам.
        // Согласно текущей структуре Student, у нас есть только общий 'grade'.
        // Данный метод возвращает grade, сконвертированный в 4.0 шкалу (пример логики).
        return (grade / 20.0) - 1.0; // Примерная конвертация 100-балльной шкалы в GPA 4.0
    }

    // --- Equals и HashCode  ---

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