package com.tourismqa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tourismqa.config.AppProperties;
import com.tourismqa.dto.RegisterRequest;
import com.tourismqa.dto.UserProfileResponse;
import com.tourismqa.entity.UserAccount;
import com.tourismqa.repository.UserAccountRepository;
import com.tourismqa.security.JwtService;
import com.tourismqa.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private DatabaseConnectionService databaseConnectionService;

    @Mock
    private UserAccountService userAccountService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getSecurity().setFirstUserAdmin(false);

        authService = new AuthService(
                userAccountRepository,
                passwordEncoder,
                jwtService,
                databaseConnectionService,
                appProperties,
                userAccountService
        );

        when(passwordEncoder.encode("abc123")).thenReturn("encoded-password");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("token");
        when(userAccountService.toProfileResponse(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            return new UserProfileResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRole().name(),
                    false,
                    null,
                    null,
                    null,
                    java.util.List.of(),
                    "STANDARD",
                    false
            );
        });
    }

    @Test
    void register_allowsMissingEmail() {
        authService.register(new RegisterRequest("testuser", "", "abc123", "测试用户"));

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isNull();
        verify(userAccountRepository, never()).existsByEmail(any());
    }

    @Test
    void register_normalizesOptionalEmailWhenProvided() {
        authService.register(new RegisterRequest("testuser", " User@Example.COM ", "abc123", "测试用户"));

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        verify(userAccountRepository).existsByEmail("user@example.com");
    }
}
