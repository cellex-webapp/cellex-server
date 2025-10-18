package com.example.cellex.exceptions;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AccountBannedException extends RuntimeException {
    private final String banReason;
    private final LocalDateTime bannedAt;

    public AccountBannedException(String banReason, LocalDateTime bannedAt) {
        super("Tài khoản đã bị khóa: " + banReason);
        this.banReason = banReason;
        this.bannedAt = bannedAt;
    }
}
