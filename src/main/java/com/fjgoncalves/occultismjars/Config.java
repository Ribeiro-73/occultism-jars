package com.fjgoncalves.occultismjars;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue VERBOSE_LOGGING = BUILDER
            .comment("Log extra Occultism Jars information to the console.")
            .define("verboseLogging", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
