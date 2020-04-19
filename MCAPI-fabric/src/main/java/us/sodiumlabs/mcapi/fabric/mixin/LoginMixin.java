package us.sodiumlabs.mcapi.fabric.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.packet.LoginHelloS2CPacket;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.packet.LoginHelloC2SPacket;
import net.minecraft.server.network.packet.LoginKeyC2SPacket;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import us.sodiumlabs.mcapi.common.BaseUtils;
import us.sodiumlabs.mcapi.common.ByteBufferUtils;
import us.sodiumlabs.mcapi.common.Signing;
import us.sodiumlabs.mcapi.common.SigningUtils;
import us.sodiumlabs.mcapi.fabric.Initializer;
import us.sodiumlabs.mcapi.fabric.service.CredInformation;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Mixin(ServerLoginNetworkHandler.class)
public class LoginMixin {
    private static final Logger LOGGER = LogManager.getLogger();

    private Optional<CredInformation> credInformation = Optional.empty();

    private boolean automated = false;

    @Shadow
    private GameProfile profile;

    @Shadow
    private SecretKey secretKey;

    @Shadow
    @Final
    private byte[] nonce;

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    @Final
    public ClientConnection client;

    // this isn't great, but see: https://github.com/SpongePowered/Mixin/issues/284
    private final static Object HELLO;
    private final static Object KEY;
    private final static Object AUTHENTICATING;
    private final static Object READY_TO_ACCEPT;

    private final static Field STATE_FIELD;

    static {
        Object hello = null;
        Object key = null;
        Object authenticating = null;
        Object readyToAccept = null;

        try {
            final Class<?> clazz = Class.forName("net.minecraft.server.network.ServerLoginNetworkHandler$State");
            for(final Object eVal: clazz.getEnumConstants()) {
                final Method nameMethod = clazz.getMethod("name");
                final Object result = nameMethod.invoke(eVal);
                if(Objects.equals(result.toString(), "HELLO")) {
                    hello = eVal;
                } else if(Objects.equals(result.toString(), "KEY")) {
                    key = eVal;
                } else if(Objects.equals(result.toString(), "AUTHENTICATING")) {
                    authenticating = eVal;
                } else if(Objects.equals(result.toString(), "READY_TO_ACCEPT")) {
                    readyToAccept = eVal;
                }
            }
        } catch (final ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Initialization failed for MC API.", e);
        }

        HELLO = requireNonNull(hello);
        KEY = requireNonNull(key);
        AUTHENTICATING = requireNonNull(authenticating);
        READY_TO_ACCEPT = requireNonNull(readyToAccept);

        try {
            STATE_FIELD = ServerLoginNetworkHandler.class.getDeclaredField("state");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Initialization failed for MC API.", e);
        }
    }

    @Inject(at = @At("HEAD"), method = "acceptPlayer()V")
    private void onAccept(final CallbackInfo info) {
        credInformation.ifPresent(c -> LOGGER.info("Accepting automated player: " + c.credId));
    }

    @Inject(at = @At("HEAD"), method = "onHello", cancellable = true)
    private void onHello(final LoginHelloC2SPacket loginHelloC2SPacket, final CallbackInfo info) {
        final String name = loginHelloC2SPacket.getProfile().getName();
        this.automated = name.startsWith("$");
        if(this.automated) {
            try {
                Validate.validState(STATE_FIELD.get(this) == HELLO, "Unexpected hello packet");

                credInformation = Initializer.getInstance().getCredCacheService().getCredInformation(name);
                profile = credInformation
                    .orElseThrow(() -> new IllegalStateException("No game profile for: " + name))
                    .toGameProfile();

                STATE_FIELD.set(this, KEY);

                client.send(new LoginHelloS2CPacket("", this.server.getKeyPair().getPublic(), this.nonce));

            } catch (final IllegalAccessException e) {
                LOGGER.error("Login failure at hello step", e);
                throw new RuntimeException("Login failure", e);
            } finally {
                info.cancel();
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "onKey", cancellable = true)
    private void onKey(final LoginKeyC2SPacket loginKeyC2SPacket, final CallbackInfo info) {
        if(this.automated) {
            try {
                Validate.validState(STATE_FIELD.get(this) == KEY, "Unexpected key packet");
                final PrivateKey privateKey = this.server.getKeyPair().getPrivate();

                // Validate input.
                final ByteBuffer payload = ByteBuffer.wrap(loginKeyC2SPacket.decryptNonce(privateKey));

                final Signing signing = Initializer.getInstance().getSigning();

                final boolean validSignature = credInformation.map(c -> c.secret)
                    .map(s -> signing.isSignaturePayloadValid(payload, ByteBuffer.wrap(nonce), s))
                    .orElse(false);

                if (!validSignature) {
                    throw new IllegalStateException("Invalid signature.");
                }

                // Set connection to ready to accept.
                STATE_FIELD.set(this, READY_TO_ACCEPT);

                // Set up encryption.
                this.secretKey = loginKeyC2SPacket.decryptSecretKey(privateKey);
                this.client.setupEncryption(this.secretKey);
            } catch (final IllegalAccessException e) {
                LOGGER.error("Login failure at key step", e);
                throw new RuntimeException("Login failure", e);
            } finally {
                info.cancel();
            }
        }
    }
}
