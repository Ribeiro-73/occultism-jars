package com.fjgoncalves.occultismjars.content;

import net.minecraft.network.chat.Component;

public enum SpiritJob {
    CRUSHER("crusher", "occultism:crush_tier", 4),
    SMELTER("smelter", "occultism:smelt_tier", 4),
    CRYSTALLIZER("crystallizer", "occultism:crystal_tier", 4);

    private final String name;
    private final String factoryPrefix;
    private final int maxTier;

    SpiritJob(String name, String factoryPrefix, int maxTier) {
        this.name = name;
        this.factoryPrefix = factoryPrefix;
        this.maxTier = maxTier;
    }

    public String getName() {
        return this.name;
    }

    public int maxTier() {
        return this.maxTier;
    }

    // jars only hold the lower half of a job's tiers
    public int jarMaxTier() {
        return this.maxTier / 2;
    }

    // "Foliot Crusher", "Djinni Smelter"...
    public Component describe(String entityId) {
        Component spirit = Component.translatable("entity." + entityId.replace(':', '.'));
        return Component.translatable("tooltip.occultismjars.spirit_name", spirit,
                Component.translatable("job.occultismjars." + this.name));
    }

    public static SpiritJob byName(String name) {
        for (SpiritJob job : values()) {
            if (job.name.equals(name)) {
                return job;
            }
        }
        return null;
    }

    public static SpiritJob byFactoryId(String factoryId) {
        for (SpiritJob job : values()) {
            if (factoryId.startsWith(job.factoryPrefix)) {
                return job;
            }
        }
        return null;
    }

    // "occultism:crush_tier3" -> 3, 0 if it can't be read
    public int tierOf(String factoryId) {
        try {
            return Integer.parseInt(factoryId.substring(this.factoryPrefix.length()));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return 0;
        }
    }
}
