package com.back.nbe9112team06.domain.member.entity;

import com.back.nbe9112team06.domain.meeting.entity.Meeting;
import com.back.nbe9112team06.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Member extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimezoneType timezone;

    @OneToMany(mappedBy = "member", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}
            , orphanRemoval = true)
    private List<Meeting> meetings = new ArrayList<>();

    public Member(String email, String passwordHash, String nickname, TimezoneType timezone) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.timezone = timezone;
    }
    public Member(int id, String nickname) {
        this.setId(id);
        this.nickname = nickname;
    }

    public String getName() {
        return nickname;
    }
}
