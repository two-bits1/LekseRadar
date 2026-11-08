package com.lekseradar.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

/**
 * Ключи и пароли, которые когда-то были значениями по умолчанию в репозитории и поэтому публичны.
 * Хранятся только их SHA-256, чтобы сами значения не возвращались в код. Бэкенд с таким ключом не запускается.
 */
public final class InsecureSecrets {
    private static final Set<String> SHA256 = Set.of(
            "8b63d14b57a7edb6d8d0da4450297ff3300598f249eab0122349536b1c2dcba4",   // старый ключ collector
            "7e0133c2faa55173fabff3ebd2901e21c39dc391767a5aefd34f4be19b391c24",   // старый JWT_SECRET docker compose
            "d4221cef54faacab2fe75b92cc914c80d7f287930f28eaad87543f860a2bd547",   // старый CREDENTIALS_KEY docker compose
            "04bd465510c0000c5134936aae8cb2ba57b3870e34bf7fc4830d7591900fc037");  // пример JWT_SECRET из .env.example

    private InsecureSecrets() {}

    public static boolean isKnown(String secret) {
        if (secret == null) return false;
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(secret.strip().getBytes(StandardCharsets.UTF_8));
            return SHA256.contains(HexFormat.of().formatHex(d));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** @throws IllegalStateException если значение публичное */
    public static void reject(String name, String secret) {
        if (isKnown(secret)) {
            throw new IllegalStateException(name + " — публичное значение из репозитория. Задайте свой: "
                    + "openssl rand -base64 32 (docker compose создаёт ключи сам, если в .env пусто)");
        }
    }
}
