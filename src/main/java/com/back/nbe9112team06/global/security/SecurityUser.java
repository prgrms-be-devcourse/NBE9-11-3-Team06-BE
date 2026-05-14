package com.back.nbe9112team06.global.security;

import lombok.Getter;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class SecurityUser extends User {

    private final int id;
    private final String nickname;

    public SecurityUser(int id, String nickname) {
        super(
                String.valueOf(id),
                "{noop}",
                List.of()
        );
        this.id = id;
        this.nickname = nickname;
    }
}