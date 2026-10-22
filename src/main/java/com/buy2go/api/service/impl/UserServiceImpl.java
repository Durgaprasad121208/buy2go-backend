package com.buy2go.api.service.impl;

import com.buy2go.api.entity.UserEntity;
import com.buy2go.api.dto.PagedResponse;
import com.buy2go.api.dto.response.UserResponseDTO;
import com.buy2go.api.exception.ResourceNotFoundException;
import com.buy2go.api.mapper.ModelMapper;
import com.buy2go.api.repository.UserAuthRepository;
import com.buy2go.api.repository.UserRepository;
import com.buy2go.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;

    @Override
    public PagedResponse<UserResponseDTO> getAllUsers(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<UserEntity> userPage;
        if (search != null && !search.trim().isEmpty()) {
            userPage = userRepository.searchUsers(search.trim(), pageable);
        } else {
            userPage = userRepository.findByIsDeletedFalse(pageable);
        }

        List<UserResponseDTO> content = userPage.getContent().stream()
                .map(ModelMapper::toUserResponse)
                .toList();

        return PagedResponse.<UserResponseDTO>builder()
                .content(content)
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();
    }

    @Override
    public UserResponseDTO getUserById(String id) {
        UserEntity user = userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return ModelMapper.toUserResponse(user);
    }

    @Override
    public void deleteUser(String id) {
        UserEntity user = userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        user.setDeleted(true);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);

        userAuthRepository.findByUserIdAndIsDeletedFalse(id).ifPresent(auth -> {
            auth.setDeleted(true);
            auth.setDeletedAt(Instant.now());
            userAuthRepository.save(auth);
        });
    }
}
