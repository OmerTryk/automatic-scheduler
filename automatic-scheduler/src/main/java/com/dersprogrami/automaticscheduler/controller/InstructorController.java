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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/instructor")
public class InstructorController {

    private final UserService userService;
    private final InstructorService instructorService;
    private final CourseService courseService;
    private final AvailabilityService availabilityService;
    private final ScheduleService scheduleService;
    private final MessageService messageService; // Mesaj Servisi eklendi

    public InstructorController(UserService userService,
                             InstructorService instructorService,
                             CourseService courseService,
                             AvailabilityService availabilityService,
                             ScheduleService scheduleService,
                             MessageService messageService) {
        this.userService = userService;
        this.instructorService = instructorService;
        this.courseService = courseService;
        this.availabilityService = availabilityService;
        this.scheduleService = scheduleService;
        this.messageService = messageService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        Optional<Instructor> instructor = instructorService.getInstructorByUser(currentUser);
        
        if (instructor.isEmpty()) {
            model.addAttribute("error", "Henüz eğitimci olarak atanmamışsınız. Lütfen bölüm başkanınızla iletişime geçin.");
            return "instructor/dashboard";
        }
        
        Instructor currentInstructor = instructor.get();
        Department department = currentInstructor.getDepartment();
        
        if (department == null) {
            model.addAttribute("error", "Henüz bir bölüme atanmamışsınız. Lütfen bölüm başkanınızla iletişime geçin.");
            return "instructor/dashboard";
        }
        
        model.addAttribute("instructor", currentInstructor);
        model.addAttribute("department", department);
        
        List<Course> courses = courseService.getCoursesByInstructor(currentInstructor);
        model.addAttribute("courses", courses);
        
        List<Availability> availabilities = availabilityService.getAvailabilitiesByInstructor(currentInstructor);
        model.addAttribute("availabilities", availabilities);
        
        Optional<Schedule> activeSchedule = scheduleService.getActiveScheduleForDepartment(department);
        activeSchedule.ifPresent(schedule -> {
            List<ScheduleItem> scheduleItems = scheduleService.getScheduleItemsBySchedule(schedule);
            List<ScheduleItem> instructorItems = scheduleItems.stream()
                    .filter(item -> item.getInstructor().equals(currentInstructor))
                    .toList();
            
            model.addAttribute("scheduleItems", instructorItems);
            model.addAttribute("schedule", schedule);
        });
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(currentUser);
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "instructor/dashboard";
    }

    @GetMapping("/courses")
    public String listCourses(Model model) {
        Instructor instructor = getCurrentInstructor();
        if (instructor == null) {
            return "redirect:/instructor/dashboard";
        }
        
        List<Course> courses = courseService.getCoursesByInstructor(instructor);
        model.addAttribute("courses", courses);
        model.addAttribute("instructor", instructor);
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(getCurrentUser());
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "instructor/courses";
    }

    @GetMapping("/availability")
    public String availability(Model model) {
        Instructor instructor = getCurrentInstructor();
        if (instructor == null) {
            return "redirect:/instructor/dashboard";
        }
        
        List<Availability> availabilities = availabilityService.getAvailabilitiesByInstructor(instructor);
        
        model.addAttribute("instructor", instructor);
        model.addAttribute("availabilities", availabilities);
        model.addAttribute("days", Availability.DayOfWeek.values());
        model.addAttribute("hours", generateHoursList());
        
        // Oluşturulan kullanılabilirlik matrisini ekleyin
        Map<Availability.DayOfWeek, Map<Integer, Boolean>> availabilityMatrix = createAvailabilityMatrix(availabilities);
        model.addAttribute("availabilityMatrix", availabilityMatrix);
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(getCurrentUser());
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "instructor/availability";
    }

    @GetMapping("/availability/new")
    public String newAvailabilityForm(Model model) {
        Instructor instructor = getCurrentInstructor();
        if (instructor == null) {
            return "redirect:/instructor/dashboard";
        }
        
        model.addAttribute("availability", new Availability());
        model.addAttribute("instructor", instructor);
        model.addAttribute("days", Availability.DayOfWeek.values());
        model.addAttribute("hours", generateHoursList());
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(getCurrentUser());
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "instructor/availability-form";
    }

    @PostMapping("/availability/save")
    public String saveAvailability(@ModelAttribute("availability") @Valid Availability availability,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "instructor/availability-form";
        }
        
        Instructor instructor = getCurrentInstructor();
        if (instructor == null) {
            redirectAttributes.addFlashAttribute("error", "Eğitimci bulunamadı");
            return "redirect:/instructor/dashboard";
        }
        
        if (availability.getStartHour() >= availability.getEndHour()) {
            redirectAttributes.addFlashAttribute("error", "Başlangıç saati bitiş saatinden önce olmalıdır");
            return "redirect:/instructor/availability/new";
        }
        
        List<Availability> existingAvailabilities = availabilityService.getAvailabilitiesByInstructorAndDay(
                instructor, availability.getDayOfWeek());
        
