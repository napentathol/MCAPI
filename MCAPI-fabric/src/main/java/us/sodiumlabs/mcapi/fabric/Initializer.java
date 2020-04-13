package us.sodiumlabs.mcapi.fabric;

import com.google.common.annotations.VisibleForTesting;
import net.fabricmc.api.DedicatedServerModInitializer;
import us.sodiumlabs.mcapi.common.Signing;
import us.sodiumlabs.mcapi.fabric.service.CredCacheService;

import java.io.File;
import java.security.SecureRandom;
import java.time.Clock;

import static java.util.Objects.requireNonNull;

public class Initializer implements DedicatedServerModInitializer {
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
        setInstance(new Container(
            new Signing(Clock.systemUTC()),
            new CredCacheService(API_KEY_FILE, new SecureRandom())));
    }
}
