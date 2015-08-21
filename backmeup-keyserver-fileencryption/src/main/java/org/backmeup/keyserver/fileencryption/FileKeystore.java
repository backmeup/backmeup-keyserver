package org.backmeup.keyserver.fileencryption;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.backmeup.keyserver.crypto.AsymmetricEncryptionProvider;
import org.backmeup.keyserver.model.CryptoException;

public class FileKeystore extends Keystore {
    private File keystore;
    
    public FileKeystore(AsymmetricEncryptionProvider asymmetricEncryption, File keystore) throws CryptoException, IOException {
        super(asymmetricEncryption);
        this.keystore = keystore;
    }
    
    public FileKeystore(byte[] secretKey, AsymmetricEncryptionProvider asymmetricEncryption, File keystore) throws CryptoException, IOException {
        super(secretKey, asymmetricEncryption);
        this.keystore = keystore;
    }
        
    public void save() throws IOException, CryptoException {
        super.save(new FileWriter(this.keystore));
    }
    
    public void load() throws IOException, CryptoException {
        super.load(new FileReader(this.keystore));
    }
    
    public File getKeystore() {
        return this.keystore;
    }
}
