package com.fjgoncalves.occultismjars;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PLAY_CRUSHING_SOUND = BUILDER
            .comment("Whether a spirit jar plays a sound while it works.")
            .define("playCrushingSound", true);

    public static final ModConfigSpec.BooleanValue RENDER_TRAPPED_SPIRIT = BUILDER
            .comment("Whether the captured spirit is drawn inside the jar and in its screen.")
            .define("renderTrappedSpirit", true);

    public static final ModConfigSpec.DoubleValue JAR_TIME_MULTIPLIER = BUILDER
            .comment("Extra multiplier on a jar's crushing time, on top of Occultism's per-tier setting. Lower is faster.")
            .defineInRange("jarTimeMultiplier", 1.0D, 0.05D, 20.0D);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
