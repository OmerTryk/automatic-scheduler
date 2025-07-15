package com.dersprogrami.automaticscheduler.helper;

import com.dersprogrami.automaticscheduler.model.Availability;
import com.dersprogrami.automaticscheduler.model.ScheduleItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Component
public class ScheduleLookupHelper {
    
    // Algoritma saatleri ve görünen saatler arasındaki eşleştirmeler
    private static final Map<Integer, String> TIME_DISPLAY_MAP = new HashMap<>();
    
    // Başlangıç saatleri için görünüm eşleştirmesi
    static {
        TIME_DISPLAY_MAP.put(8, "09:20-10:05");
        TIME_DISPLAY_MAP.put(9, "10:10-10:55");
        TIME_DISPLAY_MAP.put(10, "11:00-11:45");
        TIME_DISPLAY_MAP.put(11, "11:50-12:35");
        TIME_DISPLAY_MAP.put(12, "12:40-13:25"); // Öğle arası
        TIME_DISPLAY_MAP.put(13, "13:30-14:15");
        TIME_DISPLAY_MAP.put(14, "14:20-15:05");
        TIME_DISPLAY_MAP.put(15, "15:10-15:55");
        TIME_DISPLAY_MAP.put(16, "16:00-16:45");
    }
    
    /**
     * Algoritma saatini görüntüleme saatine dönüştürür
     * 
     * @param hour Algoritma saati (8, 9, 10, ...)
     * @return Görüntüleme saati (09:20-10:05, ...)
     */
    public String formatTimeForDisplay(Integer hour) {
        if (hour == null) {
            return "";
        }
        
        return TIME_DISPLAY_MAP.getOrDefault(hour, hour + ":00-" + (hour+1) + ":00");
    }
    
    /**
     * Program öğesinin saatini görüntüleme formatında döndürür
     * 
     * @param item Program öğesi
     * @return Görüntüleme saati (09:20-10:05, ...)
     */
    public String getFormattedTime(ScheduleItem item) {
        if (item == null || item.getStartHour() == null) {
            return "";
        }
        
        return formatTimeForDisplay(item.getStartHour());
    }
    
    /**
     * Belirli bir gün ve saatte programdaki dersi bulur
     * 
     * @param scheduleItems Ders programı öğe listesi
     * @param day Gün
     * @param hour Saat
     * @return Bulunan program öğesi, yoksa null
     */
    public ScheduleItem findScheduleItem(List<ScheduleItem> scheduleItems, Availability.DayOfWeek day, Integer hour) {
        if (scheduleItems == null || day == null || hour == null) {
            return null;
        }
        
        for (ScheduleItem item : scheduleItems) {
            if (item.getDayOfWeek() != null && 
                item.getDayOfWeek().equals(day) && 
                item.getStartHour() != null && 
                item.getStartHour().equals(hour)) {
                return item;
            }
        }
        
        return null;
    }
}