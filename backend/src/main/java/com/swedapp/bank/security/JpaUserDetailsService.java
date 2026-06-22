package com.swedapp.bank.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.swedapp.bank.db.repository.UserRepository;

@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public JpaUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String code) throws UsernameNotFoundException {
        var user = userRepository.findByCode(code)
                .orElseThrow(() -> new UsernameNotFoundException("No user with code: " + code));
        return User.withUsername(user.getCode())
                .password(user.getPassword())
                .authorities("ROLE_USER")
                .build();
    }
}
