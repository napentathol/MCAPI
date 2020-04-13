package us.sodiumlabs.mcapi.fabric.service;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import static java.io.File.createTempFile;
import static org.junit.Assert.assertEquals;

public class CredCacheServiceTest {
    @Test
    public void testAll() {
        File tmp = null;
        try {
            tmp = createTempFile("tmp", ".json");
            tmp.delete();

            final CredCacheService credCacheService = new CredCacheService(tmp, new Random());
            credCacheService.init();
            final CredInformation info = credCacheService.createCredInformation("bob", UUID.randomUUID());

            final CredCacheService loadingService = new CredCacheService(tmp, new Random());
            loadingService.init();
            final CredInformation loaded = credCacheService.getCredInformation(info.credId)
                .orElseThrow(() -> new AssertionError("Did not load creds."));

            assertEquals(info.credId, loaded.credId);
            assertEquals(info.name, loaded.name);
            assertEquals(info.secret, loaded.secret);
            assertEquals(info.ownerUuid, loaded.ownerUuid);
            assertEquals(info.uuid, loaded.uuid);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(tmp != null) {
                tmp.delete();
            }
        }
    }
}
