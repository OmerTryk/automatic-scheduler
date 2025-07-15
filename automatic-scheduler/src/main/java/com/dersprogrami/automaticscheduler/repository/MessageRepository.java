package com.dersprogrami.automaticscheduler.repository;

import com.dersprogrami.automaticscheduler.model.Department;
import com.dersprogrami.automaticscheduler.model.Message;
import com.dersprogrami.automaticscheduler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    // Kullanıcının gelen kutusu (kendisine gelen mesajlar)
    List<Message> findByReceiverOrderBySentAtDesc(User receiver);
    
    // Kullanıcının gönderilen kutusu (kendisinin gönderdiği mesajlar)
    List<Message> findBySenderOrderBySentAtDesc(User sender);
    
    // Kullanıcının gelen kutusundaki okunmamış mesajlar
    List<Message> findByReceiverAndIsReadFalseOrderBySentAtDesc(User receiver);
    
    // Aynı bölümdeki kullanıcılar arasındaki mesajlar
    List<Message> findByDepartmentAndReceiverOrderBySentAtDesc(Department department, User receiver);
    
    // Okunmamış mesaj sayısı
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver = ?1 AND m.isRead = false")
    long countUnreadMessagesByUser(User user);
}
