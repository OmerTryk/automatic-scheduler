package com.dersprogrami.automaticscheduler.controller;

import com.dersprogrami.automaticscheduler.model.*;
import com.dersprogrami.automaticscheduler.service.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/department-head")
public class DepartmentHeadController {

    private final UserService userService;
    private final DepartmentService departmentService;
    private final CourseService courseService;
    private final InstructorService instructorService;
    private final ScheduleService scheduleService;
    private final AvailabilityService availabilityService;
    private final MessageService messageService; // Mesaj servisi eklendi

    public DepartmentHeadController(UserService userService, 
                                  DepartmentService departmentService,
                                  CourseService courseService,
                                  InstructorService instructorService,
                                  ScheduleService scheduleService,
                                  AvailabilityService availabilityService,
                                  MessageService messageService) {
        this.userService = userService;
        this.departmentService = departmentService;
        this.courseService = courseService;
        this.instructorService = instructorService;
        this.scheduleService = scheduleService;
        this.availabilityService = availabilityService;
        this.messageService = messageService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        Optional<DepartmentHead> departmentHead = departmentService.getDepartmentHeadByUser(currentUser);
        
        if (departmentHead.isEmpty()) {
            model.addAttribute("error", "Henüz bir bölüme atanmamışsınız. Lütfen sistem yöneticisiyle iletişime geçin.");
            return "department-head/dashboard";
        }
        
        Department department = departmentHead.get().getDepartment();
        model.addAttribute("department", department);
        
        List<Course> courses = courseService.getCoursesByDepartment(department);
        List<Instructor> instructors = instructorService.getInstructorsByDepartment(department);
        
        model.addAttribute("courses", courses);
        model.addAttribute("instructors", instructors);
        
        Optional<Schedule> activeSchedule = scheduleService.getActiveScheduleForDepartment(department);
        activeSchedule.ifPresent(schedule -> model.addAttribute("activeSchedule", schedule));
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(currentUser);
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "department-head/dashboard";
    }

    @GetMapping("/courses")
    public String listCourses(Model model) {
        Department department = getCurrentDepartment();
        if (department == null) {
            return "redirect:/department-head/dashboard";
        }
        
        List<Course> courses = courseService.getCoursesByDepartment(department);
        model.addAttribute("courses", courses);
        model.addAttribute("department", department);
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(getCurrentUser());
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "department-head/courses";
    }

    @GetMapping("/courses/new")
    public String newCourseForm(Model model) {
        Department department = getCurrentDepartment();
        if (department == null) {
            return "redirect:/department-head/dashboard";
        }
        
        model.addAttribute("course", new Course());
        model.addAttribute("department", department);
        model.addAttribute("instructors", instructorService.getInstructorsByDepartment(department));
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(getCurrentUser());
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "department-head/course-form";
    }

    @PostMapping("/courses/save")
public String saveCourse(@ModelAttribute("course") @Valid Course course, 
                        BindingResult result,
                        @RequestParam(required = false) Long instructorId,
                        RedirectAttributes redirectAttributes) {
    
    if (result.hasErrors()) {
        return "department-head/course-form";
    }
    
    Department department = getCurrentDepartment();
    if (department == null) {
        redirectAttributes.addFlashAttribute("error", "Bölüm bulunamadı");
        return "redirect:/department-head/dashboard";
    }
    
    course.setDepartment(department);
    
    if (instructorId != null) {
        Optional<Instructor> instructor = instructorService.getInstructorById(instructorId);
        instructor.ifPresent(i -> {
            course.getInstructors().clear();
            course.getInstructors().add(i);
        });
    }
    
    courseService.createCourse(course);
    
    redirectAttributes.addFlashAttribute("success", "Ders başarıyla kaydedildi");
    return "redirect:/department-head/courses";
}

    @GetMapping("/courses/edit/{id}")
    public String editCourseForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Department department = getCurrentDepartment();
        if (department == null) {
            redirectAttributes.addFlashAttribute("error", "Bölüm bulunamadı");
            return "redirect:/department-head/dashboard";
        }
        
        Optional<Course> course = courseService.getCourseById(id);
        if (course.isEmpty() || !course.get().getDepartment().equals(department)) {
            redirectAttributes.addFlashAttribute("error", "Ders bulunamadı");
            return "redirect:/department-head/courses";
        }
        
