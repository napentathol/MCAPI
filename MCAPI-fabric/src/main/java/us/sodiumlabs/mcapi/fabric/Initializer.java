package us.sodiumlabs.mcapi.fabric;

import com.google.common.annotations.VisibleForTesting;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import us.sodiumlabs.mcapi.common.Signing;
import us.sodiumlabs.mcapi.fabric.commands.APICommands;
import us.sodiumlabs.mcapi.fabric.service.CredCacheService;

import java.io.File;
import java.security.SecureRandom;
import java.time.Clock;

import static java.util.Objects.requireNonNull;

public class Initializer implements DedicatedServerModInitializer {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final File API_KEY_FILE = new File("api-keys.json");

    private static Container instance;

    public static Container getInstance() {
        if(instance == null) {
            throw new RuntimeException("Instance already initialized.");
        }
        return instance;
    }

    @VisibleForTesting
    static void setInstance(final Container container) {
        instance = requireNonNull(container);
    }

    public static class Container {
        private final Signing signing;

        private final CredCacheService credCacheService;

        private Container(final Signing signing, CredCacheService credCacheService) {
            this.signing = requireNonNull(signing);
            this.credCacheService = requireNonNull(credCacheService);
        }

        public Signing getSigning() {
            return signing;
        }

        public CredCacheService getCredCacheService() {
            return credCacheService;
        }
    }

    @Override
    public void onInitializeServer() {
        LOGGER.info("Initializing MCAPI...");
        final Signing signing = new Signing(Clock.systemUTC());
        LOGGER.info("MCAPI loading creds...");
        final CredCacheService credCacheService = new CredCacheService(API_KEY_FILE, new SecureRandom());
        credCacheService.init();
        LOGGER.info("MCAPI creds loaded...");

        setInstance(new Container(signing, credCacheService));

        LOGGER.info("MCAPI registering commands...");
        final MinecraftServer minecraftServer = (MinecraftServer) FabricLoader.getInstance().getGameInstance();
        APICommands.register(minecraftServer.getCommandManager(), credCacheService);
        LOGGER.info("MCAPI init finished!");
    }
}
