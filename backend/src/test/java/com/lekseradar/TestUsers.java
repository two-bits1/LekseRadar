package com.lekseradar;

import com.lekseradar.dto.auth.AuthResponseDTO;
import com.lekseradar.dto.auth.RegisterRequestDTO;
import com.lekseradar.dto.auth.VerifyEmailRequestDTO;
import com.lekseradar.service.AuthService;
import com.lekseradar.service.MailService;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/** Регистрация в интеграционных тестах: MailService замокан, код берётся из «письма». */
public final class TestUsers {
    private TestUsers() {}

    public static AuthResponseDTO register(AuthService auth, MailService mail, String email, String password, String name) {
        auth.register(new RegisterRequestDTO(email, password, name, true), "test-" + System.nanoTime());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mail, atLeastOnce()).sendVerificationCode(eq(email), anyString(), code.capture(), anyLong());
        return auth.verify(new VerifyEmailRequestDTO(email, code.getValue()));
    }
}
