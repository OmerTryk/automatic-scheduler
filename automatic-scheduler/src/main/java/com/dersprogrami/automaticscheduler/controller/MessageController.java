package com.dersprogrami.automaticscheduler.controller;

import com.dersprogrami.automaticscheduler.model.Department;
import com.dersprogrami.automaticscheduler.model.DepartmentHead;
import com.dersprogrami.automaticscheduler.model.Instructor;
import com.dersprogrami.automaticscheduler.model.Message;
import com.dersprogrami.automaticscheduler.model.User;
import com.dersprogrami.automaticscheduler.service.DepartmentService;
import com.dersprogrami.automaticscheduler.service.MessageService;
import com.dersprogrami.automaticscheduler.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;
    private final DepartmentService departmentService;

    public MessageController(MessageService messageService, 
                          UserService userService,
                          DepartmentService departmentService) {
        this.messageService = messageService;
        this.userService = userService;
        this.departmentService = departmentService;
    }

    // Gelen kutusu sayfası
    @GetMapping("/inbox")
    public String inbox(Model model, Authentication authentication) {
        try {
            User currentUser = userService.getUserByUsername(authentication.getName());
            List<Message> inboxMessages = messageService.getInboxMessages(currentUser);
            long unreadCount = messageService.getUnreadMessageCount(currentUser);
            
            model.addAttribute("messages", inboxMessages);
            model.addAttribute("unreadCount", unreadCount);
            model.addAttribute("folder", "Gelen Kutusu");
            
            return "messages/inbox";
        } catch (Exception e) {
            model.addAttribute("error", "Kullanıcı bilgisi alınamadı: " + e.getMessage());
            return "redirect:/login";
        }
    }

    // Gönderilen mesajlar sayfası
    @GetMapping("/sent")
    public String sent(Model model, Authentication authentication) {
        try {
            User currentUser = userService.getUserByUsername(authentication.getName());
            List<Message> sentMessages = messageService.getSentMessages(currentUser);
            
            model.addAttribute("messages", sentMessages);
            model.addAttribute("folder", "Gönderilen Kutusu");
            
            return "messages/sent";
        } catch (Exception e) {
            model.addAttribute("error", "Kullanıcı bilgisi alınamadı: " + e.getMessage());
            return "redirect:/login";
        }
    }

    // Yeni mesaj oluşturma sayfası
    @GetMapping("/compose")
    public String composeForm(Model model, Authentication authentication) {
        try {
            User currentUser = userService.getUserByUsername(authentication.getName());
            Department userDepartment = getUserDepartment(currentUser);
            
            if (userDepartment == null) {
                return "redirect:/messages/inbox?error=Bölüm bilgisi bulunamadı";
            }
            
            // Aynı bölümdeki kullanıcıları getir
            List<User> departmentUsers = userService.getUsersByDepartment(userDepartment);
            // Kendisini listeden çıkar
            departmentUsers.removeIf(user -> user.equals(currentUser));
            
            model.addAttribute("recipients", departmentUsers);
            model.addAttribute("department", userDepartment);
            
            return "messages/compose";
        } catch (Exception e) {
            model.addAttribute("error", "Kullanıcı bilgisi alınamadı: " + e.getMessage());
            return "redirect:/login";
        }
    }

    // Yeni mesaj gönderme
    @PostMapping("/compose")
    public String sendMessage(@RequestParam("receiverId") Long receiverId,
                             @RequestParam("subject") String subject,
                             @RequestParam("content") String content,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getUserByUsername(authentication.getName());
            Department userDepartment = getUserDepartment(currentUser);
            
            if (userDepartment == null) {
                redirectAttributes.addFlashAttribute("error", "Bölüm bilgisi bulunamadı");
                return "redirect:/messages/inbox";
            }
            
            messageService.createMessage(currentUser, receiverId, subject, content, userDepartment);
            redirectAttributes.addFlashAttribute("success", "Mesaj başarıyla gönderildi");
            return "redirect:/messages/sent";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Mesaj gönderilirken hata oluştu: " + e.getMessage());
            return "redirect:/messages/compose";
        }
    }

    // Mesaj detayı görüntüleme
    @GetMapping("/{id}")
    public String viewMessage(@PathVariable("id") Long messageId,
                            Model model,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getUserByUsername(authentication.getName());
            Message message = messageService.getMessageById(messageId)
                    .orElseThrow(() -> new IllegalArgumentException("Mesaj bulunamadı"));
            
            // Sadece mesajın alıcısı veya göndericisi görebilir
            if (!message.getReceiver().equals(currentUser) && !message.getSender().equals(currentUser)) {
                redirectAttributes.addFlashAttribute("error", "Bu mesajı görüntüleme yetkiniz yok");
                return "redirect:/messages/inbox";
            }
            
            // Eğer kullanıcı mesajın alıcısıysa ve mesaj okunmamışsa, okundu olarak işaretle
            if (message.getReceiver().equals(currentUser) && !message.isRead()) {
                message = messageService.markMessageAsRead(messageId, currentUser);
            }
            
            model.addAttribute("message", message);
            model.addAttribute("isReceiver", message.getReceiver().equals(currentUser));
            
            return "messages/view";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Mesaj görüntülenirken hata oluştu: " + e.getMessage());
            return "redirect:/messages/inbox";
        }
    }

    // Mesaj silme
    @PostMapping("/{id}/delete")
    public String deleteMessage(@PathVariable("id") Long messageId,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getUserByUsername(authentication.getName());
            messageService.deleteMessage(messageId, currentUser);
            
            redirectAttributes.addFlashAttribute("success", "Mesaj başarıyla silindi");
            return "redirect:/messages/inbox";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Mesaj silinirken hata oluştu: " + e.getMessage());
            return "redirect:/messages/inbox";
        }
    }

    // Kullanıcının bölümünü al
    private Department getUserDepartment(User user) {
        if (user.getInstructor() != null && user.getInstructor().getDepartment() != null) {
            return user.getInstructor().getDepartment();
        } else if (user.getDepartmentHead() != null && user.getDepartmentHead().getDepartment() != null) {
            return user.getDepartmentHead().getDepartment();
        }
        return null;
    }
}