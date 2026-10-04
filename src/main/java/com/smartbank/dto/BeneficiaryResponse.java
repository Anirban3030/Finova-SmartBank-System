// BeneficiaryResponse.java
package com.smartbank.dto;

import com.smartbank.entity.BeneficiaryStatus;

public record BeneficiaryResponse(Long id, String name, String accountNumber,
                                  String bankName, BeneficiaryStatus status) {}