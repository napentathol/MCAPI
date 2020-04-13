package us.sodiumlabs.mcapi.fabric.service;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class CredInformation {
    public final String credId;
    public final String name;
    public final String secret;
    public final UUID uuid;
    public final UUID ownerUuid;

    public CredInformation(
        final String credId,
        final String name,
        final String secret,
        final UUID uuid,
        final UUID ownerUuid
    ) {
        this.credId = requireNonNull(credId);
        this.name = requireNonNull(name);
        this.secret = requireNonNull(secret);
        this.uuid = requireNonNull(uuid);
        this.ownerUuid = requireNonNull(ownerUuid);
    }

    public GameProfile toGameProfile() {
        return new GameProfile(uuid, name);
    }
}
