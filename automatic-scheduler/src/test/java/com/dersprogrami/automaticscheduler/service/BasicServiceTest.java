package com.dersprogrami.automaticscheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dersprogrami.automaticscheduler.model.Course;
import com.dersprogrami.automaticscheduler.model.Department;
import com.dersprogrami.automaticscheduler.model.Instructor;
import com.dersprogrami.automaticscheduler.model.User;
import com.dersprogrami.automaticscheduler.repository.CourseRepository;

@ExtendWith(MockitoExtension.class)
public class BasicServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    private Course course;
    private Department department;
    private Instructor instructor;
    private User user;
    private List<Course> courses;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setName("Bilgisayar Mühendisliği");

        user = new User();
        user.setId(1L);
        user.setUsername("instructor1");
        user.setPassword("password");
        user.setEmail("instructor1@example.com");
        user.setFullName("Ali Yılmaz");

        instructor = new Instructor();
        instructor.setId(1L);
        instructor.setTitle("Dr.");
        instructor.setUser(user);
        instructor.setDepartment(department);

        course = new Course();
        course.setId(1L);
        course.setCode("CS101");
        course.setName("Programlamaya Giriş");
        course.setWeeklyHours(4);
        course.setDepartment(department);
        course.setCredits(3);
        course.setIsElective(false);
        
        Set<Instructor> instructors = new HashSet<>();
        instructors.add(instructor);
        course.setInstructors(instructors);

        courses = new ArrayList<>();
        courses.add(course);
    }

    @Test
    void getAllCourses_ShouldReturnAllCourses() {
        // Given
        when(courseRepository.findAll()).thenReturn(courses);

        // When
        List<Course> result = courseService.getAllCourses();

        // Then
        assertEquals(1, result.size());
        assertEquals("CS101", result.get(0).getCode());
        verify(courseRepository, times(1)).findAll();
    }

    @Test
    void getCourseById_ShouldReturnCourse() {
        // Given
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        // When
        Optional<Course> result = courseService.getCourseById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Programlamaya Giriş", result.get().getName());
        verify(courseRepository, times(1)).findById(1L);
    }

    @Test
    void createCourse_ShouldSaveAndReturnCourse() {
        // Given
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        // When
        Course result = courseService.createCourse(course);

        // Then
        assertNotNull(result);
        assertEquals("CS101", result.getCode());
        verify(courseRepository, times(1)).save(course);
    }
}
