package com.buy2go.api.service;

import com.buy2go.api.dto.request.ChangePasswordRequestDTO;
import com.buy2go.api.dto.request.LoginRequestDTO;
import com.buy2go.api.dto.request.RegisterRequestDTO;
import com.buy2go.api.dto.response.JwtResponseDTO;
import com.buy2go.api.dto.response.UserResponseDTO;

public interface AuthService {
    JwtResponseDTO login(LoginRequestDTO request);
    UserResponseDTO register(RegisterRequestDTO request);
    void changePassword(String username, ChangePasswordRequestDTO request);
    UserResponseDTO getCurrentUser(String username);
}
