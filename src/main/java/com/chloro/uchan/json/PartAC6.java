package com.chloro.uchan.json;

import com.chloro.uchan.enums.Slot;
import com.chloro.uchan.enums.SlotAC6;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PartAC6 extends Part {

    private final List<SlotAC6> slots;

    public PartAC6(@JsonProperty("game") String game,
                   @JsonProperty("category") String category,
                   @JsonProperty("slots") List<SlotAC6> slots,
                   @JsonProperty("name") String name,
                   @JsonProperty("type") String type,
                   @JsonProperty("weight") @JsonAlias("Weight") Long weight,
                   @JsonProperty("loadLimit") @JsonAlias("Load Limit") Long loadLimit,
                   @JsonProperty("enLoad") @JsonAlias("EN Load") Long enLoad,
                   @JsonProperty("enAdjust") @JsonAlias("Generator Output Adj.") Long enAdjust,
                   @JsonProperty("armLoadLimit") @JsonAlias("Arms Load Limit") Long armLoadLimit,
                   @JsonProperty("enOutput") @JsonAlias("EN Output") Long enOutput) {
        super(game, category, name, type, weight, loadLimit, enLoad, enAdjust, armLoadLimit, enOutput);
        this.slots = slots;
    }

    @Override
    public List<Slot> getSlots() {
        return slots.stream().map(slotAC6 -> (Slot)slotAC6).toList();
    }
}
