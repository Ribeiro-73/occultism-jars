package com.fjgoncalves.occultismjars.content;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public enum SpiritJob {
    CRUSHER("crusher", "occultism:crush_tier", 4),
    SMELTER("smelter", "occultism:smelt_tier", 4),
    CRYSTALLIZER("crystallizer", "occultism:crystal_tier", 4),
    TRADER("trader", List.of("occultism:trader_otherstone", "occultism:trader_otherrock",
            "occultism:trader_otherworld_saplings", "occultism:gambler"), List.of()),
    // no job in their data at all, they're recognised by what they are
    PARTNER("partner", List.of(), List.of("occultism:demonic_wife", "occultism:demonic_husband"));

    private final String name;
    // tiered jobs: "occultism:crush_tier" + n
    private final String factoryPrefix;
    // untiered jobs: the exact factory ids, or the entity ids for spirits without one
    private final List<String> factoryIds;
    private final List<String> entityIds;
    private final int maxTier;

    SpiritJob(String name, String factoryPrefix, int maxTier) {
        this.name = name;
        this.factoryPrefix = factoryPrefix;
        this.factoryIds = List.of();
        this.entityIds = List.of();
        this.maxTier = maxTier;
    }

    SpiritJob(String name, List<String> factoryIds, List<String> entityIds) {
        this.name = name;
        this.factoryPrefix = null;
        this.factoryIds = factoryIds;
        this.entityIds = entityIds;
        this.maxTier = 1;
    }

    public String getName() {
        return this.name;
    }

    public int maxTier() {
        return this.maxTier;
    }

    // jars only hold the lower half of a job's tiers, so untiered jobs don't fit at all
    public int jarMaxTier() {
        return this.maxTier / 2;
    }

    // "Foliot Crusher", "Foliot Otherstone Trader", "Demonic Wife"...
    public Component describe(String entityId, String factoryId) {
        Component spirit = Component.translatable("entity." + entityId.replace(':', '.'));
        if (!this.entityIds.isEmpty()) {
            return spirit;
        }
        // traders already have their own names in Occultism
        Component job = this.factoryPrefix == null && !factoryId.isEmpty()
                ? Component.translatable("job." + factoryId.replace(':', '.'))
                : Component.translatable("job.occultismjars." + this.name);
        return Component.translatable("tooltip.occultismjars.spirit_name", spirit, job);
    }

    // the job id Occultism keeps in a spirit's saved data, "" if there is none
    public static String factoryIdOf(CompoundTag entityData) {
        return entityData.getCompoundOrEmpty("spiritJob").getStringOr("factoryId", "");
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
        if (factoryId.isEmpty()) {
            return null;
        }
        for (SpiritJob job : values()) {
            if (job.factoryPrefix != null ? factoryId.startsWith(job.factoryPrefix) : job.factoryIds.contains(factoryId)) {
                return job;
            }
        }
        return null;
    }

    // from a whole saved entity: its job id first, then what the entity is
    @Nullable
    public static SpiritJob of(CompoundTag entityData) {
        SpiritJob job = byFactoryId(factoryIdOf(entityData));
        if (job != null) {
            return job;
        }
        String entityId = entityData.getStringOr("id", "");
        for (SpiritJob candidate : values()) {
            if (candidate.entityIds.contains(entityId)) {
                return candidate;
            }
        }
        return null;
    }

    // "occultism:crush_tier3" -> 3, untiered jobs are always 1, 0 if it can't be read
    public int tierOf(String factoryId) {
        if (this.factoryPrefix == null) {
            return this.factoryIds.contains(factoryId) ? 1 : 0;
        }
        try {
            return Integer.parseInt(factoryId.substring(this.factoryPrefix.length()));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return 0;
        }
    }

    public int tierOf(CompoundTag entityData) {
        if (!this.entityIds.isEmpty()) {
            return this.entityIds.contains(entityData.getStringOr("id", "")) ? 1 : 0;
        }
        return this.tierOf(factoryIdOf(entityData));
    }
}
