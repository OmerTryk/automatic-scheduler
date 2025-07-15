package com.dersprogrami.automaticscheduler.service;

import com.dersprogrami.automaticscheduler.model.Department;
import com.dersprogrami.automaticscheduler.model.Message;
import com.dersprogrami.automaticscheduler.model.User;
import com.dersprogrami.automaticscheduler.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;

    public MessageService(MessageRepository messageRepository, UserService userService) {
        this.messageRepository = messageRepository;
        this.userService = userService;
    }

    // Tüm mesajları getir
    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    // ID'ye göre mesaj getir
    public Optional<Message> getMessageById(Long id) {
        return messageRepository.findById(id);
    }

    // Kullanıcının gelen kutusu
    public List<Message> getInboxMessages(User user) {
        return messageRepository.findByReceiverOrderBySentAtDesc(user);
    }

    // Kullanıcının gönderilen kutusu
    public List<Message> getSentMessages(User user) {
        return messageRepository.findBySenderOrderBySentAtDesc(user);
    }

    // Kullanıcının okunmamış mesajları
    public List<Message> getUnreadMessages(User user) {
        return messageRepository.findByReceiverAndIsReadFalseOrderBySentAtDesc(user);
    }

    // Aynı bölümdeki kullanıcılar arasındaki mesajlar
    public List<Message> getDepartmentInboxMessages(Department department, User user) {
        return messageRepository.findByDepartmentAndReceiverOrderBySentAtDesc(department, user);
    }

    // Okunmamış mesaj sayısı
    public long getUnreadMessageCount(User user) {
        return messageRepository.countUnreadMessagesByUser(user);
    }

    // Yeni mesaj oluştur
    @Transactional
    public Message createMessage(User sender, Long receiverId, String subject, String content, Department department) {
        User receiver = userService.getUserById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Alıcı bulunamadı"));

        // Aynı bölümde olduklarını kontrol et
        if (department != null && !isSameDepartment(sender, receiver, department)) {
            throw new IllegalArgumentException("Mesaj sadece aynı bölümdeki kişilere gönderilebilir");
        }

        LocalDateTime now = LocalDateTime.now();
        
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setSubject(subject);
        message.setContent(content);
        message.setDepartment(department);
        message.setDelivered(true); // İletildi olarak işaretle
        message.setSentAt(now);
        message.setTimestamp(now);

        return messageRepository.save(message);
    }

    // Mesajı okundu olarak işaretle
    @Transactional
    public Message markMessageAsRead(Long messageId, User currentUser) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Mesaj bulunamadı"));

        // Sadece alıcı mesajı okundu olarak işaretleyebilir
        if (!message.getReceiver().equals(currentUser)) {
            throw new IllegalArgumentException("Bu mesajı okundu olarak işaretleme yetkiniz yok");
        }

        message.markAsRead();
        return messageRepository.save(message);
    }

    // Mesajı iletildi olarak işaretle
    @Transactional
    public Message markMessageAsDelivered(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Mesaj bulunamadı"));

        message.markAsDelivered();
        return messageRepository.save(message);
    }

    // Mesajı sil
    @Transactional
    public void deleteMessage(Long messageId, User currentUser) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Mesaj bulunamadı"));

        // Sadece gönderen veya alıcı mesajı silebilir
        if (!message.getSender().equals(currentUser) && !message.getReceiver().equals(currentUser)) {
            throw new IllegalArgumentException("Bu mesajı silme yetkiniz yok");
        }

        messageRepository.delete(message);
    }

    // İki kullanıcının aynı bölümde olup olmadığını kontrol et
    private boolean isSameDepartment(User user1, User user2, Department department) {
        // Kullanıcıların bölüm kontrol mantığı
        // Burada User sınıfının yapısına göre kontrol eklenebilir
        // Örneğin: Her ikisinin de instructor/departmentHead'i aynı bölümde olmalı
        
        if (department == null) {
            return false;
        }
        
        // Örnek implementasyon - gerekirse projenin User yapısına göre değiştirin
        boolean user1InDepartment = (user1.getInstructor() != null && 
                                    user1.getInstructor().getDepartment() != null && 
                                    user1.getInstructor().getDepartment().equals(department)) ||
                                   (user1.getDepartmentHead() != null && 
                                    user1.getDepartmentHead().getDepartment() != null && 
                                    user1.getDepartmentHead().getDepartment().equals(department));
                                    
        boolean user2InDepartment = (user2.getInstructor() != null && 
                                    user2.getInstructor().getDepartment() != null && 
                                    user2.getInstructor().getDepartment().equals(department)) ||
                                   (user2.getDepartmentHead() != null && 
                                    user2.getDepartmentHead().getDepartment() != null && 
                                    user2.getDepartmentHead().getDepartment().equals(department));
                                    
        return user1InDepartment && user2InDepartment;
    }
}