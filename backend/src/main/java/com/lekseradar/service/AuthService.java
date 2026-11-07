package com.lekseradar.service;

import com.lekseradar.dto.auth.AuthResponseDTO;
import com.lekseradar.dto.auth.LoginRequestDTO;
import com.lekseradar.dto.auth.RegisterRequestDTO;
import com.lekseradar.dto.auth.RegisterStartedResponseDTO;
import com.lekseradar.dto.auth.VerifyEmailRequestDTO;
import com.lekseradar.dto.user.UserResponseDTO;
import com.lekseradar.exception.BadRequestException;
import com.lekseradar.exception.ConflictException;
import com.lekseradar.exception.TooManyRequestsException;
import com.lekseradar.exception.UnauthorizedException;
import com.lekseradar.model.UserEntity;
import com.lekseradar.repository.UserEventRepository;
import com.lekseradar.repository.UserRepository;
import com.lekseradar.security.JwtService;
import com.lekseradar.security.RateLimiter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Регистрация в два шага: {@link #register} шлёт 6-значный код на email, {@link #verify} по коду создаёт аккаунт.
 * В {@code users} попадают только подтверждённые адреса, поэтому вход ({@link #login}) — обычный email + пароль.
 * Неподтверждённые регистрации живут в памяти 15 минут (после перезапуска сервера — зарегистрироваться заново).
 */
@Service
public class AuthService {
    /** Перебор пароля: после 5 неверных входов на один email за 15 минут вход на него закрыт до конца окна. */
    static final int MAX_FAILED_LOGINS = 5;
    static final Duration FAILED_LOGIN_WINDOW = Duration.ofMinutes(15);

    static final Duration CODE_TTL = Duration.ofMinutes(15);
    static final int MAX_CODE_ATTEMPTS = 5;
    /** Письма: не больше 3 на один адрес и 10 с одного IP за 15 минут — чтобы через нас не слали спам. */
    static final int MAX_CODES_PER_EMAIL = 3;
    static final int MAX_CODES_PER_IP = 10;
    static final int MAX_PENDING = 10_000;

    private final UserRepository users;
    private final UserEventRepository events;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RateLimiter limiter;
    private final MailService mail;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    /** Хеш для проверки, когда email не найден: ответ занимает столько же времени и не выдаёт, есть ли такой ученик. */
    private final String dummyHash;

    /** Регистрация, ждущая код из письма. codeHash — SHA-256 кода, сам код не хранится. */
    private record Pending(String passwordHash, String displayName, byte[] codeHash, Instant expiresAt, int attempts) {
        Pending failed() {
            return new Pending(passwordHash, displayName, codeHash, expiresAt, attempts + 1);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(UserRepository users, UserEventRepository events, PasswordEncoder encoder, JwtService jwt,
                       RateLimiter limiter, MailService mail) {
        this(users, events, encoder, jwt, limiter, mail, Clock.systemUTC());
    }

    AuthService(UserRepository users, UserEventRepository events, PasswordEncoder encoder, JwtService jwt,
                RateLimiter limiter, MailService mail, Clock clock) {
        this.users = users;
        this.events = events;
        this.encoder = encoder;
        this.jwt = jwt;
        this.limiter = limiter;
        this.mail = mail;
        this.clock = clock;
        this.dummyHash = encoder.encode("timing-equalizer-not-a-password");
    }

    /** Шаг 1: проверить данные и отправить код. Повторный вызов отправляет новый код (старый перестаёт действовать). */
    public RegisterStartedResponseDTO register(RegisterRequestDTO req, String ip) {
        String email = normalizeEmail(req.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }
        if (limiter.count("register-mail:" + email, CODE_TTL) >= MAX_CODES_PER_EMAIL
                || !limiter.tryAcquire("register-ip:" + ip, MAX_CODES_PER_IP, CODE_TTL)) {
            throw new TooManyRequestsException("Слишком много писем с кодом. Попробуйте через 15 минут",
                    CODE_TTL.toSeconds());
        }
        limiter.record("register-mail:" + email);
        prune();

        String code = "%06d".formatted(random.nextInt(1_000_000));
        String name = req.displayName().trim();
        pending.put(email, new Pending(encoder.encode(req.password()), name, sha256(code),
                clock.instant().plus(CODE_TTL), 0));
        try {
            mail.sendVerificationCode(email, name, code, CODE_TTL.toMinutes());
        } catch (RuntimeException e) {
            pending.remove(email);
            throw e;
        }
        return new RegisterStartedResponseDTO(email, CODE_TTL.toSeconds());
    }

    /** Шаг 2: код из письма верный → аккаунт создан, сразу вход. */
    @Transactional
    public AuthResponseDTO verify(VerifyEmailRequestDTO req) {
        String email = normalizeEmail(req.email());
        String code = req.code().strip();
        Pending p = pending.get(email);
        if (p == null || clock.instant().isAfter(p.expiresAt())) {
            pending.remove(email);
            throw new BadRequestException("Код устарел или не запрашивался. Зарегистрируйтесь ещё раз");
        }
        if (!MessageDigest.isEqual(sha256(code), p.codeHash())) {
            Pending next = p.failed();
            if (next.attempts() >= MAX_CODE_ATTEMPTS) {
                pending.remove(email);
                throw new BadRequestException("Слишком много неверных кодов. Зарегистрируйтесь ещё раз");
            }
            pending.put(email, next);
            throw new BadRequestException("Неверный код. Осталось попыток: " + (MAX_CODE_ATTEMPTS - next.attempts()));
        }
        pending.remove(email);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }
        UserEntity user = new UserEntity(email, p.passwordHash(), p.displayName());
        user.markLoggedIn();
        users.save(user);
        events.add(user.getId(), "login", null, null);
        return toResponse(user);
    }

    @Transactional
    public AuthResponseDTO login(LoginRequestDTO req) {
        String email = normalizeEmail(req.email());
        String key = "login-failed:" + email;
        if (limiter.count(key, FAILED_LOGIN_WINDOW) >= MAX_FAILED_LOGINS) {
            throw new TooManyRequestsException("Слишком много неверных попыток входа. Попробуйте через 15 минут");
        }
        UserEntity user = users.findByEmailIgnoreCase(email).orElse(null);
        boolean ok = encoder.matches(req.password(), user == null ? dummyHash : user.getPasswordHash());
        if (!ok) user = null;
        if (user == null) {
            limiter.record(key);
            if (pending.containsKey(email)) {
                throw new UnauthorizedException("Почта ещё не подтверждена: введите код из письма");
            }
            throw new UnauthorizedException("Неверный email или пароль");
        }
        limiter.reset(key);
        user.markLoggedIn();
        events.add(user.getId(), "login", null, null);
        return toResponse(user);
    }

    private AuthResponseDTO toResponse(UserEntity user) {
        return new AuthResponseDTO(jwt.issue(user.getId()), UserResponseDTO.from(user));
    }

    private void prune() {
        Instant now = clock.instant();
        pending.values().removeIf(p -> now.isAfter(p.expiresAt()));
        if (pending.size() > MAX_PENDING) pending.clear();   // защита памяти
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
