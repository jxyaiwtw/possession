package dev.muzu1.possession.session;

public enum PossessionReason {
    MANUAL("message.possession.detach_success"),
    TARGET_DEATH("message.possession.force_exit.target_dead"),
    TARGET_UNLOADED("message.possession.force_exit.interrupted"),
    PLAYER_DEATH("message.possession.force_exit.interrupted"),
    PLAYER_UNLOADED("message.possession.force_exit.interrupted"),
    WORLD_CHANGE("message.possession.force_exit.interrupted"),
    CHARM_MISSING("message.possession.force_exit.charm_missing"),
    TOO_FAR("message.possession.force_exit.too_far");

    private final String translationKey;

    PossessionReason(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
