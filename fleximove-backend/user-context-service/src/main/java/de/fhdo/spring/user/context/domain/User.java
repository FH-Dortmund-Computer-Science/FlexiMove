package de.fhdo.spring.user.context.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "SimpleUser")
public abstract class User {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	@Embedded
	private Address address;
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "password_value", nullable = false))
    private Password password;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email_value", nullable = false, unique = true))
    private Email email;

    private String phoneNumber;
    @Embedded
    private PaymentInfo paymentinfo;
    
    private boolean isRegistered;

    public User(Password password, Email email, Address address, String phoneNumber, PaymentInfo paymentinfo) {
        this.password = password;
        this.email = email;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.paymentinfo=paymentinfo;
        isRegistered=true;
    }
}
