package com.chloro.uchan.json;

import com.chloro.uchan.enums.Slot;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;


@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "game")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PartAC6.class, name = "Armored Core 6: Fires of Rubicon")
})
public abstract class Part {

    private final String game;
    private final String category;
    private final String name;
    private final String type;
    private final Long weight;
    private final Long loadLimit;
    private final Long enLoad;
    private final Long enAdjust;
    private final Long armLoadLimit;
    private final Long enOutput;

    public Part(String game, String category, String name, String type,
                Long weight, Long loadLimit, Long enLoad, Long enAdjust, Long armLoadLimit, Long enOutput) {
        this.game = game;
        this.category = category;
        this.name = name;
        this.type = type;
        this.weight = weight;
        this.loadLimit = loadLimit;
        this.enLoad = enLoad;
        this.enAdjust = enAdjust;
        this.armLoadLimit = armLoadLimit;
        this.enOutput = enOutput;
    }

    public String getGame() {
        return game;
    }

    public String getCategory() {
        return category;
    }

    public abstract List<Slot> getSlots();

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Long getWeight() {
        return weight;
    }

    public Long getEnLoad() {
        return enLoad;
    }

    public Long getLoadLimit() {
        return loadLimit;
    }

    public Long getEnAdjust() {
        return enAdjust;
    }

    public Long getArmLoadLimit() {
        return armLoadLimit;
    }

    public Long getEnOutput() {
        return enOutput;
    }
}
