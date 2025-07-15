package com.dersprogrami.automaticscheduler.service;

import com.dersprogrami.automaticscheduler.algorithm.GeneticAlgorithm;
import com.dersprogrami.automaticscheduler.model.*;
import com.dersprogrami.automaticscheduler.repository.AvailabilityRepository;
import com.dersprogrami.automaticscheduler.repository.CourseRepository;
import com.dersprogrami.automaticscheduler.repository.ScheduleItemRepository;
import com.dersprogrami.automaticscheduler.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleItemRepository scheduleItemRepository;
    private final CourseRepository courseRepository;
    private final AvailabilityRepository availabilityRepository;
    private final GeneticAlgorithm geneticAlgorithm;

    public ScheduleService(ScheduleRepository scheduleRepository, 
                         ScheduleItemRepository scheduleItemRepository,
                         CourseRepository courseRepository,
                         AvailabilityRepository availabilityRepository,
                         GeneticAlgorithm geneticAlgorithm) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleItemRepository = scheduleItemRepository;
        this.courseRepository = courseRepository;
        this.availabilityRepository = availabilityRepository;
        this.geneticAlgorithm = geneticAlgorithm;
    }

    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    public Optional<Schedule> getScheduleById(Long id) {
        return scheduleRepository.findById(id);
    }

    public List<Schedule> getSchedulesByDepartment(Department department) {
        return scheduleRepository.findByDepartment(department);
    }

    public Optional<Schedule> getActiveScheduleForDepartment(Department department) {
        return scheduleRepository.findByDepartmentAndIsActiveTrue(department);
    }

    @Transactional
    public Schedule createSchedule(Schedule schedule) {
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public Schedule updateSchedule(Schedule schedule) {
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        scheduleRepository.deleteById(id);
    }

    @Transactional
    public Schedule setActiveSchedule(Schedule schedule) {
        List<Schedule> departmentSchedules = scheduleRepository.findByDepartment(schedule.getDepartment());
        for (Schedule existingSchedule : departmentSchedules) {
            existingSchedule.setIsActive(false);
            scheduleRepository.save(existingSchedule);
        }
        
        schedule.setIsActive(true);
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public Schedule generateScheduleForDepartment(Department department, String academicTerm) {
        try {
            System.out.println("ScheduleService: Program oluşturma işlemi başlıyor...");
            // Giriş parametrelerini kontrol et
            if (department == null) {
                System.err.println("ScheduleService Hata: Bölüm boş olamaz");
                throw new IllegalArgumentException("Bölüm boş olamaz");
            }
            
            if (academicTerm == null || academicTerm.trim().isEmpty()) {
                System.err.println("ScheduleService Hata: Akademik dönem boş olamaz");
                throw new IllegalArgumentException("Akademik dönem boş olamaz");
            }
    
            System.out.println("ScheduleService: Bölüm için dersleri alınıyor...");
            // Bölüm için dersleri al
            List<Course> courses = courseRepository.findByDepartment(department);
            System.out.println("ScheduleService: Toplam " + courses.size() + " ders bulundu.");
            
            // Detaylı ders dökümü
            for (Course course : courses) {
                System.out.println("  - Ders: " + course.getName() + " (ID: " + course.getId() + ")");
                System.out.println("    Haftalık saat: " + course.getWeeklyHours());
                System.out.println("    Öğretmen sayısı: " + 
                    (course.getInstructors() != null ? course.getInstructors().size() : 0));
                
                if (course.getInstructors() != null) {
                    for (Instructor instructor : course.getInstructors()) {
                        System.out.println("      * " + 
                            (instructor.getUser() != null ? instructor.getUser().getFullName() : "[Boş öğretmen]")
                            + " (ID: " + instructor.getId() + ")");
                    }
                }
            }
            
            if (courses == null || courses.isEmpty()) {
                System.err.println("ScheduleService Hata: Bu bölüm için kayıtlı ders bulunamadı");
                throw new IllegalStateException("Bu bölüm için kayıtlı ders bulunamadı");
            }

            // Derslere eğitimci atanmış mı kontrol et
            boolean anyInstructorAssigned = false;
            for (Course course : courses) {
                if (course.getInstructors() != null && !course.getInstructors().isEmpty()) {
                    anyInstructorAssigned = true;
                    break;
                }
            }
            
            if (!anyInstructorAssigned) {
                System.err.println("ScheduleService Hata: Hiçbir derse eğitimci atanmamış");
                throw new IllegalStateException("Hiçbir derse eğitimci atanmamış");
            }
            
            System.out.println("ScheduleService: Bölümdeki eğitimciler alınıyor...");
            // Bölümdeki eğitimcileri al
            List<Instructor> instructors = department.getInstructors() != null ? 
                                           department.getInstructors().stream().toList() : 
                                           new ArrayList<>();
            System.out.println("ScheduleService: Toplam " + instructors.size() + " eğitimci bulundu.");
            
            // Detaylı eğitimci bilgileri
            for (Instructor instructor : instructors) {
                System.out.println("  - Eğitimci: " + 
                    (instructor.getUser() != null ? instructor.getUser().getFullName() : "[Boş öğretmen]")
                    + " (ID: " + instructor.getId() + ")");
            }
            
            if (instructors.isEmpty()) {
                System.err.println("ScheduleService Hata: Bu bölüm için kayıtlı eğitimci bulunamadı");
                throw new IllegalStateException("Bu bölüm için kayıtlı eğitimci bulunamadı");
            }
            
            System.out.println("ScheduleService: Eğitimcilerin müsaitlik bilgileri alınıyor...");
            // Tüm eğitimcilerin müsaitlik bilgilerini al
            List<Availability> availabilities = availabilityRepository.findAll();
            System.out.println("ScheduleService: Toplam " + availabilities.size() + " müsaitlik kaydı bulundu.");
            
            // Eğitimcilere göre müsaitlik bilgisi
            Map<Long, List<Availability>> instructorAvailabilityMap = new HashMap<>();
            for (Availability availability : availabilities) {
                if (availability.getInstructor() != null && availability.getInstructor().getId() != null) {
                    Long instructorId = availability.getInstructor().getId();
                    if (!instructorAvailabilityMap.containsKey(instructorId)) {
                        instructorAvailabilityMap.put(instructorId, new ArrayList<>());
                    }
                    instructorAvailabilityMap.get(instructorId).add(availability);
                }
            }
            
            for (Instructor instructor : instructors) {
                Long instructorId = instructor.getId();
                int count = instructorAvailabilityMap.containsKey(instructorId) ? 
                          instructorAvailabilityMap.get(instructorId).size() : 0;
                
                System.out.println("  - Eğitimci " + 
                    (instructor.getUser() != null ? instructor.getUser().getFullName() : "[Boş öğretmen]")
                    + " için " + count + " müsaitlik kaydı");
                
                if (count > 0) {
                    List<Availability> instructorAvailabilities = instructorAvailabilityMap.get(instructorId);
                    for (Availability availability : instructorAvailabilities) {
                        System.out.println("    * " + availability.getDayOfWeek() + ": " + 
                                        availability.getStartHour() + "-" + availability.getEndHour());
                    }
                }
            }
            
            // Eğitimcilerin müsaitlik bilgilerini kontrol et
            boolean hasAnyAvailability = false;
            for (Instructor instructor : instructors) {
                for (Availability availability : availabilities) {
                    if (availability.getInstructor() != null && 
                        availability.getInstructor().equals(instructor)) {
                        hasAnyAvailability = true;
                        break;
                    }
                }
                if (hasAnyAvailability) break;
            }
            
            if (!hasAnyAvailability) {
                System.err.println("ScheduleService Hata: Hiçbir eğitimcinin müsaitlik bilgisi bulunamadı");
                throw new IllegalStateException("Hiçbir eğitimcinin müsaitlik bilgisi bulunamadı");
            }
            
            System.out.println("ScheduleService: Genetik algoritma çağrılıyor...");
            // Genetik algoritma ile program oluştur
            Schedule schedule = geneticAlgorithm.generateSchedule(department, courses, instructors, availabilities, academicTerm);
            
            // Oluşturulan programı kontrol et
            if (schedule == null) {
                System.err.println("ScheduleService Hata: Program oluşturulamadı");
                throw new IllegalStateException("Program oluşturulamadı");
            }
            
            // Program öğelerini doğrudan veritabanından çek
            List<ScheduleItem> scheduleItems = scheduleItemRepository.findBySchedule(schedule);
            
            if (scheduleItems == null || scheduleItems.isEmpty()) {
                System.err.println("ScheduleService Hata: Program oluşturuldu ancak program öğeleri bulunamadı. ID: " + schedule.getId());
                throw new IllegalStateException("Program oluşturuldu ancak program öğeleri bulunamadı");
            }
            
            System.out.println("ScheduleService: Program oluşturuldu, " + scheduleItems.size() + " ders programı öğesi içeriyor.");
            return schedule;
        } catch (Exception e) {
            System.err.println("ScheduleService Hata (GENEL): " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Program oluşturma hatası: " + e.getMessage(), e);
        }
    }

    public List<ScheduleItem> getScheduleItemsBySchedule(Schedule schedule) {
        return scheduleItemRepository.findBySchedule(schedule);
    }

    @Transactional
    public ScheduleItem createScheduleItem(ScheduleItem scheduleItem) {
        return scheduleItemRepository.save(scheduleItem);
    }

    @Transactional
    public ScheduleItem updateScheduleItem(ScheduleItem scheduleItem) {
        return scheduleItemRepository.save(scheduleItem);
    }

    @Transactional
    public void deleteScheduleItem(Long id) {
        scheduleItemRepository.deleteById(id);
    }
}