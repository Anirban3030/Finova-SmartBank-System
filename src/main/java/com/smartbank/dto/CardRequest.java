// CardRequest.java
package com.smartbank.dto;

import com.smartbank.entity.CardType;
import jakarta.validation.constraints.NotNull;

public record CardRequest(@NotNull(message = "Card type is required") CardType cardType) {}