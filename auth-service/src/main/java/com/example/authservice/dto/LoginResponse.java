package com.example.authservice.dto;

public record LoginResponse(String accessToken, String name, String userUuid) {}
