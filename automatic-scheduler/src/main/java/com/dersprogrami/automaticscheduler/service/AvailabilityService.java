package com.dersprogrami.automaticscheduler.service;

import com.dersprogrami.automaticscheduler.model.Availability;
import com.dersprogrami.automaticscheduler.model.Instructor;
import com.dersprogrami.automaticscheduler.repository.AvailabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;

    public AvailabilityService(AvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    public List<Availability> getAllAvailabilities() {
        return availabilityRepository.findAll();
    }

    public Optional<Availability> getAvailabilityById(Long id) {
        return availabilityRepository.findById(id);
    }

    public List<Availability> getAvailabilitiesByInstructor(Instructor instructor) {
        return availabilityRepository.findByInstructor(instructor);
    }

    public List<Availability> getAvailabilitiesByInstructorAndDay(Instructor instructor, Availability.DayOfWeek dayOfWeek) {
        return availabilityRepository.findByInstructorAndDayOfWeek(instructor, dayOfWeek);
    }

    @Transactional
    public Availability createAvailability(Availability availability) {
        return availabilityRepository.save(availability);
    }

    @Transactional
    public Availability updateAvailability(Availability availability) {
        return availabilityRepository.save(availability);
    }

    @Transactional
    public void deleteAvailability(Long id) {
        availabilityRepository.deleteById(id);
    }

    @Transactional
    public void deleteAllAvailabilitiesByInstructor(Instructor instructor) {
        List<Availability> availabilities = availabilityRepository.findByInstructor(instructor);
        availabilityRepository.deleteAll(availabilities);
    }

    /**
     * Verilen zaman aralığında eğitimcinin müsaitlik durumunu kontrol eder
     * @param instructor Eğitimci
     * @param dayOfWeek Haftanın günü
     * @param startHour Başlangıç saati
     * @param endHour Bitiş saati
     * @return Eğitimci müsaitse true, değilse false
     */
    public boolean isInstructorAvailable(Instructor instructor, Availability.DayOfWeek dayOfWeek, int startHour, int endHour) {
        List<Availability> availabilities = getAvailabilitiesByInstructorAndDay(instructor, dayOfWeek);
        
        for (Availability availability : availabilities) {
            if (startHour >= availability.getStartHour() && endHour <= availability.getEndHour()) {
                return true;
            }
        }
        
        return false;
    }

    @Transactional
    public Availability addAvailabilityForInstructor(Instructor instructor, Availability.DayOfWeek dayOfWeek, int startHour, int endHour) {
        Availability availability = new Availability();
        availability.setInstructor(instructor);
        availability.setDayOfWeek(dayOfWeek);
        availability.setStartHour(startHour);
        availability.setEndHour(endHour);
        
        return availabilityRepository.save(availability);
    }
}