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

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivateKey;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Mixin(ServerLoginNetworkHandler.class)
public class LoginMixin {
    private static final Logger LOGGER = LogManager.getLogger();

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
    }

    @Inject(at = @At("HEAD"), method = "acceptPlayer()V")
    private void onAccept(final CallbackInfo info) {
        System.out.println("acceptPlayer");
    }

    @Inject(at = @At("HEAD"), method = "onHello", cancellable = true)
    private void onHello(final LoginHelloC2SPacket loginHelloC2SPacket, final CallbackInfo info) {
        System.out.println("onHello: " + info.isCancellable());
        try {
            final Field stateField = ServerLoginNetworkHandler.class.getDeclaredField("state");
            Validate.validState(stateField.get(this) == HELLO, "Unexpected hello packet");

            profile = loginHelloC2SPacket.getProfile();

            stateField.set(this, KEY);

            client.send(new LoginHelloS2CPacket("", this.server.getKeyPair().getPublic(), this.nonce));
            info.cancel();
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Inject(at = @At("HEAD"), method = "onKey", cancellable = true)
    private void onKey(final LoginKeyC2SPacket loginKeyC2SPacket, final CallbackInfo info) {
        System.out.println("onKey: " + info.isCancellable());
        try {
            final Field stateField = ServerLoginNetworkHandler.class.getDeclaredField("state");
            Validate.validState(stateField.get(this) == KEY, "Unexpected key packet");
            stateField.set(this, READY_TO_ACCEPT);

            final PrivateKey privateKey = this.server.getKeyPair().getPrivate();
            this.secretKey = loginKeyC2SPacket.decryptSecretKey(privateKey);
            this.client.setupEncryption(this.secretKey);
            info.cancel();
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
