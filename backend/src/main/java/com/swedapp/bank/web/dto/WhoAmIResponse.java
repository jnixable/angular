package com.swedapp.bank.web.dto;

import com.swedapp.bank.domain.UserDetails;
import com.swedapp.bank.domain.UserType;

public record WhoAmIResponse(UserType userType, String code, UserDetails userDetails, String email) {
}
