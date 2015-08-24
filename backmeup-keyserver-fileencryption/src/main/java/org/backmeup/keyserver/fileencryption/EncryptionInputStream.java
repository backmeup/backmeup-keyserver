package org.backmeup.keyserver.fileencryption;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.backmeup.keyserver.crypto.AsymmetricEncryptionProvider;
import org.backmeup.keyserver.crypto.SymmetricEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.AESEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.RSAEncryptionProvider;
import org.backmeup.keyserver.model.CryptoException;

public class EncryptionInputStream extends FilterInputStream {

    private SymmetricEncryptionProvider symmetricEncryption;
    private AsymmetricEncryptionProvider asymmetricEncryption;
    private byte[] fileKey;
    
    private Keystore keystore;
    
    public EncryptionInputStream(String path, String userId, PrivateKey userPrivateKey) throws CryptoException, IOException {
        this(new File(path), userId, userPrivateKey);
    }
    
    public EncryptionInputStream(File file, String userId, PrivateKey userPrivateKey) throws CryptoException, IOException {
        super(new FileInputStream(file));
        this.initEncryption();
        
        this.keystore = new FileKeystore(asymmetricEncryption, new File(file.getAbsolutePath()+Configuration.KEYSTORE_SUFFIX));
        ((FileKeystore) this.keystore).load();
        this.fileKey = this.keystore.getSecretKey(userId, userPrivateKey);
        
        this.initCipherStream();
    }
    
    public EncryptionInputStream(InputStream in, Keystore keystore, String userId, PrivateKey userPrivateKey) throws CryptoException, IOException {
        this(in, keystore, null, userId, userPrivateKey);
    }
    
    public EncryptionInputStream(InputStream in, Reader keystoreReader, String userId, PrivateKey userPrivateKey) throws CryptoException, IOException {
        this(in, null, keystoreReader, userId, userPrivateKey);
    }
    
    private EncryptionInputStream(InputStream in, Keystore keystore, Reader keystoreReader, String userId, PrivateKey userPrivateKey) throws CryptoException, IOException {
        super(in);
        this.initEncryption();
        
        if (keystore == null) {
            this.keystore = new Keystore(asymmetricEncryption);
            this.keystore.load(keystoreReader);
        } else {
            this.keystore = keystore;
        }
        this.fileKey = this.keystore.getSecretKey(userId, userPrivateKey);
        
        this.initCipherStream();
    }
    
    private void initEncryption() throws CryptoException {
        this.symmetricEncryption = new AESEncryptionProvider(Configuration.AES_MODE);
        this.asymmetricEncryption = new RSAEncryptionProvider();
    }
    
    private void initCipherStream() throws CryptoException, IOException {
        byte[] iv = new byte[AESEncryptionProvider.IV_LENGTH];
        if (this.in.read(iv) != AESEncryptionProvider.IV_LENGTH) {
            throw new CryptoException("cannot read AES IV");
        }
        
        Cipher c = this.symmetricEncryption.getCipher();
        try {
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(this.fileKey, "AES"), new IvParameterSpec(iv));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new CryptoException(e);
        }
        this.in = new CipherInputStream(this.in, c);
    }
        
    public Keystore getKeystore() {
        return this.keystore;
    }
    
    public void saveKeystore() throws IOException {
        try {
            if(this.keystore instanceof FileKeystore) {
                ((FileKeystore) this.keystore).save();
            } else {
                throw new IllegalStateException("only callable if keystore is a FileKeystore");
            }
        } catch (CryptoException e) {
            throw new IOException(e);
        }
    }
    
    public void saveKeystore(Writer keystoreWriter) throws IOException {
        try {
            this.keystore.save(keystoreWriter);
        } catch (CryptoException e) {
            throw new IOException(e);
        }
    }
    
    public void addReceiver(String id, PublicKey publicKey) throws CryptoException {
        this.keystore.addReceiver(id, publicKey);
    }
    
    public List<String> listReceivers() {
        return this.keystore.listReceivers();
    }
    
    public boolean removeReceiver(String id) {
        return this.keystore.removeReceiver(id);
    }
    
    public boolean hasReceiver(String id) {
        return this.keystore.hasReceiver(id);
    }
}
