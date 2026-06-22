package com.swedapp.bank.domain;

import java.time.LocalDate;

public record PersonDetails(String firstname, String lastname, LocalDate birthday, String nationality) implements UserDetails {
}
