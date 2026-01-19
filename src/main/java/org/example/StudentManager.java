package org.example;

import java.util.List;

/**
 * Интерфейс управления студентами.
 * Определяет основные операции CRUD, поиск и аналитику.
 */
public interface StudentManager {
    
    // Операции с БД
    void addStudent(Student student);
    void removeStudent(String studentID);
    void updateStudent(String studentID, Student updatedStudent);
    
    // Поиск и отображение
    List<Student> displayAllStudents();
    List<Student> searchStudents(String query);
    
    // Аналитика
    double calculateAverageGrade();
    
    // Импорт / Экспорт
    void exportStudentsToCSV(String filePath);
    void importStudentsFromCSV(String filePath);
}