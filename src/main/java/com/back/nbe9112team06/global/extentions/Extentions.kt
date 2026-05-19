package com.back.nbe9112team06.global.extentions

fun <T : Any> T?.getOrThrow(): T {
    return this ?: throw NoSuchElementException()
}