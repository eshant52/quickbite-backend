package com.quickbite.quickbite.user.service;

import com.quickbite.quickbite.common.exception.ResourceConflictException;
import com.quickbite.quickbite.user.dto.CreateAdminRequest;
import com.quickbite.quickbite.user.dto.UserResponseDto;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private CreateAdminRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateAdminRequest(
                "Jane Admin",
                "jane.admin@quickbite.com",
                "+19876543210",
                "SecurePass@123"
        );
    }

    @Test
    @DisplayName("createAdmin - successfully creates and saves an admin user")
    void createAdmin_success() {
        when(userRepository.findUserByEmail(request.email())).thenReturn(Optional.empty());
        when(userRepository.findUserByPhoneNumber(request.phoneNumber())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("argon2HashedPass");

        UUID generatedId = UUID.randomUUID();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(generatedId);
            return u;
        });

        UserResponseDto response = adminUserService.createAdmin(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(generatedId);
        assertThat(response.name()).isEqualTo("Jane Admin");
        assertThat(response.email()).isEqualTo("jane.admin@quickbite.com");
        assertThat(response.phoneNumber()).isEqualTo("+19876543210");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        assertThat(response.isActive()).isTrue();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getPasswordHash()).isEqualTo("argon2HashedPass");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("createAdmin - throws ResourceConflictException when email is already taken")
    void createAdmin_emailConflict() {
        when(userRepository.findUserByEmail(request.email())).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> adminUserService.createAdmin(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createAdmin - throws ResourceConflictException when phone number is already taken")
    void createAdmin_phoneConflict() {
        when(userRepository.findUserByEmail(request.email())).thenReturn(Optional.empty());
        when(userRepository.findUserByPhoneNumber(request.phoneNumber())).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> adminUserService.createAdmin(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Phone number is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("listAdmins - returns all administrators mapped to UserResponseDto")
    void listAdmins_success() {
        User admin1 = new User();
        admin1.setId(UUID.randomUUID());
        admin1.setName("Admin One");
        admin1.setEmail("admin1@quickbite.com");
        admin1.setPhoneNumber("+11111111111");
        admin1.setRole(UserRole.ADMIN);
        admin1.setActive(true);

        User admin2 = new User();
        admin2.setId(UUID.randomUUID());
        admin2.setName("Admin Two");
        admin2.setEmail("admin2@quickbite.com");
        admin2.setPhoneNumber("+22222222222");
        admin2.setRole(UserRole.ADMIN);
        admin2.setActive(true);

        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(admin1, admin2));

        List<UserResponseDto> result = adminUserService.listAdmins();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).email()).isEqualTo("admin1@quickbite.com");
        assertThat(result.get(1).email()).isEqualTo("admin2@quickbite.com");
    }
}
