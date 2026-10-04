// AuthResponse.java
package com.smartbank.dto;

public record AuthResponse(String token, String tokenType, long expiresInSeconds) {}