package com.buy2go.api.service.impl;

import com.buy2go.api.config.JwtTokenProvider;
import com.buy2go.api.constants.Constants;
import com.buy2go.api.entity.UserAuthEntity;
import com.buy2go.api.entity.UserEntity;
import com.buy2go.api.dto.request.ChangePasswordRequestDTO;
import com.buy2go.api.dto.request.LoginRequestDTO;
import com.buy2go.api.dto.request.RegisterRequestDTO;
import com.buy2go.api.dto.response.JwtResponseDTO;
import com.buy2go.api.dto.response.UserResponseDTO;
import com.buy2go.api.exception.InvalidPasswordException;
import com.buy2go.api.exception.ResourceNotFoundException;
import com.buy2go.api.exception.SystemException;
import com.buy2go.api.mapper.ModelMapper;
import com.buy2go.api.repository.UserAuthRepository;
import com.buy2go.api.repository.UserRepository;
import com.buy2go.api.service.AuthService;
import com.buy2go.api.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAuthRepository userAuthRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    @Override
    public JwtResponseDTO login(LoginRequestDTO request) {
        String loginIdentifier = request.getEmail() != null ? request.getEmail() : request.getUserName();
        UserAuthEntity auth = userAuthRepository.findByEmailAndIsDeletedFalse(loginIdentifier)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!PasswordUtils.matches(request.getPassword(), auth.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = tokenProvider.generateTokenFromUsername(auth.getUsername());

        return JwtResponseDTO.builder()
                .token(token)
                .type("Bearer")
                .id(auth.getUserId())
                .email(auth.getEmail())
                .firstName(auth.getFirstName())
                .lastName(auth.getLastName())
                .role(auth.getRoleName())
                .build();
    }

    @Override
    public UserResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.findByEmailAndIsDeletedFalse(request.getEmail()).isPresent()
                || userAuthRepository.existsByEmail(request.getEmail())) {
            throw new SystemException("Email is already registered");
        }

        UserEntity user = new UserEntity();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRoleName(Constants.ROLE_CUSTOMER);
        user = userRepository.save(user);

        UserAuthEntity auth = new UserAuthEntity();
        auth.setEmail(request.getEmail());
        auth.setPassword(PasswordUtils.encode(request.getPassword()));
        auth.setFirstName(request.getFirstName());
        auth.setLastName(request.getLastName());
        auth.setUserId(user.getId());
        auth.setRoleName(Constants.ROLE_CUSTOMER);
        userAuthRepository.save(auth);

        return ModelMapper.toUserResponse(user);
    }

    @Override
    public void changePassword(String email, ChangePasswordRequestDTO request) {
        UserAuthEntity auth = userAuthRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        if (!PasswordUtils.matches(request.getOldPassword(), auth.getPassword())) {
            throw new InvalidPasswordException("Current password does not match");
        }

        auth.setPassword(PasswordUtils.encode(request.getNewPassword()));
        userAuthRepository.save(auth);
    }

    @Override
    public UserResponseDTO getCurrentUser(String email) {
        UserAuthEntity auth = userAuthRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        UserEntity user = userRepository.findById(auth.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User details not found for ID: " + auth.getUserId()));

        return ModelMapper.toUserResponse(user);
    }
}
