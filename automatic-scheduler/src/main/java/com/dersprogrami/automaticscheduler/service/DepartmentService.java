package com.dersprogrami.automaticscheduler.service;

import com.dersprogrami.automaticscheduler.model.Department;
import com.dersprogrami.automaticscheduler.model.DepartmentHead;
import com.dersprogrami.automaticscheduler.model.User;
import com.dersprogrami.automaticscheduler.repository.DepartmentHeadRepository;
import com.dersprogrami.automaticscheduler.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentHeadRepository departmentHeadRepository;

    public DepartmentService(DepartmentRepository departmentRepository, DepartmentHeadRepository departmentHeadRepository) {
        this.departmentRepository = departmentRepository;
        this.departmentHeadRepository = departmentHeadRepository;
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Optional<Department> getDepartmentById(Long id) {
        return departmentRepository.findById(id);
    }

    public Optional<Department> getDepartmentByName(String name) {
        return departmentRepository.findByName(name);
    }

    @Transactional
    public Department createDepartment(Department department) {
        return departmentRepository.save(department);
    }

    @Transactional
    public Department updateDepartment(Department department) {
        return departmentRepository.save(department);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        departmentRepository.deleteById(id);
    }

    @Transactional
    public DepartmentHead assignDepartmentHead(Department department, User user) {
        if (department.getDepartmentHead() != null) {
            DepartmentHead oldHead = department.getDepartmentHead();
            departmentHeadRepository.delete(oldHead);
        }

        DepartmentHead departmentHead = new DepartmentHead();
        departmentHead.setDepartment(department);
        departmentHead.setUser(user);

        return departmentHeadRepository.save(departmentHead);
    }

    public Optional<DepartmentHead> getDepartmentHeadByUser(User user) {
        return departmentHeadRepository.findByUser(user);
    }

    public List<Department> getDepartmentsByHeadUser(User user) {
        Optional<DepartmentHead> departmentHead = departmentHeadRepository.findByUser(user);
        return departmentHead.map(head -> List.of(head.getDepartment())).orElse(List.of());
    }
}