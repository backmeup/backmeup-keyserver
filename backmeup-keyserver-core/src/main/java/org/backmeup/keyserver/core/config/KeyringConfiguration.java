package org.backmeup.keyserver.core.config;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.backmeup.keyserver.crypto.Keyring;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class KeyringConfiguration {
    
    private static final String KEYRINGFILE = "backmeup-keyserver.keyrings";
    private static List<Keyring> KEYRINGS = null;

    private KeyringConfiguration() {
    }
    
    static {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader.getResourceAsStream(KEYRINGFILE) != null) {
                ObjectMapper mapper = new ObjectMapper();
                List<Keyring> keyrings = mapper.readValue(loader.getResourceAsStream(KEYRINGFILE), new TypeReference<List<Keyring>>() {                    
                });
                KEYRINGS = Collections.unmodifiableList(keyrings);
            } else {
                throw new IOException("unable to load properties file: " + KEYRINGFILE);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    public static List<Keyring> getKeyrings() {
        return KEYRINGS;
    }
}
