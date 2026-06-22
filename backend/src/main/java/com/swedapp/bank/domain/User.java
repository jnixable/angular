package com.swedapp.bank.domain;


/**
 * Type that represents system actor (user that can interact with the app)
 * @param userType - either person or entity user
 * @param code - numeric field (only numbers, no other characters). Either person code or business entity
 * */
public record User(UserType userType, String code, UserDetails userDetails, String email) {
}
