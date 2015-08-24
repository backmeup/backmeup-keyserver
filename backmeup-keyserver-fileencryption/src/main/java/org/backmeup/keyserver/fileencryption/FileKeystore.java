package org.backmeup.keyserver.fileencryption;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

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
        super.save(new OutputStreamWriter(new FileOutputStream(this.keystore), "UTF-8"));
    }
    
    public void load() throws IOException, CryptoException {
        super.load(new InputStreamReader(new FileInputStream(this.keystore), "UTF-8"));
    }
    
    public File getKeystore() {
        return this.keystore;
    }
}
