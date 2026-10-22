package com.buy2go.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDTO {

    private String email;
    private String password;

    // Backward compatibility if userName is passed in request payload
    public void setUserName(String userName) {
        if (this.email == null || this.email.trim().isEmpty()) {
            this.email = userName;
        }
    }

    public String getUserName() {
        return this.email;
    }
}
