package com.lekseradar.dto.platform;

/**
 * {@code POST /platforms/teams/start}: ученик открывает verificationUri и вводит userCode,
 * фронт раз в interval секунд вызывает {@code /platforms/teams/poll} с deviceCode.
 *
 * @param expiresIn через сколько секунд код перестанет работать
 */
public record TeamsStartResponseDTO(String userCode, String verificationUri, String deviceCode, int interval,
                                    int expiresIn) {}
