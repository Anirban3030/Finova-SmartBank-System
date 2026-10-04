// CustomerResponse.java   (note: no password field, ever)
package com.smartbank.dto;

import com.smartbank.entity.Role;

public record CustomerResponse(Long id, String name, String email, String phone, Role role) {}