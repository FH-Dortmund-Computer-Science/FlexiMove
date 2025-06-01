package de.fhdo.spring.user.context.domain;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Provider extends User {
	private String companyName;

    public Provider() {
        super();
    }

    public Provider(Password password, Email email, Adress address, String phoneNumber, String companyName, PaymentInfo paymentInfo) {
        super(password, email, null, address, null, phoneNumber, paymentInfo);
        this.companyName = companyName;
    }

    // Getter
    public String getCompanyName() {
        return companyName;
    }

    // Setter
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}
