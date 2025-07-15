package com.dersprogrami.automaticscheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;
    
    @Column(nullable = false)
    private String subject;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(nullable = false, name = "sent_at")
    private LocalDateTime sentAt = LocalDateTime.now();
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(nullable = false, name = "is_read")
    private boolean isRead = false;
    
    @Column(nullable = false, name = "is_delivered")
    private boolean isDelivered = true; // Varsayılan olarak true
    
    @Column(nullable = false, name = "timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
    
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
    
    public void markAsDelivered() {
        this.isDelivered = true;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id);
    }
}