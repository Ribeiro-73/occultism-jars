package com.fjgoncalves.occultismjars.content;

import net.minecraft.resources.ResourceLocation;

public enum CrusherType {
    FOLIOT("foliot", 1),
    DJINNI("djinni", 2),
    AFRIT("afrit", 3),
    MARID("marid", 4);

    public static final String CRUSHER_JOB_PREFIX = "occultism:crush_tier";

    private final String spirit;
    private final int tier;

    CrusherType(String spirit, int tier) {
        this.spirit = spirit;
        this.tier = tier;
    }

    public String spirit() {
        return this.spirit;
    }

    public int tier() {
        return this.tier;
    }

    public ResourceLocation entityId() {
        return ResourceLocation.fromNamespaceAndPath("occultism", this.spirit);
    }

    public static CrusherType byEntityId(ResourceLocation id) {
        if (id == null || !id.getNamespace().equals("occultism")) {
            return null;
        }
        for (CrusherType type : values()) {
            if (type.spirit.equals(id.getPath())) {
                return type;
            }
        }
        return null;
    }

    public static CrusherType byTier(int tier) {
        for (CrusherType type : values()) {
            if (type.tier == tier) {
                return type;
            }
        }
        return null;
    }
}
