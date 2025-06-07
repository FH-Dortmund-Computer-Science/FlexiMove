package de.fhdo.spring.user.context.controllers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.UUID;

import org.apache.hc.core5.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import clients.BookingClient;
import de.fhdo.spring.user.context.domain.Adress;
import de.fhdo.spring.user.context.domain.Customer;
import de.fhdo.spring.user.context.domain.Email;
import de.fhdo.spring.user.context.domain.Password;
import de.fhdo.spring.user.context.domain.Provider;
import de.fhdo.spring.user.context.domain.User;
import de.fhdo.spring.user.context.dto.BookingDto;
import de.fhdo.spring.user.context.dto.LoginRequest;
import de.fhdo.spring.user.context.repository.UserRepository;
import de.fhdo.spring.user.context.services.LoginService;
import de.fhdo.spring.user.context.services.RegistrationService;
import de.fhdo.spring.user.context.services.UserService;
import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;
    private final LoginService loginService;
    private final RegistrationService registrationService;
    private final BookingClient bookingClient;  // <-- hier

    @Autowired
    public UserController(UserService userService, LoginService loginService, RegistrationService registrationService, BookingClient bookingClient) {
        this.userService = userService;
        this.loginService = loginService;
        this.registrationService = registrationService;
        this.bookingClient = bookingClient;  // <-- hier
    }


	// Alle User abrufen
	@GetMapping
	public List<User> getAlleUser() {
		return userService.getAlleUser();
	}

	// User nach ID abrufen
	@GetMapping("/{id}")
	public User getUserById(@PathVariable Long id) {
		return userService.getUserById(id);
	}

	// User nach E-Mail abrufen
	@GetMapping("/email")
	public User getUserByEmail(@RequestParam Email email) {
		return userService.getUserByEmail(email);
	}
	
	
	//Provider mit ID suchen und dann Namen wiedergeben
	
	
	// User speichern (z.B. Customer oder Provider)
	@PostMapping
	public void createUser(@RequestBody User user) {
		userService.saveUser(user);
	}

	/* User löschen   ==> Ansastasia ansprechen weil auch alle bestehenden Buchungen gelöscht werden müssen
	@DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user != null) {
            userService.deleteUser(user);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }*/
	
	
/*	
	@DeleteMapping("/{userId}/bookings")
	public ResponseEntity<Void> deleteUserBookings(@PathVariable Long userId) {
	    try {
	        BookingClient.deleteUserBookings(userId); // ruft den Feign-Client auf
	        return ResponseEntity.noContent().build();
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	    }
	}
*/
	

	@DeleteMapping("/{userId}")
	public ResponseEntity<Void> deleteUserWithBookings(@PathVariable Long userId) {
	    try {
	        // Zuerst Buchungen löschen
	        bookingClient.deleteUserBookings(userId);

	        // Wenn erfolgreich, dann Nutzer löschen
	        User user = userService.getUserById(userId);
	        if (user != null) {
	            userService.deleteUserById(userId);
	            return ResponseEntity.noContent().build(); // 204
	        } else {
	            return ResponseEntity.notFound().build(); // 404
	        }

	    } catch (Exception e) {
	        // Falls das Löschen der Buchungen oder des Nutzers fehlschlägt
	        System.err.println("Fehler beim Löschen: " + e.getMessage());
	        return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build(); // 500
	    }
	}

	
	
	
	
/*	
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
	    try {
	        userService.deleteUserById(id); // neue Methode
	        return ResponseEntity.noContent().build();
	    } catch (EntityNotFoundException e) {
	        return ResponseEntity.notFound().build();
	    }
	}*/

	
	
	
	// Change Password
	@PutMapping("/{id}/password")
	public void updatePassword(@PathVariable Long id, @RequestBody Password newPassword) {
		User user = userService.getUserById(id);
		if (user != null) {
			userService.updatepw(user, newPassword);
		}
	}

	//Change Adress
	@PutMapping("/{id}/address")
	public void updateAddress(@PathVariable Long id, @RequestBody Adress newAddress) {
		User user = userService.getUserById(id);
		if (user != null) {
			userService.updateadress(user, newAddress);
		}
	}

	// Company Name ändern (nur für Provider)
	@PutMapping("/provider/{id}/companyname")
	public void updateCompanyName(@PathVariable Long id, @RequestBody String newCompanyName) {
		User user = userService.getUserById(id);
		if (user instanceof Provider) {
			Provider provider = (Provider) user;
			userService.updtaecompanyname(provider, newCompanyName);
		}
	}
	
	@PostMapping("/registercustomer")
    public ResponseEntity<String> registerCustomer(@RequestBody Customer customer) {
        try {
            registrationService.registerUser(customer);
            return ResponseEntity.ok("Registrierung erfolgreich!");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
	
	@PostMapping("/registerprovider")
    public ResponseEntity<String> registerProvider(@RequestBody Provider provider) {
        try {
            registrationService.registerUser(provider);
            return ResponseEntity.ok("Registrierung erfolgreich!");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
	@PostMapping("/login")
	public ResponseEntity<String> loginUser(@RequestBody LoginRequest loginRequest) {
	    try {
			//TODO: User zurueckgeben, nicht boolean
	        boolean success = loginService.login(loginRequest.getEmail(), loginRequest.getPassword());
			//TODO: LoginResponse zuruecksenden
	        return ResponseEntity.ok("Login erfolgreich!");
	    } catch (IllegalStateException e) {
	        return ResponseEntity.status(401).body(e.getMessage());
	    }
}
	
	@GetMapping("/provider/{id}/companyName")
    public ResponseEntity<String> getProviderCompanyName(@PathVariable Long id) {
        String companyName = userService.getCompanyNameById(id);

        if (companyName != null) {
            return ResponseEntity.ok(companyName);
        } else {
            return ResponseEntity.status(404).body("Kein Provider mit dieser ID gefunden.");
        }
    }
	

}