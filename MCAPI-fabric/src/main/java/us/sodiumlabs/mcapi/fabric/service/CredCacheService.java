package us.sodiumlabs.mcapi.fabric.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import us.sodiumlabs.mcapi.common.BaseUtils;
import us.sodiumlabs.mcapi.common.ByteBufferUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

public class CredCacheService {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String NAME = "name";
    public static final String SECRET = "secret";
    public static final String UUID = "uuid";
    public static final String OWNER = "owner";
    private static final Object synchronizer = new Object();

    private static final String RAND_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int ID_LENGTH = 8;

    private final Gson gson = new Gson();

    private final File file;

    private final Random random;

    private final Map<String, CredInformation> credInformationMap = new HashMap<>();

    public CredCacheService(final File file, final Random random) {
        this.file = requireNonNull(file);
        this.random = requireNonNull(random);
    }

    public void init() {
        synchronized (synchronizer) {
            if (!file.exists()) createEmptyCredFile(file);
            load();
        }
    }

    public void createEmptyCredFile(final File file) {
        try {
            if(!file.createNewFile()) throw new IOException("unable to create new file");

            try(final FileWriter writer = new FileWriter(file)) {
                writer.write("{}\n");
                writer.flush();
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to initialize file.", e);
        }
    }

    public Optional<CredInformation> getCredInformation(final String key) {
        synchronized (synchronizer) {
            return Optional.ofNullable(credInformationMap.get(key));
        }
    }

    public List<CredInformation> listUsers(final UUID ownerUuid) {
        synchronized (synchronizer) {
            return credInformationMap.values().stream()
                .filter(c -> Objects.equals(c.ownerUuid, ownerUuid))
                .collect(Collectors.toList());
        }
    }

    public CredInformation createCredInformation(final String name, final UUID ownerUuid) {
        final CredInformation credInformation = new CredInformation(
            "$MCAI" + generateIdString(),
            name,
            BaseUtils.toBase64(ByteBufferUtils.randomBytes(random, 32)),
            java.util.UUID.randomUUID(),
            ownerUuid);

        synchronized (synchronizer) {
            credInformationMap.put(credInformation.credId, credInformation);
            save();
        }

        return credInformation;
    }

    public void deleteCredInformation(final String id) {
        synchronized (synchronizer) {
            credInformationMap.remove(id);
            save();
        }
    }

    private String generateIdString() {
        return IntStream.generate(() -> random.nextInt(RAND_CHARS.length())).limit(ID_LENGTH)
            .collect(StringBuilder::new, (s, i) -> s.append(RAND_CHARS.charAt(i)), (s1, s2) -> s1.append(s2.toString()))
            .toString();
    }

    private void load() {
        try(final FileReader reader = new FileReader(file)) {
            final JsonParser parser = new JsonParser();
            final JsonElement element = parser.parse(reader);
            final JsonObject mapObject = element.getAsJsonObject();
            mapObject.entrySet()
                .forEach(credEntry -> {
                    final JsonObject creds = credEntry.getValue().getAsJsonObject();
                    final CredInformation credInformation = new CredInformation(
                        credEntry.getKey(),
                        creds.get(NAME).getAsString(),
                        creds.get(SECRET).getAsString(),
                        java.util.UUID.fromString(creds.get(UUID).getAsString()),
                        java.util.UUID.fromString(creds.get(OWNER).getAsString()));
                    credInformationMap.put(credEntry.getKey(), credInformation);
                });
        } catch (IOException e) {
            LOGGER.warn("Exception while loading.", e);
        }
    }

    private void save() {
        try(final FileWriter writer = new FileWriter(file)) {
            final JsonObject object = new JsonObject();
            credInformationMap.forEach((key, credObject) -> {
                final JsonObject credJson = new JsonObject();

                credJson.addProperty(NAME, credObject.name);
                credJson.addProperty(SECRET, credObject.secret);
                credJson.addProperty(UUID, credObject.uuid.toString());
                credJson.addProperty(OWNER, credObject.ownerUuid.toString());

                object.add(credObject.credId, credJson);
            });
            writer.write(gson.toJson(object));
        } catch (IOException e) {
            LOGGER.warn("Exception while saving.", e);
        }
    }
}
