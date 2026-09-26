package com.fjgoncalves.occultismjars.content;

import net.minecraft.resources.Identifier;

public enum SpiritType {
    FOLIOT("foliot", 1),
    DJINNI("djinni", 2),
    AFRIT("afrit", 3),
    MARID("marid", 4);

    private final String spirit;
    private final int tier;

    SpiritType(String spirit, int tier) {
        this.spirit = spirit;
        this.tier = tier;
    }

    public String spirit() {
        return this.spirit;
    }

    public int tier() {
        return this.tier;
    }

    public Identifier entityId() {
        return Identifier.fromNamespaceAndPath("occultism", this.spirit);
    }

    public static SpiritType byEntityId(Identifier id) {
        if (id == null || !id.getNamespace().equals("occultism")) {
            return null;
        }
        for (SpiritType type : values()) {
            if (type.spirit.equals(id.getPath())) {
                return type;
            }
        }
        return null;
    }

    public static SpiritType byTier(int tier) {
        for (SpiritType type : values()) {
            if (type.tier == tier) {
                return type;
            }
        }
        return null;
    }
}
