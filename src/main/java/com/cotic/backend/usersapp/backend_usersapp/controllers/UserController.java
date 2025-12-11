package com.cotic.backend.usersapp.backend_usersapp.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RestController;

import com.cotic.backend.usersapp.backend_usersapp.models.dto.UserDto;
import com.cotic.backend.usersapp.backend_usersapp.models.entities.User;
import com.cotic.backend.usersapp.backend_usersapp.models.request.UserRequest;
import com.cotic.backend.usersapp.backend_usersapp.services.UserService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/users")
@CrossOrigin(originPatterns = "*")
public class UserController {

    private final UserService service;

    @Autowired
    public UserController(UserService service) {
        this.service = service;
    }

    // DTO para recibir el token
    public static class TokenRequest {
        public String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    // @GetMapping("/")
    @GetMapping
    public List<UserDto> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> show(@PathVariable(name = "id") Long id) {
        Optional<UserDto> userOptional = service.findById(id);
        if (userOptional.isPresent()) {
            return ResponseEntity.ok(userOptional.orElseThrow());
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody User user, BindingResult result) {
        if (result.hasErrors()) {
            return validation(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@Valid @RequestBody UserRequest user, BindingResult result,
            @PathVariable(name = "id") Long id) {
        if (result.hasErrors()) {
            return validation(result);
        }
        Optional<UserDto> o = service.update(user, id);
        if (o.isPresent()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(o.orElseThrow());
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> remove(@PathVariable(name = "id") Long id) {
        Optional<UserDto> o = service.findById(id);
        if (o.isPresent()) {
            service.remove(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private ResponseEntity<?> validation(BindingResult result) {
        Map<String, String> errors = new HashMap<>();
        result.getFieldErrors().forEach(err -> {
            errors.put(err.getField(), "El campo " + err.getField() + " " + err.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Endpoint para validar un token JWT
     * Ejemplo de body: { "token": "Bearer eyJ..." }
     */
    @PostMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody TokenRequest request) {
        Map<String, Object> resp = new HashMap<>();
        if (service instanceof com.cotic.backend.usersapp.backend_usersapp.services.UserServiceImpl) {
            com.cotic.backend.usersapp.backend_usersapp.services.UserServiceImpl impl = (com.cotic.backend.usersapp.backend_usersapp.services.UserServiceImpl) service;
            com.cotic.backend.usersapp.backend_usersapp.services.TokenValidationResult result = impl
                    .validateTokenDetails(request.token);
            resp.put("valid", result.isValid());
            resp.put("username", result.getUsername());
            resp.put("roles", result.getRoles());
        } else {
            resp.put("valid", false);
        }
        return ResponseEntity.ok(resp);
    }
}
