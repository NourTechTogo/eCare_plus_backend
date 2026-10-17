package com.nourtech.ecareplus.identity.application.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MotDePasseService {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public String hacher(String motDePasse) {
        return passwordEncoder.encode(motDePasse);
    }

    public boolean correspond(String motDePasse, String hash) {
        return passwordEncoder.matches(motDePasse, hash);
    }
}
