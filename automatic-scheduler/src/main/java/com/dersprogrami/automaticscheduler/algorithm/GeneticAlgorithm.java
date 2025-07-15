package com.dersprogrami.automaticscheduler.algorithm;

import com.dersprogrami.automaticscheduler.model.*;
import com.dersprogrami.automaticscheduler.repository.ScheduleItemRepository;
import com.dersprogrami.automaticscheduler.repository.ScheduleRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GeneticAlgorithm {

    @Value("${app.genetic.population-size:100}")
    private int POPULATION_SIZE;
    
    @Value("${app.genetic.max-generations:200}")
    private int MAX_GENERATIONS;
    
    @Value("${app.genetic.max-execution-time-ms:60000}")
    private long MAX_EXECUTION_TIME_MS;
    
    private static final double CROSSOVER_RATE = 0.8;
    private static final double MUTATION_RATE = 0.2;
    private static final int TOURNAMENT_SIZE = 5;
    
    private final ScheduleRepository scheduleRepository;
    private final ScheduleItemRepository scheduleItemRepository;
    
    public GeneticAlgorithm(ScheduleRepository scheduleRepository, 
                           ScheduleItemRepository scheduleItemRepository) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleItemRepository = scheduleItemRepository;
    }
    
    private static final int INSTRUCTOR_AVAILABILITY_WEIGHT = 100;
    private static final int INSTRUCTOR_CONFLICT_WEIGHT = 100;
    private static final int ROOM_CONFLICT_WEIGHT = 100;
    private static final int CONSECUTIVE_LECTURES_WEIGHT = 30;
    private static final int LUNCH_BREAK_WEIGHT = 50;
    
    private static final int LUNCH_BREAK_START = 12;
    private static final int LUNCH_BREAK_END = 13;
    
    private static final int WORK_DAY_START = 8;
    private static final int WORK_DAY_END = 17;
   
    private int selectOptimumStartHour(int weeklyHours, int preferredStartHour) {

        if (weeklyHours <= 2) {
            return preferredStartHour; 
        }
        
        // 3-4 saatlik dersler için, öğle arasına denk gelmeyecek bir başlangıç seç
        if (weeklyHours >= 3 && weeklyHours <= 4) {
            // Sabah saatleri için: Dersi sabahın başına koy (8:00)
            if (preferredStartHour < 11) {
                return WORK_DAY_START; // Sabah 8'den başlat, böylece 4 saatlik dersin son saati 12'de biter
            }
            // Öğleden sonra için: Dersi öğleden sonranın başına koy (13:00)
            else {
                return LUNCH_BREAK_END; // Öğleden sonra 13:00'ten başlat, böylece 4 saatlik ders 17:00'de biter
            }
        }
        
        // 5+ saatlik dersler için, birden fazla güne bölünmesi gerekebilir
        // Bu durumda ilk gün için en uygun başlangıç saatini seç
        return WORK_DAY_START; // Varsayılan olarak sabah 8'den başlat
    }
    
    /**
     * Genetik algoritmayı kullanarak ders programı oluşturur
     */
    public Schedule generateSchedule(Department department, List<Course> courses, List<Instructor> instructors, 
                                    List<Availability> availabilities, String academicTerm) {
        // Giriş parametrelerini kontrol et
        if (department == null) {
            throw new IllegalArgumentException("Bölüm bilgisi boş olamaz");
        }
        
        if (courses == null || courses.isEmpty()) {
            throw new IllegalArgumentException("Bölümde ders bulunmamaktadır");
        }
        
        if (instructors == null || instructors.isEmpty()) {
            throw new IllegalArgumentException("Bölümde eğitimci bulunmamaktadır");
        }
        
        if (availabilities == null || availabilities.isEmpty()) {
            throw new IllegalArgumentException("Eğitimcilerin müsaitlik bilgisi bulunmamaktadır");
        }
        
        // Eğitimcilere ders atanmış mı kontrol et
        boolean anyInstructorAssigned = false;
        for (Course course : courses) {
            if (course != null && course.getInstructors() != null && !course.getInstructors().isEmpty()) {
                anyInstructorAssigned = true;
                break;
            }
        }
        
        if (!anyInstructorAssigned) {
            throw new IllegalArgumentException("Hiçbir derse eğitimci atanmamış");
        }
        
        // Başlangıç popülasyonu oluştur
        List<Chromosome> population = initializePopulation(courses, instructors, availabilities);
        
        // Popülasyon oluşturulamadı mı kontrol et
        if (population.isEmpty()) {
            throw new IllegalStateException("Başlangıç popülasyonu oluşturulamadı");
        }
        
        // En iyi kromozomu takip et
        Chromosome bestChromosome = null;
        int bestFitness = -1;
        
        // Maksimum çalışma süresini kontrol etmek için başlangıç zamanı al
        long startTime = System.currentTimeMillis();
        
        try {
            // Maksimum nesil sayısına ulaşana kadar devam et
            for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
                // Çalışma süresini kontrol et
                long currentTime = System.currentTimeMillis();
                if (currentTime - startTime > MAX_EXECUTION_TIME_MS) {
                    System.out.println("Maksimum çalışma süresine ulaşıldı, optimizasyon durduruluyor. Nesil: " + generation);
                    break;
                }
                
                // Popülasyonun fitness değerlerini hesapla
                evaluatePopulation(population, availabilities);
                
                // En iyi kromozomu bul ve güncelle
                Chromosome currentBest = getBestChromosome(population);
                if (currentBest == null || currentBest.getGenes() == null || currentBest.getGenes().isEmpty()) {
                    System.out.println("Uyarı: Nesil " + generation + " için geçerli bir kromozom bulunamadı.");
                    continue;
                }
                
                int currentBestFitness = calculateFitness(currentBest, availabilities);
                
                if (bestChromosome == null || currentBestFitness > bestFitness) {
                    bestChromosome = deepCopy(currentBest);
                    bestFitness = currentBestFitness;
                    
                    // Eğer mükemmel bir çözüm bulunursa erken sonlandır
                    if (isOptimalSolution(bestFitness, courses.size())) {
                        System.out.println("Optimal çözüm bulundu, nesil: " + generation);
                        break;
                    }
                }
                
                // Yeni nesil oluştur
                List<Chromosome> newPopulation = evolvePopulation(population);
                if (newPopulation != null && !newPopulation.isEmpty()) {
                    population = newPopulation;
                } else {
                    System.out.println("Uyarı: Nesil " + generation + " için yeni popülasyon oluşturulamadı.");
                    break;
                }
            }
        } catch (Exception e) {
            // Herhangi bir hata durumunda hata mesajını yazdır ve devam et
            System.err.println("Genetik algoritma hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Eğer tüm nesiller tamamlandıysa, en iyi kromozomu kullan
        if (bestChromosome == null) {
            try {
                bestChromosome = getBestChromosome(population);
            } catch (Exception e) {
                // Eğer en iyi kromozom bulunamazsa yeni bir rastgele kromozom oluştur
                System.err.println("En iyi kromozom bulunamadı, yeni bir kromozom oluşturuluyor: " + e.getMessage());
                bestChromosome = createRandomChromosome(courses, instructors, availabilities);
            }
        }
        
        // Oluşturulan kromozomun geçerli olup olmadığını kontrol et
        if (bestChromosome == null || bestChromosome.getGenes() == null || bestChromosome.getGenes().isEmpty()) {
            throw new IllegalStateException("Genetik algoritma geçerli bir ders programı oluşturamadı");
        }
        
        // En iyi kromozomdan Schedule nesnesi oluştur
        return createScheduleFromChromosome(bestChromosome, department, academicTerm);
    }
    
    /**
     * Başlangıç popülasyonunu rastgele oluşturur
     */
    private List<Chromosome> initializePopulation(List<Course> courses, List<Instructor> instructors, 
                                                 List<Availability> availabilities) {
        List<Chromosome> population = new ArrayList<>();
        
        System.out.println("GA: Başlangıç popülasyonu oluşturuluyor. Population size: " + POPULATION_SIZE);
        System.out.println("GA: Kurs sayısı: " + (courses != null ? courses.size() : 0));
        System.out.println("GA: Eğitimci sayısı: " + (instructors != null ? instructors.size() : 0));
        
        try {
            if (courses == null || courses.isEmpty()) {
                System.err.println("GA: Kurslar listesi boş, popülasyon oluşturulamadı.");
                return population;
            }
            
            if (instructors == null || instructors.isEmpty()) {
                System.err.println("GA: Eğitimciler listesi boş, popülasyon oluşturulamadı.");
                return population;
            }
            
            for (int i = 0; i < POPULATION_SIZE; i++) {
                try {
                    Chromosome chromosome = createRandomChromosome(courses, instructors, availabilities);
                    if (chromosome != null && chromosome.getGenes() != null && !chromosome.getGenes().isEmpty()) {
                        population.add(chromosome);
                        System.out.println("GA: Kromozom #" + i + " oluşturuldu, gen sayısı: " + chromosome.getGenes().size());
                    } else {
                        System.err.println("GA: Kromozom #" + i + " geçersiz, atlanıyor.");
                    }
                } catch (Exception e) {
                    System.err.println("GA: Kromozom #" + i + " oluşturulurken hata: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("GA: Toplam " + population.size() + " kromozom oluşturuldu.");
        } catch (Exception e) {
            System.err.println("GA: Popülasyon oluşturulurken genel hata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return population;
    }
    
    /**
     * Rastgele bir kromozom oluşturur
     */
    private Chromosome createRandomChromosome(List<Course> courses, List<Instructor> instructors,
                                             List<Availability> availabilities) {
        Random random = new Random();
        List<Gene> genes = new ArrayList<>();
        
        System.out.println("GA: createRandomChromosome - Rastgele kromozom oluşturuluyor");
        
        if (courses == null || courses.isEmpty()) {
            System.err.println("GA: createRandomChromosome - Kurslar listesi boş");
            return new Chromosome(genes);
        }
        
        int geneCount = 0;
        int skippedCourses = 0;
        
        // Tüm dersleri ve öğretmenleri yaz
        System.out.println("Mevcut dersler:");
        for (Course course : courses) {
            System.out.println("  - " + course.getName() + " (ID: " + course.getId() + "), "
                    + (course.getInstructors() != null ? course.getInstructors().size() : 0) + " öğretmen");
            if (course.getInstructors() != null) {
                for (Instructor instructor : course.getInstructors()) {
                    System.out.println("    * " + instructor.getUser().getFullName());
                }
            }
        }
        
        for (Course course : courses) {
            // Eğer kurs null ise atla
            if (course == null) {
                System.err.println("GA: createRandomChromosome - Null kurs, atlanıyor");
                skippedCourses++;
                continue;
            }
            
            // Eğitimci kontrolü
            if (course.getInstructors() == null || course.getInstructors().isEmpty()) {
                System.err.println("GA: createRandomChromosome - Kurs için eğitimci yok: " + course.getName());
                skippedCourses++;
                continue;
            }
            
            // Haftalık saat kontrolü
            if (course.getWeeklyHours() == null || course.getWeeklyHours() <= 0) {
                System.err.println("GA: createRandomChromosome - Geçersiz haftalık saat: " + course.getName());
                skippedCourses++;
                continue;
            }
            
            System.out.println("GA: createRandomChromosome - Kurs işleniyor: " + course.getName() + ", Haftalık saat: " + course.getWeeklyHours());
            System.out.println("GA: createRandomChromosome - Eğitimci sayısı: " + course.getInstructors().size());
            
            // Dersin haftalık saatlerini kontrol et
            int weeklyHours = course.getWeeklyHours();
            int remainingHours = weeklyHours;
            
            // Dersin tüm saatleri için tek bir gün ve başlangıç saati seçelim
            Availability.DayOfWeek selectedDay = Availability.DayOfWeek.values()[random.nextInt(Availability.DayOfWeek.values().length)];
            
            // Başlangıç saatini belirle - öğle arasından kaçın
            int startHour;
            if (random.nextBoolean()) {
                // Sabah saatlerini seç (8-9)
                // Daha genel durumda (8-10) olabilir ama 4 saatlik dersler için 8'den başlamak daha uygun
                startHour = 8 + random.nextInt(2); 
            } else {
                // Öğleden sonra saatlerini seç (13-14)
                startHour = 13 + random.nextInt(2);
            }
            
            System.out.println("GA: Ders için seçilen gün: " + selectedDay + ", başlangıç saati: " + startHour);
            
            // Haftalık ders saati için optimum başlangıç saati seçimi
            int optimumStartHour = selectOptimumStartHour(weeklyHours, startHour);
            System.out.println("GA: " + weeklyHours + " saatlik ders için optimum başlangıç saati: " + optimumStartHour);
            
            // Ders bölünmeden tek bir günde ve ardışık saatlerde atanmalı
            Instructor selectedInstructor = null;
            if (course.getInstructors() != null && !course.getInstructors().isEmpty()) {
                List<Instructor> courseInstructors = new ArrayList<>(course.getInstructors());
                int instructorIndex = random.nextInt(courseInstructors.size());
                selectedInstructor = courseInstructors.get(instructorIndex);
            } else {
                System.err.println("GA: createRandomChromosome - Derse atanmış eğitimci yok");
                continue;
            }
            
            if (selectedInstructor == null) {
                System.err.println("GA: createRandomChromosome - Seçilen eğitimci null");
                continue;
            }
            
            // Bu ders için saatleri belirle (hepsi aynı günde ve ardışık)
            for (int hour = 0; hour < weeklyHours; hour++) {
                try {
                    // Dersin bir saati için bir gen oluştur
                    Gene gene = new Gene();
                    gene.setCourse(course);
                    gene.setInstructor(selectedInstructor);
                    gene.setDayOfWeek(selectedDay);
                    
                    // Saati belirle
                    int currentHour = optimumStartHour + hour;
                    
                    // Sınır kontrolü - 17:00'den sonra ders olmayacak
                    if (currentHour < WORK_DAY_END) {
                        gene.setStartHour(currentHour);
                        gene.setEndHour(currentHour + 1);
                        genes.add(gene);
                        geneCount++;
                    
                                System.out.println("GA: Gen oluşturuldu: " + course.getName() + ", " + selectedDay + ", " 
                                        + currentHour + "-" + (currentHour+1));
                            } else {
                                System.out.println("GA: Maksimum saat sınırı (" + WORK_DAY_END + ":00) aşıldı, gen oluşturulmadı: " + 
                                                 course.getName() + ", saat: " + currentHour);
                            }
                } catch (Exception e) {
                    System.err.println("GA: Gen oluşturma hatası: " + e.getMessage());
                    e.printStackTrace();
                    break;
                }
            }
        }
        
        System.out.println("GA: createRandomChromosome - Toplam " + geneCount + " gen oluşturuldu, " + skippedCourses + " kurs atlandı.");
        return new Chromosome(genes);
    }
    
    /**
     * Popülasyondaki her kromozomun fitness değerini hesaplar
     */
    private void evaluatePopulation(List<Chromosome> population, List<Availability> availabilities) {
        for (Chromosome chromosome : population) {
            chromosome.setFitness(calculateFitness(chromosome, availabilities));
        }
    }
    
    /**
     * Bir kromozomun fitness değerini hesaplar
     * Daha yüksek değer daha iyi kromozom anlamına gelir
     */
    private int calculateFitness(Chromosome chromosome, List<Availability> availabilities) {
        int fitness = 0;
        List<Gene> genes = chromosome.getGenes();
        
        // Eğitimci müsaitlik kontrolü
        fitness += evaluateInstructorAvailability(genes, availabilities) * INSTRUCTOR_AVAILABILITY_WEIGHT;
        
        // Eğitimci çakışma kontrolü
        fitness -= countInstructorConflicts(genes) * INSTRUCTOR_CONFLICT_WEIGHT;
        
        // Sınıf çakışma kontrolü
        fitness -= countRoomConflicts(genes) * ROOM_CONFLICT_WEIGHT;
        
        // Ardışık dersler için bonus
        fitness += countConsecutiveLectures(genes) * CONSECUTIVE_LECTURES_WEIGHT;
        
        // Öğle arası için bonus
        fitness += evaluateLunchBreaks(genes) * LUNCH_BREAK_WEIGHT;
        
        return fitness;
    }
    
    /**
     * Eğitimcilerin müsaitlik durumlarına göre değerlendirme yapar
     */
    private int evaluateInstructorAvailability(List<Gene> genes, List<Availability> availabilities) {
        int availabilityScore = 0;
        
        // Eğer genes veya availabilities null ise sıfır döndür
        if (genes == null || genes.isEmpty() || availabilities == null || availabilities.isEmpty()) {
            System.out.println("GA: evaluateInstructorAvailability - genes veya availabilities boş/null");
            return 0;
        }
        
        System.out.println("GA: evaluateInstructorAvailability - " + genes.size() + " gen ve " + availabilities.size() + " müsaitlik kaydı değerlendiriliyor.");
        
        // Her gene için eğitimcinin müsaitlik durumunu kontrol et
        for (Gene gene : genes) {
            // Eğer herhangi bir alan null ise, bu geni atla
            if (gene == null || gene.getInstructor() == null || gene.getDayOfWeek() == null || 
                gene.getStartHour() == 0 || gene.getEndHour() == 0) {
                System.out.println("GA: evaluateInstructorAvailability - geçersiz gen atlandı");
                continue;
            }
            
            Instructor instructor = gene.getInstructor();
            Availability.DayOfWeek day = gene.getDayOfWeek();
            int startHour = gene.getStartHour();
            int endHour = gene.getEndHour();
            
            // Debug bilgileri
            System.out.println("GA: Eğitimci " + instructor.getId() + ", gün " + day + ", saat " + startHour + "-" + endHour);
            
            // Bu eğitimcinin müsaitlik listesini al
            List<Availability> instructorAvailabilities = availabilities.stream()
                .filter(a -> a != null && a.getInstructor() != null && 
                       a.getInstructor().getId() != null && instructor.getId() != null &&
                       a.getInstructor().getId().equals(instructor.getId()) && 
                       a.getDayOfWeek() == day)
                .collect(Collectors.toList());
            
            System.out.println("GA: Eğitimci için " + instructorAvailabilities.size() + " müsaitlik kaydı bulundu.");
            
            // Eğitmenci için bu zaman diliminde müsaitlik var mı kontrol et
            boolean isAvailable = false;
            for (Availability availability : instructorAvailabilities) {
                if (availability.getStartHour() != null && availability.getEndHour() != null &&
                    startHour >= availability.getStartHour() && endHour <= availability.getEndHour()) {
                    isAvailable = true;
                    System.out.println("GA: Eğitimci müsait - saat " + availability.getStartHour() + "-" + availability.getEndHour());
                    break;
                }
            }
            
            if (isAvailable) {
                availabilityScore++;
            } else {
                System.out.println("GA: Eğitimci müsait değil!");
            }
        }
        
        System.out.println("GA: Toplam müsaitlik skoru: " + availabilityScore + " / " + genes.size());
        return availabilityScore;
    }
    
    /**
     * Eğitimci çakışmalarını sayar - aynı eğitimcinin aynı zamanda iki farklı ders vermesi
     */
    private int countInstructorConflicts(List<Gene> genes) {
        if (genes == null || genes.isEmpty()) {
            return 0;
        }
        
        int conflicts = 0;
        
        try {
            for (int i = 0; i < genes.size(); i++) {
                Gene gene1 = genes.get(i);
                if (gene1 == null || gene1.getInstructor() == null || gene1.getDayOfWeek() == null || 
                    gene1.getStartHour() == 0 || gene1.getEndHour() == 0) {
                    continue;
                }
                
                for (int j = i + 1; j < genes.size(); j++) {
                    Gene gene2 = genes.get(j);
                    if (gene2 == null || gene2.getInstructor() == null || gene2.getDayOfWeek() == null || 
                        gene2.getStartHour() == 0 || gene2.getEndHour() == 0) {
                        continue;
                    }
                    
                    // Eğitimcilerin ID'lerini karşılaştır (equals metodu yerine)
                    boolean sameInstructor = gene1.getInstructor().getId() != null && 
                                            gene2.getInstructor().getId() != null && 
                                            gene1.getInstructor().getId().equals(gene2.getInstructor().getId());
                                            
                    // Aynı eğitimci, aynı gün ve saatlerde çakışma var mı
                    if (sameInstructor && 
                        gene1.getDayOfWeek() == gene2.getDayOfWeek() && 
                        !(gene1.getEndHour() <= gene2.getStartHour() || gene1.getStartHour() >= gene2.getEndHour())) {
                        conflicts++;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Eğitimci çakışma kontrolü hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        return conflicts;
    }
    
    /**
     * Sınıf çakışmalarını sayar - aynı sınıfta aynı zamanda iki farklı ders olması
     */
    private int countRoomConflicts(List<Gene> genes) {
        if (genes == null || genes.isEmpty()) {
            return 0;
        }
        
        int conflicts = 0;
        
        try {
            for (int i = 0; i < genes.size(); i++) {
                Gene gene1 = genes.get(i);
                if (gene1 == null || gene1.getClassroom() == null || gene1.getDayOfWeek() == null || 
                    gene1.getStartHour() == 0 || gene1.getEndHour() == 0) {
                    continue;
                }
                
                for (int j = i + 1; j < genes.size(); j++) {
                    Gene gene2 = genes.get(j);
                    if (gene2 == null || gene2.getClassroom() == null || gene2.getDayOfWeek() == null || 
                        gene2.getStartHour() == 0 || gene2.getEndHour() == 0) {
                        continue;
                    }
                    
                    // Aynı sınıf, aynı gün ve saatlerde çakışma var mı
                    if (gene1.getClassroom().equals(gene2.getClassroom()) && 
                        gene1.getDayOfWeek() == gene2.getDayOfWeek() && 
                        !(gene1.getEndHour() <= gene2.getStartHour() || gene1.getStartHour() >= gene2.getEndHour())) {
                        conflicts++;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Sınıf çakışma kontrolü hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        return conflicts;
    }
    
    /**
     * Ardışık dersleri sayar ve aynı dersin bölümlerinin aynı günde olmasını teşvik eder
     */
    private int countConsecutiveLectures(List<Gene> genes) {
        if (genes == null || genes.isEmpty()) {
            return 0;
        }
        
        int consecutiveCount = 0;
        
        try {
            // 1. Genleri eğitimci ve güne göre grupla
            Map<Long, Map<Availability.DayOfWeek, List<Gene>>> instructorDayGenes = new HashMap<>();
            
            for (Gene gene : genes) {
                if (gene == null || gene.getInstructor() == null || gene.getInstructor().getId() == null || 
                    gene.getDayOfWeek() == null || gene.getStartHour() == 0 || gene.getEndHour() == 0) {
                    continue;
                }
                
                Long instructorId = gene.getInstructor().getId();
                Availability.DayOfWeek day = gene.getDayOfWeek();
                
                instructorDayGenes.putIfAbsent(instructorId, new HashMap<>());
                instructorDayGenes.get(instructorId).putIfAbsent(day, new ArrayList<>());
                instructorDayGenes.get(instructorId).get(day).add(gene);
            }
            
            // Her eğitimci ve gün için ardışık dersleri kontrol et
            for (Map<Availability.DayOfWeek, List<Gene>> dayGenes : instructorDayGenes.values()) {
                for (List<Gene> dayGeneList : dayGenes.values()) {
                    if (dayGeneList.size() < 2) continue;
                    
                    // Genleri başlangıç saatine göre sırala
                    dayGeneList.sort(Comparator.comparing(Gene::getStartHour));
                    
                    // Ardışık dersleri kontrol et
                    for (int i = 0; i < dayGeneList.size() - 1; i++) {
                        Gene gene1 = dayGeneList.get(i);
                        Gene gene2 = dayGeneList.get(i + 1);
                        
                        if (gene1 != null && gene2 != null && 
                            gene1.getEndHour() != 0 && gene2.getStartHour() != 0 &&
                            gene1.getEndHour() == gene2.getStartHour()) {
                            consecutiveCount++;
                        }
                    }
                }
            }
            
            // 2. Genleri derse göre grupla ve aynı dersin parçalarının aynı günde olmasını ödüllendir
            Map<Long, Map<Availability.DayOfWeek, List<Gene>>> courseDayGenes = new HashMap<>();
            
            for (Gene gene : genes) {
                if (gene == null || gene.getCourse() == null || gene.getCourse().getId() == null || 
                    gene.getDayOfWeek() == null) {
                    continue;
                }
                
                Long courseId = gene.getCourse().getId();
                Availability.DayOfWeek day = gene.getDayOfWeek();
                
                courseDayGenes.putIfAbsent(courseId, new HashMap<>());
                courseDayGenes.get(courseId).putIfAbsent(day, new ArrayList<>());
                courseDayGenes.get(courseId).get(day).add(gene);
            }
            
            // Her ders için günlere göre grupla
            for (Long courseId : courseDayGenes.keySet()) {
                Map<Availability.DayOfWeek, List<Gene>> dayGeneSets = courseDayGenes.get(courseId);
                
                // Dersin tüm saatlerini hesapla
                int totalHours = 0;
                for (List<Gene> dayGenes : dayGeneSets.values()) {
                    totalHours += dayGenes.size();
                }
                
                // 3+ saatlik dersler için aynı günde olma bonusu
                if (totalHours >= 3) {
                    for (List<Gene> dayGenes : dayGeneSets.values()) {
                        if (dayGenes.size() >= 3) {
                            // Genleri başlangıç saatine göre sırala
                            dayGenes.sort(Comparator.comparing(Gene::getStartHour));
                            
                            // Ardışık saatlerin sayısını kontrol et
                            int consecutiveHours = 1;
                            for (int i = 0; i < dayGenes.size() - 1; i++) {
                                Gene gene1 = dayGenes.get(i);
                                Gene gene2 = dayGenes.get(i + 1);
                                
                                if (gene1 != null && gene2 != null && 
                                    gene1.getEndHour() != 0 && gene2.getStartHour() != 0 &&
                                    gene1.getEndHour() == gene2.getStartHour()) {
                                    consecutiveHours++;
                                } else {
                                    // Ardışık olmayan saat bulundu, sayacı resetle
                                    consecutiveHours = 1;
                                }
                            }
                            
                            // 3 veya daha fazla ardışık saat varsa büyük bonus ver
                            if (consecutiveHours >= 3) {
                                consecutiveCount += consecutiveHours * 3;
                            }
                            
                            // Eğer tüm ders saatleri aynı gündeyse ekstra bonus ver
                            if (dayGenes.size() == totalHours) {
                                consecutiveCount += totalHours * 2;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ardışık ders kontrolü hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        return consecutiveCount;
    }
    
    /**
     * Öğle arası değerlendirmesi yapar - Öğrenci ders programlarında öğle saatinde (11-13 arası) boş zaman olması
     */
    private int evaluateLunchBreaks(List<Gene> genes) {
        if (genes == null || genes.isEmpty()) {
            return 0;
        }
        
        int lunchBreakScore = 0;
        
        try {
            // Derse göre ders programlarını grupla (aynı ders farklı gruplara verildiğinde)
            Map<Long, Map<Availability.DayOfWeek, List<Gene>>> courseDayGenes = new HashMap<>();
            
            for (Gene gene : genes) {
                if (gene == null || gene.getCourse() == null || gene.getCourse().getId() == null || 
                    gene.getDayOfWeek() == null || gene.getStartHour() == 0 || gene.getEndHour() == 0) {
                    continue;
                }
                
                Long courseId = gene.getCourse().getId();
                Availability.DayOfWeek day = gene.getDayOfWeek();
                
                courseDayGenes.putIfAbsent(courseId, new HashMap<>());
                courseDayGenes.get(courseId).putIfAbsent(day, new ArrayList<>());
                courseDayGenes.get(courseId).get(day).add(gene);
            }
            
            // Her ders için günlere göre öğle arasını kontrol et
            for (Long courseId : courseDayGenes.keySet()) {
                // Her gün için puanlama yap
                for (Availability.DayOfWeek day : Availability.DayOfWeek.values()) {
                    // Gün için ders listesi
                    List<Gene> dayGeneList = courseDayGenes.getOrDefault(courseId, new HashMap<>())
                            .getOrDefault(day, new ArrayList<>());
                    
                    if (dayGeneList.isEmpty()) {
                        continue; // Bu ders için bu günde ders yoksa atla
                    }
                    
                    // Genleri başlangıç saatine göre sırala
                    dayGeneList.sort(Comparator.comparing(Gene::getStartHour));
                    
                    // Öğle arası zamanı (12-13 arası)
                    int lunchStartHour = LUNCH_BREAK_START;
                    int lunchEndHour = LUNCH_BREAK_END;
                    
                    boolean hasLunchBreak = true;
                    
                    // Öğle saatlerinde boşluk var mı kontrol et
                    for (Gene gene : dayGeneList) {
                        // Dersin zamanı öğle aralığı ile çakışıyor mu?
                        if ((gene.getStartHour() < lunchEndHour && gene.getEndHour() > lunchStartHour)) {
                            hasLunchBreak = false;
                            break;
                        }
                    }
                    
                    // Eğer öğle arası varsa puan ekle
                    if (hasLunchBreak) {
                        lunchBreakScore++;
                        System.out.println("GA: Öğle arası bulundu: Ders ID=" + courseId + ", Gün=" + day);
                    }
                    
                    // Özel durum: 4+ saatlik derslerin tüm saatlerinin aynı günde olması için ekstra bonus
                    // Bu dersin toplam saat sayısını hesapla
                    int totalHoursForCourse = 0;
                    for (List<Gene> genesByDay : courseDayGenes.get(courseId).values()) {
                        totalHoursForCourse += genesByDay.size();
                    }
                    
                    // 4+ saatlik dersler için ve tüm saatler bu günde ise ödül ver
                    if (totalHoursForCourse >= 4 && dayGeneList.size() == totalHoursForCourse) {
                        // Tüm saatler ardışık mı kontrol et
                        boolean allConsecutive = true;
                        for (int i = 0; i < dayGeneList.size() - 1; i++) {
                            Gene gene1 = dayGeneList.get(i);
                            Gene gene2 = dayGeneList.get(i + 1);
                            
                            if (gene1.getEndHour() != gene2.getStartHour()) {
                                allConsecutive = false;
                                break;
                            }
                        }
                        
                        // Tüm saatler ardışık ise büyük bonus ver
                        if (allConsecutive) {
                            lunchBreakScore += dayGeneList.size() * 2;
                            System.out.println("GA: Büyük bonus: " + dayGeneList.size() + " saatlik " + 
                                               " ders aynı günde ardışık. Ders ID=" + courseId + ", Gün=" + day);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Öğle arası değerlendirme hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("GA: Toplam öğle arası skoru: " + lunchBreakScore);
        return lunchBreakScore;
    }
    
    /**
     * Popülasyondan en iyi kromozomu döndürür
     */
    private Chromosome getBestChromosome(List<Chromosome> population) {
        if (population == null || population.isEmpty()) {
            return new Chromosome(new ArrayList<>());
        }
        
        try {
            // Null olan kromozomları filtrele
            List<Chromosome> validChromosomes = population.stream()
                .filter(c -> c != null && c.getGenes() != null && !c.getGenes().isEmpty())
                .collect(Collectors.toList());
            
            if (validChromosomes.isEmpty()) {
                return new Chromosome(new ArrayList<>());
            }
            
            // En yüksek fitness değerine sahip kromozomu bul
            return Collections.max(validChromosomes, Comparator.comparing(Chromosome::getFitness));
        } catch (Exception e) {
            System.err.println("En iyi kromozomu bulma hatası: " + e.getMessage());
            e.printStackTrace();
            return new Chromosome(new ArrayList<>());
        }
    }
    
    /**
     * Optimal çözümün bulunup bulunmadığını kontrol eder
     */
    private boolean isOptimalSolution(int fitness, int totalCourses) {
        // Hiç çakışma olmayan, tüm dersler için müsaitlik sağlanan ve öğle arası olan durumu kontrol et
        int perfectScore = totalCourses * INSTRUCTOR_AVAILABILITY_WEIGHT;
        
        // Öğle arası için haftanın her günü için puan ekle
        perfectScore += (Availability.DayOfWeek.values().length * totalCourses) * LUNCH_BREAK_WEIGHT;
        
        System.out.println("GA: Mükemmel çözüm skoru: " + perfectScore + ", Şu anki en iyi skor: " + fitness);
        return fitness >= perfectScore * 0.9; // %90 oranında iyi bir çözüm yeterli kabul edilebilir
    }
    
    /**
     * Kromozomun derin kopyasını oluşturur
     */
    private Chromosome deepCopy(Chromosome chromosome) {
        if (chromosome == null || chromosome.getGenes() == null) {
            return new Chromosome(new ArrayList<>());
        }
        
        List<Gene> copiedGenes = new ArrayList<>();
        
        try {
            for (Gene gene : chromosome.getGenes()) {
                if (gene == null) continue;
                
                Gene geneCopy = new Gene();
                geneCopy.setCourse(gene.getCourse());  // Sadece referans kopyalanıyor, derin kopya değil
                geneCopy.setInstructor(gene.getInstructor());  // Sadece referans kopyalanıyor, derin kopya değil
                geneCopy.setDayOfWeek(gene.getDayOfWeek());
                geneCopy.setStartHour(gene.getStartHour());
                geneCopy.setEndHour(gene.getEndHour());
                geneCopy.setClassroom(gene.getClassroom());
                
                copiedGenes.add(geneCopy);
            }
        } catch (Exception e) {
            System.err.println("Derin kopyalama hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        Chromosome copy = new Chromosome(copiedGenes);
        copy.setFitness(chromosome.getFitness());
        return copy;
    }
    
    /**
     * Popülasyonu evrimleştirir - yeni nesil oluşturur
     */
    private List<Chromosome> evolvePopulation(List<Chromosome> population) {
        List<Chromosome> newPopulation = new ArrayList<>();
        
        // Elitizm: En iyi kromozomu bir sonraki nesile doğrudan aktar
        Chromosome elite = deepCopy(getBestChromosome(population));
        newPopulation.add(elite);
        
        // Geri kalan popülasyonu seçim, çaprazlama ve mutasyon ile oluştur
        while (newPopulation.size() < POPULATION_SIZE) {
            // Turnuva seçimi ile iki ebeveyn seç
            Chromosome parent1 = tournamentSelection(population);
            Chromosome parent2 = tournamentSelection(population);
            
            // Çaprazlama
            Chromosome[] offspring;
            if (Math.random() < CROSSOVER_RATE) {
                offspring = crossover(parent1, parent2);
            } else {
                // Çaprazlama yapılmazsa, ebeveynleri kopyala
                offspring = new Chromosome[] {deepCopy(parent1), deepCopy(parent2)};
            }
            
            // Mutasyon
            for (Chromosome child : offspring) {
                if (Math.random() < MUTATION_RATE) {
                    mutate(child);
                }
            }
            
            // Yeni nesle ekle (popülasyon boyutunu aşmamak için kontrol et)
            for (Chromosome child : offspring) {
                if (newPopulation.size() < POPULATION_SIZE) {
                    newPopulation.add(child);
                } else {
                    break;
                }
            }
        }
        
        return newPopulation;
    }
    
    /**
     * Turnuva seçimi - popülasyondan rastgele kromozomlar seçer ve en iyisini döndürür
     */
    private Chromosome tournamentSelection(List<Chromosome> population) {
        List<Chromosome> tournament = new ArrayList<>();
        Random random = new Random();
        
        // Turnuva için rastgele kromozomlar seç
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            int randomIndex = random.nextInt(population.size());
            tournament.add(population.get(randomIndex));
        }
        
        // Turnuvadaki en iyi kromozomu döndür
        return getBestChromosome(tournament);
    }
    
    /**
     * İki ebeveyn kromozomdan çaprazlama yaparak iki yavru oluşturur
     */
    private Chromosome[] crossover(Chromosome parent1, Chromosome parent2) {
        try {
            // Ebeveynlerin null olup olmadığını kontrol et
            if (parent1 == null || parent2 == null || 
                parent1.getGenes() == null || parent2.getGenes() == null) {
                // Geçersiz ebeveynler durumunda yeni boş kromozomlar döndür
                return new Chromosome[] {
                    new Chromosome(new ArrayList<>()),
                    new Chromosome(new ArrayList<>())
                };
            }
            
            Random random = new Random();
            List<Gene> parent1Genes = parent1.getGenes();
            List<Gene> parent2Genes = parent2.getGenes();
            
            // Ebeveyn genlerinin boş olup olmadığını kontrol et
            if (parent1Genes.isEmpty() || parent2Genes.isEmpty()) {
                return new Chromosome[] {
                    new Chromosome(new ArrayList<>()),
                    new Chromosome(new ArrayList<>())
                };
            }

            // Çaprazlama noktasını belirle
            int crossoverPoint = random.nextInt(Math.min(parent1Genes.size(), parent2Genes.size()));
            
            // Çaprazlama yaparak yavruları oluştur
            List<Gene> child1Genes = new ArrayList<>();
            List<Gene> child2Genes = new ArrayList<>();
            
            // İlk çocuk için: parent1'in ilk kısmı + parent2'nin ikinci kısmı
            for (int i = 0; i < parent1Genes.size(); i++) {
                if (i < crossoverPoint) {
                    if (i < parent1Genes.size() && parent1Genes.get(i) != null) {
                        Gene geneCopy = new Gene();
                        Gene original = parent1Genes.get(i);
                        
                        geneCopy.setCourse(original.getCourse());
                        geneCopy.setInstructor(original.getInstructor());
                        geneCopy.setDayOfWeek(original.getDayOfWeek());
                        geneCopy.setStartHour(original.getStartHour());
                        geneCopy.setEndHour(original.getEndHour());
                        geneCopy.setClassroom(original.getClassroom());
                        child1Genes.add(geneCopy);
                    }
                } else if (i < parent2Genes.size() && parent2Genes.get(i) != null) {
                    Gene geneCopy = new Gene();
                    Gene original = parent2Genes.get(i);
                    
                    geneCopy.setCourse(original.getCourse());
                    geneCopy.setInstructor(original.getInstructor());
                    geneCopy.setDayOfWeek(original.getDayOfWeek());
                    geneCopy.setStartHour(original.getStartHour());
                    geneCopy.setEndHour(original.getEndHour());
                    geneCopy.setClassroom(original.getClassroom());
                    
                    child1Genes.add(geneCopy);
                }
            }
            
            // İkinci çocuk için: parent2'nin ilk kısmı + parent1'in ikinci kısmı
            for (int i = 0; i < parent2Genes.size(); i++) {
                if (i < crossoverPoint) {
                    if (i < parent2Genes.size() && parent2Genes.get(i) != null) {
                        Gene geneCopy = new Gene();
                        Gene original = parent2Genes.get(i);
                        
                        geneCopy.setCourse(original.getCourse());
                        geneCopy.setInstructor(original.getInstructor());
                        geneCopy.setDayOfWeek(original.getDayOfWeek());
                        geneCopy.setStartHour(original.getStartHour());
                        geneCopy.setEndHour(original.getEndHour());
                        geneCopy.setClassroom(original.getClassroom());
                        
                        child2Genes.add(geneCopy);
                    }
                } else if (i < parent1Genes.size() && parent1Genes.get(i) != null) {
                    Gene geneCopy = new Gene();
                    Gene original = parent1Genes.get(i);
                    
                    geneCopy.setCourse(original.getCourse());
                    geneCopy.setInstructor(original.getInstructor());
                    geneCopy.setDayOfWeek(original.getDayOfWeek());
                    geneCopy.setStartHour(original.getStartHour());
                    geneCopy.setEndHour(original.getEndHour());
                    geneCopy.setClassroom(original.getClassroom());
                    
                    child2Genes.add(geneCopy);
                }
            }
            
            return new Chromosome[] {
                new Chromosome(child1Genes),
                new Chromosome(child2Genes)
            };
        } catch (Exception e) {
            System.err.println("Çaprazlama sırasında hata oluştu: " + e.getMessage());
            return new Chromosome[] {
                new Chromosome(new ArrayList<>()),
                new Chromosome(new ArrayList<>())
            };
        }
    }
    
    /**
     * Kromozomu mutasyona uğratır - rastgele bir geninin özelliklerini değiştirir
     */
    private void mutate(Chromosome chromosome) {
        try {
            // Kromozom kontrolü
            if (chromosome == null || chromosome.getGenes() == null) {
                return;
            }
            
            List<Gene> genes = chromosome.getGenes();
            if (genes.isEmpty()) {
                return;
            }
            
            Random random = new Random();
            int geneIndex = random.nextInt(genes.size());
            Gene gene = genes.get(geneIndex);
            
            // Gen kontrolü
            if (gene == null || gene.getCourse() == null || gene.getInstructor() == null) {
                System.err.println("Mutasyon için geçersiz gen: " + geneIndex);
                return;
            }
            
            // Öğle saati kontrolü (11-13 arası)
            boolean isLunchTime = gene.getStartHour() < 13 && gene.getEndHour() > 11;
            
            // Mutasyon tipini rastgele seç
            int mutationType;
            
            // Eğer ders öğle saatine denk geliyorsa, daha yüksek olarak saati değiştirme olasılığını artır
            if (isLunchTime && random.nextDouble() < 0.7) { // %70 olasılıkla öğle arasındaki dersi taşı
                mutationType = 1; // Saati değiştir
                System.out.println("GA: Öğle saatinde ders tespit edildi, saat değişikliği mutasyonu uygulanıyor");
            } else {
                // 1 - Saat değiştirme 
                // 2 - (Derslik değişikliği - kullanılmıyor) 
                // 3 - Eğitmen değiştirme 
                // 4 - Öğle saati mutasyonu
                mutationType = random.nextInt(4) + 1; // Gün değiştirme seçeneğini kaldırdık (0 değerini atla)
                System.out.println("GA: Mutasyon tipi seçildi: " + mutationType);
            }
            
            switch (mutationType) {
                case 1: // Saati değiştir - aynı dersin genlerinin hepsini birlikte kaydır
                    Course course = gene.getCourse();
                    Availability.DayOfWeek day = gene.getDayOfWeek();
                    
                    // Aynı derse ait tüm genleri bul
                    List<Gene> sameCourseGenes = genes.stream()
                        .filter(g -> g.getCourse().equals(course) && g.getDayOfWeek() == day)
                        .sorted(Comparator.comparing(Gene::getStartHour))
                        .collect(Collectors.toList());
                    
                    if (sameCourseGenes.isEmpty()) {
                        break;
                    }
                    
                    // Toplam ders saatlerinin sayısını al
                    int totalCourseHours = sameCourseGenes.size();
                    System.out.println("GA: Ders toplam saat: " + totalCourseHours);
                    
                    // Dersin başlangıç saatini değiştir
                    int newStartHour;
                    if (isLunchTime || totalCourseHours >= 3) {
                        // Öğle saatindeki tüm dersleri veya uzun dersleri birlikte taşı
                        // 3+ saatlik dersler için ya sabah ya da öğleden sonra bloğunu tamamen kullan
                        boolean moveToMorning = random.nextBoolean();
                        
                        if (moveToMorning) {
                            // Sabah saatlerine taşı (8'den başlayarak)
                            newStartHour = WORK_DAY_START;
                            
                            // Sabah saatleri yeterli mi? (8-12 arası toplam 4 saat)
                            if (totalCourseHours > 4) {
                                // 4 saatten fazla ise, başka gün kullan
                                System.out.println("GA: 4 saatten fazla ders, sabah blogu yetersiz");
                            }
                        } else {
                            // Öğleden sonra 13'ten başlat, maksimum 17:00'ye kadar
                            newStartHour = LUNCH_BREAK_END;
                            
                            // Öğleden sonra yeterli saat var mı? (13-17 arası toplam 4 saat)
                            if (totalCourseHours > 4) {
                                // 4 saatten fazla ise, sabah bloğunu kullan
                                newStartHour = WORK_DAY_START;
                                System.out.println("GA: 4 saatten fazla ders, öğleden sonra yeterli değil");
                            }
                        }
                    } else {
                        // Normal saat değişikliği (kısa dersler için)
                        newStartHour = WORK_DAY_START + random.nextInt(LUNCH_BREAK_START - WORK_DAY_START); // 8-12 arası
                        
                        // Öğle saatinden kaçınma
                        if (newStartHour == LUNCH_BREAK_START) {
                            // Öğleden sonra 13'e kaydır
                            newStartHour = LUNCH_BREAK_END;
                        }
                        
                        // Toplam ders saatleri + başlangıç saati 17'yi aşabilir mi kontrol et
                        if (newStartHour + totalCourseHours > WORK_DAY_END) {
                            // Ders günün sonunda bitmeli, başlangıç saatini ayarla
                            newStartHour = WORK_DAY_END - totalCourseHours;
                            if (newStartHour < WORK_DAY_START) newStartHour = WORK_DAY_START; // Minimum 8:00
                        }
                    }
                    
                    System.out.println("GA: Yeni başlangıç saati: " + newStartHour);
                    
                    // Aynı dersin tüm genlerinin saatlerini sırayla güncelle
                    for (int i = 0; i < sameCourseGenes.size(); i++) {
                        Gene courseGene = sameCourseGenes.get(i);
                        int hourPosition = i + newStartHour;
                        
                        // Sınır kontrolü - 17:00'den sonra ders atama
                        if (hourPosition < WORK_DAY_END) {
                            courseGene.setStartHour(hourPosition);
                            courseGene.setEndHour(hourPosition + 1);
                            System.out.println("GA: Ders saati güncellendi: " + course.getName() + ", " + hourPosition + "-" + (hourPosition+1));
                        } else {
                            // 17:00'den sonraki dersleri ertesi güne kaydır veya başka bir uygun gün bul
                            System.out.println("GA: Maksimum saat sınırı (" + WORK_DAY_END + ":00) aşıldı, ders planlanamadı: " + course.getName());
                        }
                    }
                    break;
                    
                case 2: // Derslik değiştirme artık yapılmıyor - Okul kendi belirleyecek
                    // Derslik "Derslik atanmadı" olarak kalıyor, değişmiyor
                    break;
                    
                case 3: // Eğitmenin değiştirilmesi (eğer kurs için birden fazla eğitmen varsa)
                    if (gene.getCourse().getInstructors() != null) {
                        List<Instructor> courseInstructors = new ArrayList<>(gene.getCourse().getInstructors());
                        if (courseInstructors.size() > 1) {
                            Instructor currentInstructor = gene.getInstructor();
                            if (currentInstructor != null) {
                                try {
                                    Instructor newInstructor;
                                    int attempts = 0;
                                    do {
                                        int instructorIndex = random.nextInt(courseInstructors.size());
                                        newInstructor = courseInstructors.get(instructorIndex);
                                        attempts++;
                                        // Sonsuz döngüden kaçınma
                                        if (attempts > 10) break;
                                    } while (newInstructor != null && newInstructor.equals(currentInstructor));
                                    
                                    if (newInstructor != null) {
                                        gene.setInstructor(newInstructor);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Eğitmen atama hatası: " + e.getMessage());
                                }
                            }
                        }
                    }
                    break;
                    
                case 4: // Öğle saati mutasyonu (özel bir durum)
                    // Aynı case 1 gibi, ancak öğle saatinden kesinlikle uzakta
                    Course lunchCourse = gene.getCourse();
                    Availability.DayOfWeek lunchDay = gene.getDayOfWeek();
                    
                    // Toplam ders saatini bul
                    int courseWeeklyHours = lunchCourse.getWeeklyHours();
                    
                    // Aynı derse ait tüm genleri bul
                    List<Gene> lunchCourseGenes = genes.stream()
                        .filter(g -> g.getCourse().equals(lunchCourse) && g.getDayOfWeek() == lunchDay)
                        .sorted(Comparator.comparing(Gene::getStartHour))
                        .collect(Collectors.toList());
                    
                    if (lunchCourseGenes.isEmpty()) {
                        break;
                    }
                    
                    // Kullanılan eğitimciyi al ve tüm genler için aynı eğitimciyi kullan
                    Instructor courseInstructor = gene.getInstructor();
                    
                    // Öğle saatine yakın bir ders mi kontrol et
                    boolean isNearLunch = lunchCourseGenes.stream()
                            .anyMatch(g -> g.getStartHour() >= 10 && g.getStartHour() <= 14 ||
                                       g.getEndHour() >= 11 && g.getEndHour() <= 15);
                    
                    // 4 saatlik bir ders mi kontrol et
                    boolean isLongCourse = courseWeeklyHours >= 4;
                    
                    // Dersin gen sayısını hesapla
                    int lunchCourseTotalHours = lunchCourseGenes.size();
                    
                    // Öğle arasına denk geliyor veya uzun ders ise taşı
                    if (isNearLunch || isLongCourse) {
                        // 4 saatlik dersler için sabah bloğunu kullan (8-12)
                        int lunchNewStartHour = WORK_DAY_START;
                        
                        if (lunchCourseTotalHours == 4) {
                            // 4 saatlik dersler için ya sabah (8-12) ya da öğleden sonra (13-17) bloğunu kullan
                            boolean useAfternoon = random.nextBoolean();
                            if (useAfternoon) {
                                lunchNewStartHour = LUNCH_BREAK_END; // Öğleden sonra 13:00'ten başlat, 17:00'de biter
                            }
                        }
                        
                        // Aynı eğitimciyi tüm saatler için ata ve ardışık saatleri düzenle
                        for (int i = 0; i < lunchCourseGenes.size(); i++) {
                            Gene courseGene = lunchCourseGenes.get(i);
                            int hourPosition = i + lunchNewStartHour;
                            
                            courseGene.setInstructor(courseInstructor); // Aynı eğitimciyi kullan
                            
                                // Sınır kontrolü - 17:00'den sonra ders atama
                                if (hourPosition < WORK_DAY_END) {
                                    courseGene.setStartHour(hourPosition);
                                    courseGene.setEndHour(hourPosition + 1);
                                    System.out.println("GA: Öğle arası mutasyonu: Ders saati güncellendi: " + 
                                                     lunchCourse.getName() + ", " + hourPosition + "-" + (hourPosition+1));
                                } else {
                                    // 17:00'den sonraki dersleri başka güne veya başka saate kaydır
                                    System.out.println("GA: Maksimum saat sınırı (" + WORK_DAY_END + ":00) aşıldı, ders planlanamadı: " + lunchCourse.getName());
                                }
                        }
                        
                        // Fitness değerini artırmak için direk bonus ekle
                        chromosome.setFitness(chromosome.getFitness() + (lunchCourseTotalHours * 2));
                        System.out.println("GA: Öğle arası mutasyonu bonusu: +" + (lunchCourseTotalHours * 2));
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("Mutasyon işlemi sırasında hata: " + e.getMessage());
        }
    }
    
    /**
     * Kromozomdan Schedule nesnesi oluşturur
     */
    private Schedule createScheduleFromChromosome(Chromosome chromosome, Department department, String academicTerm) {
        try {
            // Parametreleri kontrol et
            if (chromosome == null || chromosome.getGenes() == null) {
                throw new IllegalArgumentException("Geçersiz kromozom");
            }
            
            if (department == null) {
                throw new IllegalArgumentException("Bölüm bilgisi geçersiz");
            }
            
            if (academicTerm == null || academicTerm.trim().isEmpty()) {
                academicTerm = "Belirsiz Dönem";
            }
            
            // Yeni bir Schedule nesnesi oluştur
            Schedule schedule = new Schedule();
            schedule.setName(department.getName() + " - " + academicTerm + " Ders Programı");
            schedule.setDepartment(department);
            schedule.setAcademicTerm(academicTerm);
            schedule.setDescription("Genetik algoritma ile otomatik oluşturulmuştur.");
            schedule.setIsActive(true);
            schedule.setScheduleItems(new HashSet<>()); // Önce boş set oluştur
            
            // Önce Schedule'u kaydet, id ata
            schedule = scheduleRepository.save(schedule);
            
            // Kromozomdaki her gen için program öğeleri (ScheduleItem) oluştur ve kaydet
            List<ScheduleItem> scheduleItems = saveScheduleItems(schedule, chromosome.getGenes());
            
            if (scheduleItems.isEmpty()) {
                throw new IllegalStateException("Ders programında hiç ders bulunamadı");
            }
            
            // Güncel Schedule'i kaydet
            schedule = scheduleRepository.save(schedule);
            
            // Güncel Schedule'ı getir
            return scheduleRepository.findById(schedule.getId()).orElse(schedule);
        } catch (Exception e) {
            System.err.println("Program oluşturma hatası: " + e.getMessage());
            e.printStackTrace();
            // Hata durumunda boş bir program döndür
            Schedule fallbackSchedule = new Schedule();
            fallbackSchedule.setName("Oluşturulamayan Program");
            fallbackSchedule.setDescription("Program oluşturulurken hata meydana geldi: " + e.getMessage());
            fallbackSchedule.setAcademicTerm(academicTerm != null ? academicTerm : "Belirsiz Dönem");
            fallbackSchedule.setDepartment(department);
            fallbackSchedule.setIsActive(false);
            return scheduleRepository.save(fallbackSchedule);
        }
    }
    
    /**
     * Genleri ScheduleItem nesnelerine dönüştürür ve kaydeder
     */
    private List<ScheduleItem> saveScheduleItems(Schedule schedule, List<Gene> genes) {
        List<ScheduleItem> savedItems = new ArrayList<>();
        
        // Eğer genler boşsa, boş liste döndür
        if (genes == null || genes.isEmpty()) {
            System.err.println("GA: Kaydedilecek gen bulunmadı");
            return savedItems;
        }
        
        // İşlem başladığına dair mesaj
        System.out.println("GA: Toplam " + genes.size() + " program öğesi kaydedilecek");
        
        for (Gene gene : genes) {
            // Gen geçerlilik kontrolü
            if (gene == null || gene.getCourse() == null || gene.getInstructor() == null || 
            gene.getDayOfWeek() == null || 
            gene.getStartHour() == 0 || 
            gene.getEndHour() == 0) {
            System.err.println("Geçersiz gen atlandı");
            continue;
        }
            
            try {
                ScheduleItem item = new ScheduleItem();
                item.setCourse(gene.getCourse());
                item.setInstructor(gene.getInstructor());
                item.setDayOfWeek(gene.getDayOfWeek());
                item.setStartHour(gene.getStartHour());
                item.setEndHour(gene.getEndHour());
                item.setSchedule(schedule);
                
                // Kaydedilen öğeyi listeye ekle
                ScheduleItem savedItem = scheduleItemRepository.save(item);
                savedItems.add(savedItem);
                
                // Schedule'a ekle (yardımcı metodu kullan)
                schedule.addScheduleItem(savedItem);
                
                System.out.println("Program öğesi kaydedildi: Ders=" + gene.getCourse().getName() + 
                                 ", Eğitimci=" + gene.getInstructor().getUser().getFullName() + 
                                 ", Gün=" + gene.getDayOfWeek() + 
                                 ", Saat=" + gene.getStartHour() + "-" + gene.getEndHour());
            } catch (Exception e) {
                System.err.println("Program öğesi oluşturma veya kaydetme hatası: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // İşlem sonuçlarını raporla
        System.out.println("GA: Toplam " + savedItems.size() + " program öğesi başarıyla kaydedildi");
        
        return savedItems;
    }
    
    /**
     * Kromozom sınıfı - bir ders programı çözümünü temsil eder
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Chromosome {
        private List<Gene> genes;
        private int fitness;
        
        public Chromosome(List<Gene> genes) {
            this.genes = genes;
            this.fitness = 0;
        }
    }
    
    /**
     * Gen sınıfı - bir ders programı atama birimini temsil eder
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Gene {
        private Course course;
        private Instructor instructor;
        private Availability.DayOfWeek dayOfWeek;
        private int startHour;
        private int endHour;
        private String classroom;
    }
}