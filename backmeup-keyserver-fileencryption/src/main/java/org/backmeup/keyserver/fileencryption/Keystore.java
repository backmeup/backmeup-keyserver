package org.backmeup.keyserver.fileencryption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.backmeup.keyserver.crypto.AsymmetricEncryptionProvider;
import org.backmeup.keyserver.crypto.SymmetricEncryptionProvider;
import org.backmeup.keyserver.model.CryptoException;
import org.backmeup.keyserver.model.KeyserverUtils;

public class Keystore {
    
    protected class Entry {
        private static final String SEPARATOR = ";";
        
        private String id;
        private String encodedFileKey;

        protected Entry() {
        }
        
        protected Entry(String id) {
            this.id = id;
        }
        
        protected String getId() {
            return this.id;
        }

        protected byte[] getFileKey(PrivateKey privateKey) throws CryptoException {
            if (privateKey == null) {
                throw new CryptoException("need private key to decode entry");
            }

            byte[] encryptedFileKey = KeyserverUtils.fromBase64String(this.encodedFileKey);
            return asymmetricEncryption.decrypt(privateKey, encryptedFileKey);
        }

        protected void setFileKey(PublicKey publicKey, byte[] fileKey) throws CryptoException {
            if (publicKey == null) {
                throw new CryptoException("need public key to encode entry");
            }
            
            byte[] encryptedFileKey = asymmetricEncryption.encrypt(publicKey, fileKey);
            this.encodedFileKey = KeyserverUtils.toBase64String(encryptedFileKey);
        }
        
        protected void load(String encodedEntry) throws CryptoException {
            String[] parts = encodedEntry.split(SEPARATOR);
            if (parts.length != 2) {
                throw new CryptoException("cannot parse keystore entry: " + encodedEntry);
            }
            
            this.id = parts[0];
            this.encodedFileKey = parts[1];
        }
        
        protected String save() throws CryptoException {
            return id + SEPARATOR + encodedFileKey;
        }
        
        @Override
        public int hashCode() {
            return this.id.hashCode();
        }
        
        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof Entry)) {
                return false;
            }
            
            if (other == this) {
                return true;
            }
            
            return this.id.equals(((Entry) other).id); 
        }
    }
    
    private SymmetricEncryptionProvider symmetricEncryption;
    private AsymmetricEncryptionProvider asymmetricEncryption;
    private byte[] fileKey;
    private Map<String, Entry> receivers;
    private File keystore;
    
    
    public Keystore(SymmetricEncryptionProvider symmetricEncryption, int keyLength, AsymmetricEncryptionProvider asymmetricEncryption, File keystore) throws CryptoException, IOException {
        this.symmetricEncryption = symmetricEncryption;
        this.asymmetricEncryption = asymmetricEncryption;
        
        this.fileKey = this.symmetricEncryption.generateKey(keyLength);
        
        this.keystore = keystore;
        this.receivers = new HashMap<>();
    }
    
    public void addReceiver(String id, PublicKey publicKey) throws CryptoException {
        if (this.receivers.containsKey(id)) {
            throw new CryptoException("keystore entry already exists for " + id);
        }
        Entry e = new Entry(id);
        e.setFileKey(publicKey, fileKey);
        this.receivers.put(id, e);
    }
    
    public List<String> listReceivers() {
        return new ArrayList<String>(this.receivers.keySet());
    }
    
    public boolean removeReceiver(String id) {
        return this.receivers.remove(id) != null;
    }
    
    public boolean hasReceiver(String id) {
        return this.receivers.containsKey(id);
    }
    
    public byte[] getFileKey(String id, PrivateKey privateKey) throws CryptoException {
        Entry e = this.receivers.get(id);
        if (e == null) {
            throw new CryptoException("no keystore entry for " + id);
        }
        
        return e.getFileKey(privateKey); 
    }
    
    public byte[] getFileKey(PrivateKey privateKey) throws CryptoException {
        for (Entry e : this.receivers.values()) {
            try {
                return e.getFileKey(privateKey);
            } catch(CryptoException ex) {
                continue;
            }
        }
        
        throw new CryptoException("no keystore entry matching given private key");
    }
    
    public void save() throws IOException, CryptoException {
        try (BufferedWriter bout = new BufferedWriter(new FileWriter(this.keystore))) {
            for (Entry e : this.receivers.values()) {
                bout.write(e.save());
                bout.newLine();
            }
            bout.flush();
        }
    }
    
    public void load() throws IOException, CryptoException {
        this.receivers = new HashMap<String, Entry>();
        try (BufferedReader bin = new BufferedReader(new FileReader(this.keystore))) {
            String line = null;
            while ((line = bin.readLine()) != null) {
                Entry e = new Entry();
                e.load(line);
                this.receivers.put(e.id, e);
            }
        }
    }

    public SymmetricEncryptionProvider getSymmetricEncryption() {
        return symmetricEncryption;
    }
    
    public AsymmetricEncryptionProvider getAsymmetricEncryption() {
        return asymmetricEncryption;
    }
    
    public File getKeystore() {
        return this.keystore;
    }
}
