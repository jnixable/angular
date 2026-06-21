package com.swedapp.bank.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.swedapp.bank.db.repository.UserRepository;
import com.swedapp.bank.security.JwtService;
import com.swedapp.bank.web.dto.LoginRequest;
import com.swedapp.bank.web.dto.LoginResponse;
import com.swedapp.bank.web.dto.WhoAmIResponse;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public UserController(AuthenticationManager authenticationManager, JwtService jwtService,
                          UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.pcode(), request.password()));
        var token = jwtService.generateToken(authentication.getName());
        return new LoginResponse(token, jwtService.getExpirationMs() / 1000);
    }

    @GetMapping("/whoami")
    public WhoAmIResponse whoami(Authentication authentication) {
        var user = userRepository.findByPcode(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user with pcode: " + authentication.getName()));
        return new WhoAmIResponse(
                user.getPcode(), user.getFirstname(), user.getLastname(), user.getEmail());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleAuthenticationException(AuthenticationException ex) {
        return Map.of("error", "Invalid credentials");
    }
}
