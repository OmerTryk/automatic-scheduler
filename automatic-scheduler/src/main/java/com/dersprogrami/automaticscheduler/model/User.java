package com.dersprogrami.automaticscheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"instructor", "departmentHead"})
@EqualsAndHashCode(of = {"id", "username"}, callSuper = false)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Instructor instructor;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private DepartmentHead departmentHead;

    public boolean isInstructor() {
        return roles.stream().anyMatch(role -> role.getName().equals("ROLE_INSTRUCTOR"));
    }

    public boolean isDepartmentHead() {
        return roles.stream().anyMatch(role -> role.getName().equals("ROLE_DEPARTMENT_HEAD"));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email); // Instructor'ı dahil etmeyin
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
                Objects.equals(username, user.username) &&
                Objects.equals(email, user.email);
    }
}