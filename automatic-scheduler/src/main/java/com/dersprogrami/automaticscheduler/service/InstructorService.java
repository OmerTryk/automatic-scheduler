package com.dersprogrami.automaticscheduler.service;

import com.dersprogrami.automaticscheduler.model.Department;
import com.dersprogrami.automaticscheduler.model.Instructor;
import com.dersprogrami.automaticscheduler.model.User;
import com.dersprogrami.automaticscheduler.repository.InstructorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InstructorService {

    private final InstructorRepository instructorRepository;

    public InstructorService(InstructorRepository instructorRepository) {
        this.instructorRepository = instructorRepository;
    }

    public List<Instructor> getAllInstructors() {
        return instructorRepository.findAll();
    }

    public Optional<Instructor> getInstructorById(Long id) {
        return instructorRepository.findById(id);
    }

    public Optional<Instructor> getInstructorByUser(User user) {
        return instructorRepository.findByUser(user);
    }

    public List<Instructor> getInstructorsByDepartment(Department department) {
        return instructorRepository.findAll().stream()
                .filter(instructor -> instructor.getDepartment() != null && 
                                     instructor.getDepartment().equals(department))
                .collect(Collectors.toList());
    }

    @Transactional
    public Instructor createInstructor(Instructor instructor) {
        return instructorRepository.save(instructor);
    }

    @Transactional
    public Instructor assignInstructorToDepartment(Instructor instructor, Department department) {
        instructor.setDepartment(department);
        return instructorRepository.save(instructor);
    }

    @Transactional
    public Instructor updateInstructor(Instructor instructor) {
        return instructorRepository.save(instructor);
    }

    @Transactional
    public void deleteInstructor(Long id) {
        instructorRepository.deleteById(id);
    }

    @Transactional
    public Instructor createInstructorFromUser(User user, Department department) {
        Instructor instructor = new Instructor();
        instructor.setUser(user);
        instructor.setDepartment(department);
        
        return instructorRepository.save(instructor);
    }
}