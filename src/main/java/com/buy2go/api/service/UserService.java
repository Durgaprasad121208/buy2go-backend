package com.buy2go.api.service;

import com.buy2go.api.dto.PagedResponse;
import com.buy2go.api.dto.response.UserResponseDTO;

public interface UserService {
    PagedResponse<UserResponseDTO> getAllUsers(int page, int size, String search);
    UserResponseDTO getUserById(String id);
    void deleteUser(String id);
}
