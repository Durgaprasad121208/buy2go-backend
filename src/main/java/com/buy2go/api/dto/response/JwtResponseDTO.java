package com.buy2go.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponseDTO {

    private String token;
    @Builder.Default
    private String type = "Bearer";
    private String id;
    private String userName;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}
