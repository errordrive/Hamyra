package com.example.data.model;

public enum SportCategory {
    CRICKET("Cricket"),
    FOOTBALL("Football"),
    TENNIS("Tennis"),
    MOTORSPORT("MotorSport"),
    WRESTLING("Wrestling"),
    OTHER("Other");

    public final String displayName;

    SportCategory(String displayName) {
        this.displayName = displayName;
    }
}
