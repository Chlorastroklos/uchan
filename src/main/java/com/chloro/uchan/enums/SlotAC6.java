package com.chloro.uchan.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum SlotAC6 implements Slot {
    R_ARM_UNIT("R-ARM UNIT"),
    L_ARM_UNIT("L-ARM UNIT"),
    R_BACK_UNIT("R-BACK UNIT"),
    L_BACK_UNIT("L-BACK UNIT"),
    HEAD("HEAD"),
    CORE("CORE"),
    ARMS("ARMS"),
    LEGS("LEGS"),
    BOOSTER("BOOSTER"),
    FCS("FCS"),
    GENERATOR("GENERATOR"),
    EXPANSION("EXPANSION");

    private final String name;

    SlotAC6(final String name) {
        this.name = name;
    }

    @JsonCreator
    public static SlotAC6 fromString(final String string) {
        return Arrays.stream(SlotAC6.values())
                .filter(slot -> slot.name.equals(string))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Long getOrdinal() {
        return Long.valueOf(ordinal());
    }
}
