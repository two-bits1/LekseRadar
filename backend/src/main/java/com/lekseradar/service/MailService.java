package com.lekseradar.service;

import com.lekseradar.config.AppConfig;
import com.lekseradar.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Письма ученикам через SMTP из {@code spring.mail.*} (например, Gmail с паролем приложения).
 * Без настроенной почты: при {@code MAIL_DEV_LOG_CODES=true} код пишется в лог, иначе 503.
 */
@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> sender;
    private final String username;
    private final String from;
    private final boolean devLogCodes;
    private final String site;

    public MailService(ObjectProvider<JavaMailSender> sender, AppConfig cfg,
                       @Value("${spring.mail.username:}") String username) {
        this.sender = sender;
        this.username = username == null ? "" : username.strip();
        String f = cfg.mail() == null || cfg.mail().from() == null ? "" : cfg.mail().from().strip();
        this.from = f.isEmpty() ? this.username : f;
        this.devLogCodes = cfg.mail() != null && cfg.mail().devLogCodes();
        this.site = siteOf(cfg);
    }

    /** Адрес сайта для ссылки в письме: первый https-адрес из CORS_ORIGINS, иначе первый вообще. */
    static String siteOf(AppConfig cfg) {
        List<String> origins = cfg.cors() == null || cfg.cors().allowedOrigins() == null ? List.of() : cfg.cors().allowedOrigins();
        String pick = origins.stream().map(String::strip).filter(o -> o.startsWith("https://")).findFirst()
                .orElse(origins.isEmpty() ? "" : origins.get(0).strip());
        return pick.endsWith("/") ? pick.substring(0, pick.length() - 1) : pick;
    }

    public String site() { return site; }

    /**
     * Письмо-напоминание. Без настроенной почты: при {@code MAIL_DEV_LOG_CODES=true} текст пишется в лог
     * (считается отправленным), иначе письмо не уходит и возвращается false.
     */
    public boolean sendReminder(String to, ReminderMail.Mail mail) {
        if (!configured()) {
            if (devLogCodes) {
                log.warn("MAIL not configured (dev): reminder to {}: {}\n{}", to, mail.subject(), mail.body());
                return true;
            }
            return false;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(mail.subject());
        msg.setText(mail.body());
        try {
            sender.getObject().send(msg);
            return true;
        } catch (MailException e) {
            log.warn("reminder mail to {} failed: {}", to, e.getMessage());
            return false;
        }
    }

    public boolean configured() {
        return !username.isEmpty() && sender.getIfAvailable() != null;
    }

    /** Код подтверждения регистрации. */
    public void sendVerificationCode(String to, String name, String code, long minutes) {
        if (!configured()) {
            if (devLogCodes) {
                log.warn("MAIL not configured (dev): verification code for {} is {}", to, code);
                return;
            }
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Отправка писем не настроена на сервере (MAIL_USERNAME, MAIL_PASSWORD)");
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("LekseRadar: " + code);
        msg.setText("""
                Hei %s!

                Koden din for å bekrefte e-posten i LekseRadar: %s
                Den gjelder i %d minutter.

                Ваш код подтверждения почты в LekseRadar: %s (действует %d минут).
                Your LekseRadar verification code: %s

                Hvis du ikke har registrert deg, kan du se bort fra denne e-posten.
                """.formatted(name, code, minutes, code, minutes, code));
        try {
            sender.getObject().send(msg);
        } catch (MailException e) {
            // Чаще всего: неверный пароль приложения Gmail или нет связи с smtp.gmail.com
            log.warn("verification mail to {} failed: {}", to, e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Не удалось отправить письмо. Попробуйте позже");
        }
    }
}
