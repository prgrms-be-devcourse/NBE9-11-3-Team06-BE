package com.back.nbe9112team06.global.security

import org.springframework.security.core.userdetails.User

// TODO java코드에서는 생성자를 따로 만들었지만 kotlin에서는 이미 존재한다고 경고 추후 문제 생길 가능성 OK
class SecurityUser(
    val id: Int,
    val nickname: String
) : User(
    id.toString(),
    "{noop}",
    emptyList()
)