package com.back.nbe9112team06.domain.member.entity;

public enum TimezoneType {
    ASIA_SEOUL("Asia/Seoul"),      // name: "ASIA_SEOUL", value: "Asia/Seoul"
    UTC("UTC"),
    AMERICA_NEW_YORK("America/New_York"),
    AMERICA_LOS_ANGELES("America/Los_Angeles");

    private String value;
    TimezoneType(String value) { this.value = value; }
    public String getValue() { return value; }
}
