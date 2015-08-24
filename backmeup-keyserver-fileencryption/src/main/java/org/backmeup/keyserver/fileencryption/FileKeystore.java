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
    private static final String DEFAULT_ENCODING = "UTF-8";
    private File keystore;
    
    public FileKeystore(AsymmetricEncryptionProvider asymmetricEncryption, File keystore) {
        super(asymmetricEncryption);
        this.keystore = keystore;
    }
    
    public FileKeystore(byte[] secretKey, AsymmetricEncryptionProvider asymmetricEncryption, File keystore) {
        super(secretKey, asymmetricEncryption);
        this.keystore = keystore;
    }
        
    public void save() throws IOException, CryptoException { //NOSONAR
        super.save(new OutputStreamWriter(new FileOutputStream(this.keystore), DEFAULT_ENCODING));
    }
    
    public void load() throws IOException, CryptoException { //NOSONAR
        super.load(new InputStreamReader(new FileInputStream(this.keystore), DEFAULT_ENCODING));
    }
    
    public File getKeystore() {
        return this.keystore;
    }
}
