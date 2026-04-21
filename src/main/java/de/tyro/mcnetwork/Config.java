package de.tyro.mcnetwork;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEV_MODE = BUILDER
            .comment("Developer Mode")
            .define("devMode", false);

    static final ModConfigSpec SPEC = BUILDER.build();

}
