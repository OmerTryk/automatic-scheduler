package com.dersprogrami.automaticscheduler.service;

import com.dersprogrami.automaticscheduler.model.Course;
import com.dersprogrami.automaticscheduler.model.Department;
import com.dersprogrami.automaticscheduler.model.Instructor;
import com.dersprogrami.automaticscheduler.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Optional<Course> getCourseById(Long id) {
        return courseRepository.findById(id);
    }

    public List<Course> getCoursesByDepartment(Department department) {
        return courseRepository.findByDepartment(department);
    }

    public Optional<Course> getCourseByCodeAndDepartment(String code, Department department) {
        return courseRepository.findByCodeAndDepartment(code, department);
    }

    @Transactional
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(Course course) {
        return courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    @Transactional
    public Course assignInstructorToCourse(Course course, Instructor instructor) {
        Set<Instructor> instructors = course.getInstructors();
        if (instructors == null) {
            instructors = new HashSet<>();
        }
        instructors.add(instructor);
        course.setInstructors(instructors);
        
        return courseRepository.save(course);
    }

    @Transactional
    public Course removeInstructorFromCourse(Course course, Instructor instructor) {
        Set<Instructor> instructors = course.getInstructors();
        if (instructors != null) {
            instructors.remove(instructor);
            course.setInstructors(instructors);
        }
        
        return courseRepository.save(course);
    }

    public List<Course> getCoursesByInstructor(Instructor instructor) {
        List<Course> allCourses = courseRepository.findAll();
        return allCourses.stream()
                .filter(course -> course.getInstructors().contains(instructor))
                .toList();
    }
}