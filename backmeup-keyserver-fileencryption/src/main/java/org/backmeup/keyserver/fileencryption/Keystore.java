package org.backmeup.keyserver.fileencryption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.backmeup.keyserver.crypto.AsymmetricEncryptionProvider;
import org.backmeup.keyserver.model.CryptoException;
import org.backmeup.keyserver.model.KeyserverUtils;

public class Keystore {
    
    protected static final String EXC_SECRET_KEY_NOT_LOADED = "secret key has to be set or loaded first";

    protected class Entry {
        private static final String SEPARATOR = ";";
        
        private String id;
        private String encodedSecretKey;

        protected Entry() {
        }
        
        protected Entry(String id) {
            this.id = id;
        }
        
        protected String getId() {
            return this.id;
        }

        protected byte[] getSecretKey(PrivateKey privateKey) throws CryptoException {
            byte[] encryptedSecretKey = KeyserverUtils.fromBase64String(this.encodedSecretKey);
            return asymmetricEncryption.decrypt(privateKey, encryptedSecretKey);
        }

        protected void setSecretKey(PublicKey publicKey, byte[] secretKey) throws CryptoException {
            byte[] encryptedSecretKey = asymmetricEncryption.encrypt(publicKey, secretKey);
            this.encodedSecretKey = KeyserverUtils.toBase64String(encryptedSecretKey);
        }
        
        protected void load(String encodedEntry) throws CryptoException {
            String[] parts = encodedEntry.split(SEPARATOR);
            if (parts.length != 2) {
                throw new CryptoException("cannot parse keystore entry: " + encodedEntry);
            }
            
            this.id = parts[0];
            this.encodedSecretKey = parts[1];
        }
        
        protected String save() throws CryptoException {
            return id + SEPARATOR + encodedSecretKey;
        }
        
        @Override
        public int hashCode() {
            return this.id.hashCode();
        }
        
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Entry)) {
                return false;
            }
            
            if (other == this) {
                return true;
            }
            
            return this.id.equals(((Entry) other).id); 
        }
    }
    
    private AsymmetricEncryptionProvider asymmetricEncryption;
    private Map<String, Entry> receivers;
    private byte[] secretKey;
    
    public Keystore(AsymmetricEncryptionProvider asymmetricEncryption) {
        this.asymmetricEncryption = asymmetricEncryption;
        this.receivers = new HashMap<>();
    }
    
    public Keystore(byte[] secretKey, AsymmetricEncryptionProvider asymmetricEncryption) {
        this(asymmetricEncryption);
        this.secretKey = secretKey.clone();
    }
    
    public AsymmetricEncryptionProvider getAsymmetricEncryption() {
        return asymmetricEncryption;
    }
    
    public void setSecretKey(byte[] secretKey) {
        if (!this.receivers.isEmpty()) {
            throw new IllegalStateException("cannot set secret key if receivers already exist");
        }
        this.secretKey = secretKey.clone();
    }
    
    public byte[] getSecretKey(String id, PrivateKey privateKey) throws CryptoException {
        Entry e = this.receivers.get(id);
        if (e == null) {
            throw new CryptoException("no keystore entry for " + id);
        }
        
        this.secretKey = e.getSecretKey(privateKey);
        return this.secretKey.clone();
    }
    
    public byte[] getSecretKey(PrivateKey privateKey) throws CryptoException {
        for (Entry e : this.receivers.values()) {
            try {
                this.secretKey = e.getSecretKey(privateKey);
                return this.secretKey.clone();
            } catch(CryptoException ex) {
                continue;
            }
        }
        
        throw new CryptoException("no keystore entry matching given private key");
    }

    public void addReceiver(String id, PublicKey publicKey) throws CryptoException {
        if (this.secretKey == null) {
            throw new IllegalStateException(EXC_SECRET_KEY_NOT_LOADED);
        }
        if (this.receivers.containsKey(id)) {
            throw new CryptoException("keystore entry already exists for " + id);
        }
        
        Entry e = new Entry(id);
        e.setSecretKey(publicKey, secretKey);
        this.receivers.put(id, e);
    }
    
    public boolean removeReceiver(String id) {
        if (this.secretKey == null) {
            throw new IllegalStateException(EXC_SECRET_KEY_NOT_LOADED);
        }
        
        return this.receivers.remove(id) != null;
    }
    
    public List<String> listReceivers() {
        return new ArrayList<String>(this.receivers.keySet());
    }
    
    public boolean hasReceiver(String id) {
        return this.receivers.containsKey(id);
    }
    
    public void save(Writer out) throws IOException, CryptoException { //NOSONAR
        if (this.secretKey == null) {
            throw new IllegalStateException(EXC_SECRET_KEY_NOT_LOADED);
        }
        
        try (BufferedWriter bout = new BufferedWriter(out)) {
            for (Entry e : this.receivers.values()) {
                bout.write(e.save());
                bout.newLine();
            }
            bout.flush();
        }
    }
    
    public void load(Reader in) throws IOException, CryptoException { //NOSONAR
        this.receivers = new HashMap<String, Entry>();
        try (BufferedReader bin = new BufferedReader(in)) {
            String line = null;
            while ((line = bin.readLine()) != null) {
                Entry e = new Entry();
                e.load(line);
                this.receivers.put(e.id, e);
            }
        }
    }
}
