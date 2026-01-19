package org.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StudentTest {

    @Test
    public void testValidStudent() {
        Student s = new Student("Valid Name", 20, 95.5);
        Assertions.assertEquals("Valid Name", s.getName());
        Assertions.assertEquals(20, s.getAge());
        Assertions.assertEquals(95.5, s.getGrade());
    }

    @Test
    public void testInvalidName() {
        // Name with special characters should cause error
        Assertions.assertThrows(StudentValidationException.class, () -> {
            new Student("Bad@Name", 20, 90);
        });
    }

    @Test
    public void testAgeBoundaries() {
        // Less than 18
        Assertions.assertThrows(StudentValidationException.class, () -> new Student("Test", 17, 90));
        // More than 100
        Assertions.assertThrows(StudentValidationException.class, () -> new Student("Test", 101, 90));
    }

    @Test
    public void testGradePrecision() {
        // More than 2 decimal places
        Assertions.assertThrows(StudentValidationException.class, () -> new Student("Test", 20, 85.555));
    }

    @Test
    public void testCourseManagement() {
        Student s = new Student("Test", 20, 90);
        s.addCourse("CS101");
        Assertions.assertTrue(s.getCourses().contains("CS101"));
        s.removeCourse("CS101");
        Assertions.assertFalse(s.getCourses().contains("CS101"));
    }
}