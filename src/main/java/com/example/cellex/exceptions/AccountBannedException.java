package com.example.cellex.exceptions;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AccountBannedException extends RuntimeException {
    private final String banReason;

    public AccountBannedException(String banReason) {
        super("Tài khoản đã bị khóa vì lí do: " + banReason + " vui lòng liên hệ quản trị viên");
        this.banReason = banReason;
    }
}
