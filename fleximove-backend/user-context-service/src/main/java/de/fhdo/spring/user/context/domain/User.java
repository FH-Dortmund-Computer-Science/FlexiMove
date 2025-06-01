package de.fhdo.spring.user.context.domain;

import java.time.LocalDate;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
public abstract class User {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	@Embedded
	private Adress address;
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "password_value", nullable = false))
    private Password password;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email_value", nullable = false, unique = true))
    private Email email;

    private LocalDate dateOfBirth;
   
    private Boolean hasDrivingLicense;

    private String phoneNumber;
    @Embedded
    private PaymentInfo paymentinfo;
    
    private boolean isRegisterd;

    // Standard-Konstruktor für JPA
    public User() {}

    // Konstruktor mit wichtigen Feldern (ohne ID)
    public User(Password password, Email email, LocalDate dateOfBirth,
                Adress address, Boolean hasDrivingLicense, String phoneNumber, PaymentInfo paymentinfo) {
        this.password = password;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.hasDrivingLicense = hasDrivingLicense;
        this.phoneNumber = phoneNumber;
        this.paymentinfo=paymentinfo;
        isRegisterd=true;
    }

    // Getter und Setter
    public Long getId() {
        return id;
    }

  
    public Password getPassword() {
        return password;
    }

    public void setPassword(Password password) {
        this.password = password;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Adress getAddress() {
        return address;
    }

    public void setAddress(Adress address) {
        this.address = address;
    }

    public Boolean getHasDrivingLicense() {
        return hasDrivingLicense;
    }

    public void setHasDrivingLicense(Boolean hasDrivingLicense) {
        this.hasDrivingLicense = hasDrivingLicense;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public PaymentInfo getPaymentInfo() {
		return paymentinfo;
    	
    }
    
    public void setPaymentInfo(PaymentInfo paymeninfo) {
		this.paymentinfo=paymeninfo;
    	
    }

	public boolean isRegisterd() {
		return isRegisterd;
	}

	public void setRegisterd(boolean isRegisterd) {
		this.isRegisterd = isRegisterd;
	}
}
