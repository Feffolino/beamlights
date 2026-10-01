package it.ratlab.beamlights.core;

/** Decides whether the gamma mask draws the local player's beam (pure, no Minecraft classes). */
public final class MaskPolicy {
    public enum Reason {
        ACTIVE("active"),
        DISABLED("off in config"),
        FAILED("render failed"),
        SHADER_PACK("shader pack in use"),
        NOT_FIRST_PERSON("not first person"),
        NO_BEAM("no local beam");

        public final String text;

        Reason(String text) {
            this.text = text;
        }
    }

    private MaskPolicy() {
    }

    /** First failing check wins; ACTIVE only when every check passes. */
    public static Reason decide(boolean enabled, boolean failed, boolean shaderPack, boolean firstPerson,
                                boolean hasBeam) {
        if (!enabled) return Reason.DISABLED;
        if (failed) return Reason.FAILED;
        if (shaderPack) return Reason.SHADER_PACK;
        if (!firstPerson) return Reason.NOT_FIRST_PERSON;
        if (!hasBeam) return Reason.NO_BEAM;
        return Reason.ACTIVE;
    }
}