        for (Availability existing : existingAvailabilities) {
            if (existing.getId() != null && existing.getId().equals(availability.getId())) {
                continue;
            }
            
            if (!(availability.getEndHour() <= existing.getStartHour() || 
                 availability.getStartHour() >= existing.getEndHour())) {
                
                redirectAttributes.addFlashAttribute("error", "Bu zaman aralığında başka bir müsaitlik kaydınız bulunmaktadır");
                return "redirect:/instructor/availability/new";
            }
        }
        
        availability.setInstructor(instructor);
        availabilityService.createAvailability(availability);
        
        redirectAttributes.addFlashAttribute("success", "Müsaitlik başarıyla kaydedildi");
        return "redirect:/instructor/availability";
    }

    @GetMapping("/availability/edit/{id}")
    public String editAvailabilityForm(@PathVariable Long id, 
                                    Model model, 
                                    RedirectAttributes redirectAttributes) {
        
        Instructor instructor = getCurrentInstructor();
        if (instructor == null) {
            redirectAttributes.addFlashAttribute("error", "Eğitimci bulunamadı");
            return "redirect:/instructor/dashboard";
        }
        
        Optional<Availability> availability = availabilityService.getAvailabilityById(id);
        
        if (availability.isEmpty() || !availability.get().getInstructor().equals(instructor)) {
            redirectAttributes.addFlashAttribute("error", "Müsaitlik kaydı bulunamadı");
            return "redirect:/instructor/availability";
        }
        
        model.addAttribute("availability", availability.get());
        model.addAttribute("instructor", instructor);
        model.addAttribute("days", Availability.DayOfWeek.values());
        model.addAttribute("hours", generateHoursList());
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(getCurrentUser());
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "instructor/availability-form";
    }

    @GetMapping("/availability/delete/{id}")
    public String deleteAvailability(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Instructor instructor = getCurrentInstructor();
        if (instructor == null) {
            redirectAttributes.addFlashAttribute("error", "Eğitimci bulunamadı");
            return "redirect:/instructor/dashboard";
        }
        
        Optional<Availability> availability = availabilityService.getAvailabilityById(id);
        
        if (availability.isEmpty() || !availability.get().getInstructor().equals(instructor)) {
            redirectAttributes.addFlashAttribute("error", "Müsaitlik kaydı bulunamadı");
            return "redirect:/instructor/availability";
        }
        
        availabilityService.deleteAvailability(id);
        
        redirectAttributes.addFlashAttribute("success", "Müsaitlik kaydı başarıyla silindi");
        return "redirect:/instructor/availability";
    }

    @GetMapping("/schedule")
    public String viewSchedule(Model model) {
        Instructor instructor = getCurrentInstructor();
        if (instructor == null) {
            return "redirect:/instructor/dashboard";
        }
        
        Department department = instructor.getDepartment();
        if (department == null) {
            model.addAttribute("error", "Bölüm bulunamadı");
            return "instructor/schedule";
        }
        
        Optional<Schedule> activeSchedule = scheduleService.getActiveScheduleForDepartment(department);
        
        if (activeSchedule.isEmpty()) {
            model.addAttribute("error", "Bölümünüz için aktif bir ders programı bulunmamaktadır");
            return "instructor/schedule";
        }
        
        Schedule schedule = activeSchedule.get();
        List<ScheduleItem> allScheduleItems = scheduleService.getScheduleItemsBySchedule(schedule);
        
        List<ScheduleItem> instructorItems = allScheduleItems.stream()
                .filter(item -> item.getInstructor().equals(instructor))
                .collect(Collectors.toList());
        
        // Tablodaki hücreler için önceden hazırlanmış bir veri yapısı oluştur
        Map<Availability.DayOfWeek, Map<Integer, ScheduleItem>> scheduleGrid = new HashMap<>();
        
        // Günler için haritayı oluştur
        for (Availability.DayOfWeek day : Availability.DayOfWeek.values()) {
            scheduleGrid.put(day, new HashMap<>());
        }
        
        // Ders öğelerini grid'e yerleştir
        for (ScheduleItem item : instructorItems) {
            Availability.DayOfWeek day = item.getDayOfWeek();
            Integer hour = item.getStartHour();
            
            if (day != null && hour != null) {
                scheduleGrid.get(day).put(hour, item);
            }
        }
        
        model.addAttribute("schedule", schedule);
        model.addAttribute("scheduleItems", instructorItems);
        model.addAttribute("scheduleGrid", scheduleGrid);
        model.addAttribute("days", Availability.DayOfWeek.values());
        model.addAttribute("hours", generateHoursList());
        
        // Okunmamış mesaj sayısını ekle
        long unreadMessageCount = messageService.getUnreadMessageCount(getCurrentUser());
        model.addAttribute("unreadMessageCount", unreadMessageCount);
        
        return "instructor/schedule";
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

    private Instructor getCurrentInstructor() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return null;
        }
        
        Optional<Instructor> instructor = instructorService.getInstructorByUser(currentUser);
        return instructor.orElse(null);
    }
}