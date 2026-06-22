package com.swedapp.bank.db.entity;

import com.swedapp.bank.domain.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = true)
    private String firstName;

    @Column(nullable = true)
    private String lastName;

    @Column(nullable = true)
    private String companyName;

    @Column(nullable = true)
    private LocalDate birthday;

    @Column(nullable = true)
    private String nationality;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt-hashed password. */
    @Column(nullable = false)
    private String password;

    protected UserEntity() {
        // Required by JPA.
    }

    public UserEntity(UserType userType, String code, String firstName, String lastName,
            String companyName, LocalDate birthday, String nationality, String email, String password) {
        this.userType = userType;
        this.code = code;
        this.firstName = firstName;
        this.lastName = lastName;
        this.companyName = companyName;
        this.birthday = birthday;
        this.nationality = nationality;
        this.email = email;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
