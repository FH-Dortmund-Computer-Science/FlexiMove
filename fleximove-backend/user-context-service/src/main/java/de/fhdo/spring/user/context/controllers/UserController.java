package de.fhdo.spring.user.context.controllers;

import java.util.List;

import de.fhdo.spring.user.context.domain.*;
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
import de.fhdo.spring.user.context.dto.LoginRequest;
import de.fhdo.spring.user.context.services.LoginService;
import de.fhdo.spring.user.context.services.RegistrationService;
import de.fhdo.spring.user.context.services.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;
	private final LoginService loginService;
	private final RegistrationService registrationService;

	@Autowired
	public UserController(UserService userService, LoginService loginService, RegistrationService registrationService) {
		this.userService = userService;
		this.loginService = loginService;
		this.registrationService = registrationService;
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

	// User löschen   ==> Ansastasia ansprechen weil auch alle bestehenden Buchungen gelöscht werden müssen
	@DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user != null) {
            userService.deleteUser(user);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
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
	public void updateAddress(@PathVariable Long id, @RequestBody Address newAddress) {
		User user = userService.getUserById(id);
		if (user != null) {
			userService.updateAddress(user, newAddress);
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
            return ResponseEntity.ok("Registration successful!");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
	
	@PostMapping("/registerprovider")
    public ResponseEntity<String> registerProvider(@RequestBody Provider provider) {
        try {
            registrationService.registerUser(provider);
            return ResponseEntity.ok("Registration successful!");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
	@PostMapping("/login")
	public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
	    try {
	        User loggedUser = loginService.login(loginRequest.getEmail(), loginRequest.getPassword());
			//TODO: nicht kompletten User mit Password zurueckgeben, sondern LoginResponse
	        return ResponseEntity.ok(loggedUser);
	    } catch (IllegalStateException e) {
	        return ResponseEntity.status(401).body("Invalid credentials");
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