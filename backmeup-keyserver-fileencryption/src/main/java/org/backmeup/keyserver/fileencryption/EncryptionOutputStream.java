package org.backmeup.keyserver.fileencryption;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.security.InvalidKeyException;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;

import org.backmeup.keyserver.crypto.AsymmetricEncryptionProvider;
import org.backmeup.keyserver.crypto.SymmetricEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.AESEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.RSAEncryptionProvider;
import org.backmeup.keyserver.model.CryptoException;

public class EncryptionOutputStream extends FilterOutputStream {

    private SymmetricEncryptionProvider symmetricEncryption;
    private AsymmetricEncryptionProvider asymmetricEncryption;
    private byte[] fileKey;
    
    private Keystore keystore;
    private Writer keystoreWriter;
    
    public EncryptionOutputStream(String path, String ownerId, PublicKey ownerPublicKey) throws IOException {
        this(new File(path), ownerId, ownerPublicKey);
    }
    
    public EncryptionOutputStream(File file, String ownerId, PublicKey ownerPublicKey) throws IOException {
        super(new FileOutputStream(file));
        try {
            this.initEncryption();
            
            this.keystore = new FileKeystore(this.fileKey, asymmetricEncryption, new File(file.getAbsolutePath()+Configuration.KEYSTORE_SUFFIX));
            this.keystore.addReceiver(ownerId, ownerPublicKey);
            
            this.initCipherStream();
        } catch (CryptoException e) {
            throw new IOException(e);
        }
    }
    
    public EncryptionOutputStream(OutputStream out, Keystore keystore, String ownerId, PublicKey ownerPublicKey) throws IOException {
        this(out, keystore, null, ownerId, ownerPublicKey);
    }
    
    public EncryptionOutputStream(OutputStream out, Writer keystoreWriter, String ownerId, PublicKey ownerPublicKey) throws IOException {
        this(out, null, keystoreWriter, ownerId, ownerPublicKey);
    }
    
    private EncryptionOutputStream(OutputStream out, Keystore keystore, Writer keystoreWriter, String ownerId, PublicKey ownerPublicKey) throws IOException {
        super(out);
        try {
            this.initEncryption();
            
            if (keystore == null) {
                this.keystore = new Keystore(this.fileKey, asymmetricEncryption);
            } else {
                this.keystore = keystore;
                this.keystore.setSecretKey(this.fileKey);
            }
            this.keystore.addReceiver(ownerId, ownerPublicKey);
            this.keystoreWriter = keystoreWriter;
            
            this.initCipherStream();
        } catch (CryptoException e) {
            throw new IOException(e);
        }
    }
    
    private void initEncryption() throws CryptoException {
        this.symmetricEncryption = new AESEncryptionProvider(Configuration.AES_MODE);
        this.asymmetricEncryption = new RSAEncryptionProvider();
        this.fileKey = this.symmetricEncryption.generateKey(Configuration.AES_KEY_LENGTH);
    }
    
    private void initCipherStream() throws CryptoException, IOException {
        Cipher c = this.symmetricEncryption.getCipher();
        try {
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(this.fileKey, "AES"));
        } catch (InvalidKeyException e) {
            throw new CryptoException(e);
        }
        this.out = new CipherOutputStream(out, c);
        this.out.write(this.symmetricEncryption.getIV());
    }
        
    @Override
    public void close() throws IOException {
        super.close();
        this.saveKeystore();
    }
    
    public Keystore getKeystore() {
        return this.keystore;
    }
    
    public void saveKeystore() throws IOException {
        try {
            if(this.keystore instanceof FileKeystore) {
                ((FileKeystore) this.keystore).save();
            } else {
                this.keystore.save(this.keystoreWriter);
            }
        } catch (CryptoException e) {
            throw new IOException(e);
        }
    }
}