        model.addAttribute("course", course.get());
        model.addAttribute("department", department);
        model.addAttribute("instructors", instructorService.getInstructorsByDepartment(department));
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(getCurrentUser());
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "department-head/course-form";
    }

    @GetMapping("/courses/delete/{id}")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Department department = getCurrentDepartment();
        if (department == null) {
            redirectAttributes.addFlashAttribute("error", "Bölüm bulunamadı");
            return "redirect:/department-head/dashboard";
        }
        
        Optional<Course> course = courseService.getCourseById(id);
        if (course.isEmpty() || !course.get().getDepartment().equals(department)) {
            redirectAttributes.addFlashAttribute("error", "Ders bulunamadı");
            return "redirect:/department-head/courses";
        }
        
        courseService.deleteCourse(id);
        
        redirectAttributes.addFlashAttribute("success", "Ders başarıyla silindi");
        return "redirect:/department-head/courses";
    }

    @GetMapping("/instructors")
    public String listInstructors(Model model) {
        Department department = getCurrentDepartment();
        if (department == null) {
            return "redirect:/department-head/dashboard";
        }
        
        List<Instructor> instructors = instructorService.getInstructorsByDepartment(department);
        model.addAttribute("instructors", instructors);
        model.addAttribute("department", department);
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(getCurrentUser());
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "department-head/instructors";
    }

    @GetMapping("/instructors/{id}/courses")
    public String instructorCourses(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Department department = getCurrentDepartment();
        if (department == null) {
            redirectAttributes.addFlashAttribute("error", "Bölüm bulunamadı");
            return "redirect:/department-head/dashboard";
        }
        
        Optional<Instructor> instructor = instructorService.getInstructorById(id);
        if (instructor.isEmpty() || instructor.get().getDepartment() == null || 
            !instructor.get().getDepartment().equals(department)) {
            
            redirectAttributes.addFlashAttribute("error", "Eğitimci bulunamadı");
            return "redirect:/department-head/instructors";
        }
        
        List<Course> allCourses = courseService.getCoursesByDepartment(department);
        List<Course> instructorCourses = courseService.getCoursesByInstructor(instructor.get());
        
        model.addAttribute("instructor", instructor.get());
        model.addAttribute("allCourses", allCourses);
        model.addAttribute("instructorCourses", instructorCourses);
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(getCurrentUser());
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "department-head/instructor-courses";
    }

    @PostMapping("/instructors/{instructorId}/courses/assign")
    public String assignCourseToInstructor(@PathVariable Long instructorId,
                                        @RequestParam Long courseId,
                                        RedirectAttributes redirectAttributes) {
        
        Department department = getCurrentDepartment();
        if (department == null) {
            redirectAttributes.addFlashAttribute("error", "Bölüm bulunamadı");
            return "redirect:/department-head/dashboard";
        }
        
        Optional<Instructor> instructor = instructorService.getInstructorById(instructorId);
        Optional<Course> course = courseService.getCourseById(courseId);
        
        if (instructor.isEmpty() || course.isEmpty() || 
            instructor.get().getDepartment() == null || 
            !instructor.get().getDepartment().equals(department) ||
            !course.get().getDepartment().equals(department)) {
            
            redirectAttributes.addFlashAttribute("error", "Eğitimci veya ders bulunamadı");
            return "redirect:/department-head/instructors";
        }
        
        courseService.assignInstructorToCourse(course.get(), instructor.get());
        
        redirectAttributes.addFlashAttribute("success", "Ders eğitimciye başarıyla atandı");
        return "redirect:/department-head/instructors/" + instructorId + "/courses";
    }

    @PostMapping("/instructors/{instructorId}/courses/remove")
    public String removeCourseFromInstructor(@PathVariable Long instructorId,
                                          @RequestParam Long courseId,
                                          RedirectAttributes redirectAttributes) {
        
        Department department = getCurrentDepartment();
        if (department == null) {
            redirectAttributes.addFlashAttribute("error", "Bölüm bulunamadı");
            return "redirect:/department-head/dashboard";
        }
        
        Optional<Instructor> instructor = instructorService.getInstructorById(instructorId);
        Optional<Course> course = courseService.getCourseById(courseId);
        
        if (instructor.isEmpty() || course.isEmpty() || 
            instructor.get().getDepartment() == null || 
            !instructor.get().getDepartment().equals(department) ||
            !course.get().getDepartment().equals(department)) {
            
            redirectAttributes.addFlashAttribute("error", "Eğitimci veya ders bulunamadı");
            return "redirect:/department-head/instructors";
        }
        
        courseService.removeInstructorFromCourse(course.get(), instructor.get());
        
        redirectAttributes.addFlashAttribute("success", "Ders eğitimciden başarıyla kaldırıldı");
        return "redirect:/department-head/instructors/" + instructorId + "/courses";
    }

    @GetMapping("/schedules")
    public String listSchedules(Model model) {
        Department department = getCurrentDepartment();
        if (department == null) {
            return "redirect:/department-head/dashboard";
        }
        
        List<Schedule> schedules = scheduleService.getSchedulesByDepartment(department);
        Optional<Schedule> activeSchedule = scheduleService.getActiveScheduleForDepartment(department);
        
        model.addAttribute("schedules", schedules);
        model.addAttribute("department", department);
        activeSchedule.ifPresent(schedule -> model.addAttribute("activeSchedule", schedule));
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(getCurrentUser());
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "department-head/schedules";
    }

    @GetMapping("/schedules/new")
    public String newScheduleForm(Model model) {
        Department department = getCurrentDepartment();
        if (department == null) {
            return "redirect:/department-head/dashboard";
        }

        // Yeni program için akademik dönem bilgisini hazırla
        model.addAttribute("department", department);
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(getCurrentUser());
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "department-head/schedule-form";
    }

    @PostMapping("/schedules/generate")
    public String generateSchedule(@RequestParam String academicTerm,
                                 RedirectAttributes redirectAttributes) {
        System.out.println("DepartmentHeadController: Program oluştur butonu tıklandı");
        
        Department department = getCurrentDepartment();
        if (department == null) {
            redirectAttributes.addFlashAttribute("error", "Bölüm bulunamadı");
            return "redirect:/department-head/dashboard";
        }
        
        System.out.println("DepartmentHeadController: Bölüm ID: " + department.getId() + ", Adı: " + department.getName());
        
        // Akademik dönemi kontrol et
        if (academicTerm == null || academicTerm.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Akademik dönem belirtilmeli");
            return "redirect:/department-head/schedules";
        }
        
        System.out.println("DepartmentHeadController: Akademik Dönem: " + academicTerm);
        
        // Bölümde ders olup olmadığını kontrol et
        List<Course> courses = courseService.getCoursesByDepartment(department);
        System.out.println("DepartmentHeadController: Bölümdeki ders sayısı: " + courses.size());
        
        if (courses.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Bölümde kayıtlı ders bulunmamaktadır");
            return "redirect:/department-head/schedules";
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
            redirectAttributes.addFlashAttribute("error", "Hiçbir derse eğitimci atanmamış");
            return "redirect:/department-head/schedules";
        }
        
        // Eğitimcilerin müsaitlik bilgilerini kontrol et
        List<Instructor> instructors = instructorService.getInstructorsByDepartment(department);
        System.out.println("DepartmentHeadController: Bölümdeki eğitimci sayısı: " + instructors.size());
        
        if (instructors.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Bölümde kayıtlı eğitimci bulunmamaktadır");
            return "redirect:/department-head/schedules";
        }
        
        // Müsaitlik bilgilerini kontrol et
        boolean allInstructorsHaveAvailability = true;
        List<String> instructorsWithoutAvailability = new java.util.ArrayList<>();
        
        for (Instructor instructor : instructors) {
            List<Availability> availabilities = availabilityService.getAvailabilitiesByInstructor(instructor);
            if (availabilities.isEmpty()) {
                allInstructorsHaveAvailability = false;
                instructorsWithoutAvailability.add(instructor.getUser().getFullName());
                System.out.println("DepartmentHeadController: Müsaitlik bilgisi olmayan eğitimci: " 
                    + instructor.getUser().getFullName() + " (ID: " + instructor.getId() + ")");
            } else {
                System.out.println("DepartmentHeadController: Eğitimci " 
                    + instructor.getUser().getFullName() + " için " + availabilities.size() + " müsaitlik kaydı bulundu.");
            }
        }
        
        if (!allInstructorsHaveAvailability) {
            String instructorNames = String.join(", ", instructorsWithoutAvailability);
            redirectAttributes.addFlashAttribute("error", "Aşağıdaki eğitimcilerin müsaitlik bilgileri eksik: " + instructorNames);
            return "redirect:/department-head/schedules";
        }
        
        System.out.println("DepartmentHeadController: Tüm kontroller başarılı, program oluşturuluyor...");
        
        // Ders programını genetik algoritma ile oluştur
        try {
            System.out.println("DepartmentHeadController: ScheduleService.generateScheduleForDepartment çağrılıyor...");
            Schedule schedule = scheduleService.generateScheduleForDepartment(department, academicTerm);
            
            // Programı kontrol et
            if (schedule != null && schedule.getScheduleItems() != null && !schedule.getScheduleItems().isEmpty()) {
                scheduleService.setActiveSchedule(schedule);
                System.out.println("DepartmentHeadController: Program başarıyla oluşturuldu! ID: " + schedule.getId());
                redirectAttributes.addFlashAttribute("success", "Ders programı başarıyla oluşturuldu");
            } else {
                System.err.println("DepartmentHeadController: Program boş!");
                redirectAttributes.addFlashAttribute("error", "Ders programı oluşturuldu ancak içi boş");
            }
        } catch (Exception e) {
            System.err.println("DepartmentHeadController: Program oluşturma hatası!");
            e.printStackTrace();
            String errorMessage = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                errorMessage = e.getCause().getMessage();
                System.err.println("DepartmentHeadController: Alt hata nedeni: " + errorMessage);
            }
            redirectAttributes.addFlashAttribute("error", "Ders programı oluşturulurken bir hata oluştu: " + errorMessage);
        }
        
        return "redirect:/department-head/schedules";
    }

    @GetMapping("/schedules/{id}/view")
    public String viewSchedule(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Department department = getCurrentDepartment();
        if (department == null) {
            redirectAttributes.addFlashAttribute("error", "Bölüm bulunamadı");
            return "redirect:/department-head/dashboard";
        }
        
        Optional<Schedule> schedule = scheduleService.getScheduleById(id);
        
        if (schedule.isEmpty() || !schedule.get().getDepartment().equals(department)) {
            redirectAttributes.addFlashAttribute("error", "Ders programı bulunamadı");
            return "redirect:/department-head/schedules";
        }
        
        List<ScheduleItem> scheduleItems = scheduleService.getScheduleItemsBySchedule(schedule.get());
        
        // Tablodaki hücreler için önceden hazırlanmış bir veri yapısı oluştur
        Map<Availability.DayOfWeek, Map<Integer, ScheduleItem>> scheduleGrid = new HashMap<>();
        
        // Günler için haritayı oluştur
        for (Availability.DayOfWeek day : Availability.DayOfWeek.values()) {
            scheduleGrid.put(day, new HashMap<>());
        }
        
        // Ders öğelerini grid'e yerleştir
        for (ScheduleItem item : scheduleItems) {
            Availability.DayOfWeek day = item.getDayOfWeek();
            Integer hour = item.getStartHour();
            
            if (day != null && hour != null) {
                scheduleGrid.get(day).put(hour, item);
            }
        }
        
        model.addAttribute("schedule", schedule.get());
        model.addAttribute("scheduleItems", scheduleItems);
        model.addAttribute("scheduleGrid", scheduleGrid);
        model.addAttribute("department", department);
        model.addAttribute("days", Availability.DayOfWeek.values());
        model.addAttribute("hours", generateHoursList());
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(getCurrentUser());
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "department-head/schedule-view";
    }

    @GetMapping("/schedules/{id}/set-active")
    public String setActiveSchedule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Department department = getCurrentDepartment();
        if (department == null) {
            redirectAttributes.addFlashAttribute("error", "Bölüm bulunamadı");
            return "redirect:/department-head/dashboard";
        }
        
        Optional<Schedule> schedule = scheduleService.getScheduleById(id);
        
        if (schedule.isEmpty() || !schedule.get().getDepartment().equals(department)) {
            redirectAttributes.addFlashAttribute("error", "Ders programı bulunamadı");
            return "redirect:/department-head/schedules";
        }
        
        scheduleService.setActiveSchedule(schedule.get());
        
        redirectAttributes.addFlashAttribute("success", "Ders programı aktif olarak ayarlandı");
        return "redirect:/department-head/schedules";
    }

    @GetMapping("/schedules/{id}/delete")
    public String deleteSchedule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Department department = getCurrentDepartment();
        if (department == null) {
            redirectAttributes.addFlashAttribute("error", "Bölüm bulunamadı");
            return "redirect:/department-head/dashboard";
        }
        
        Optional<Schedule> schedule = scheduleService.getScheduleById(id);
        
        if (schedule.isEmpty() || !schedule.get().getDepartment().equals(department)) {
            redirectAttributes.addFlashAttribute("error", "Ders programı bulunamadı");
            return "redirect:/department-head/schedules";
        }
        
        // Aktif program siliniyorsa uyarı ver
        if (schedule.get().getIsActive()) {
            redirectAttributes.addFlashAttribute("error", "Aktif ders programı silinemez");
            return "redirect:/department-head/schedules";
        }
        
        scheduleService.deleteSchedule(id);
        
        redirectAttributes.addFlashAttribute("success", "Ders programı başarıyla silindi");
        return "redirect:/department-head/schedules";
    }

    @GetMapping("/instructors/{id}/availability")
    public String viewInstructorAvailability(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Department department = getCurrentDepartment();
        if (department == null) {
            redirectAttributes.addFlashAttribute("error", "Bölüm bulunamadı");
            return "redirect:/department-head/dashboard";
        }
        
        Optional<Instructor> instructor = instructorService.getInstructorById(id);
        
        if (instructor.isEmpty() || instructor.get().getDepartment() == null || 
            !instructor.get().getDepartment().equals(department)) {
            
            redirectAttributes.addFlashAttribute("error", "Eğitimci bulunamadı");
            return "redirect:/department-head/instructors";
        }
        
        List<Availability> availabilities = availabilityService.getAvailabilitiesByInstructor(instructor.get());
        
        model.addAttribute("instructor", instructor.get());
        model.addAttribute("availabilities", availabilities);
        model.addAttribute("days", Availability.DayOfWeek.values());
        model.addAttribute("hours", generateHoursList());
        
        // Oluşturulan kullanılabilirlik matrisini ekleyin
        Map<Availability.DayOfWeek, Map<Integer, Boolean>> availabilityMatrix = createAvailabilityMatrix(availabilities);
        model.addAttribute("availabilityMatrix", availabilityMatrix);
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(getCurrentUser());
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "department-head/instructor-availability";
    }

    private Map<Availability.DayOfWeek, Map<Integer, Boolean>> createAvailabilityMatrix(List<Availability> availabilities) {
        Map<Availability.DayOfWeek, Map<Integer, Boolean>> matrix = new HashMap<>();
        
        // Tüm günler için haritaları oluşturun
        for (Availability.DayOfWeek day : Availability.DayOfWeek.values()) {
            Map<Integer, Boolean> dayMap = new HashMap<>();
            for (int hour : generateHoursList()) {
                dayMap.put(hour, false); // Varsayılan olarak müsait değil
            }
            matrix.put(day, dayMap);
        }
        
        // Müsaitlikleri işaretleyin
        for (Availability availability : availabilities) {
            Availability.DayOfWeek day = availability.getDayOfWeek();
            int startHour = availability.getStartHour();
            int endHour = availability.getEndHour();
            
            Map<Integer, Boolean> dayMap = matrix.get(day);
            if (dayMap != null) {
                for (int hour = startHour; hour < endHour; hour++) {
                    dayMap.put(hour, true); // Müsait
                }
            }
        }
        
        return matrix;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        try {
            String username = authentication.getName();
            return userService.getUserByUsername(username);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Integer> generateHoursList() {
        // Saatler 8-16 arası kullanılıyor (algoritma saatleri)
        return List.of(8, 9, 10, 11, 12, 13, 14, 15, 16);
    }

    private Department getCurrentDepartment() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return null;
        }
        
        Optional<DepartmentHead> departmentHead = departmentService.getDepartmentHeadByUser(currentUser);
        return departmentHead.map(DepartmentHead::getDepartment).orElse(null);
    }
}