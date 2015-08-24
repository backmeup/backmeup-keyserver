package org.backmeup.keyserver.fileencryption;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;

import org.backmeup.keyserver.crypto.AsymmetricEncryptionProvider;
import org.backmeup.keyserver.crypto.SymmetricEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.AESEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.RSAEncryptionProvider;
import org.backmeup.keyserver.model.CryptoException;
import org.junit.Test;

public class FileKeystoreTest {
    
    private static final int RSA_KEY_LENGTH = 2048;
    private static final String USER1_ID = "user1";
    private static final String USER2_ID = "user2";

    public FileKeystore prepareKeystore() throws CryptoException, IOException {
        SymmetricEncryptionProvider symmetricEncryption = new AESEncryptionProvider(Configuration.AES_MODE);
        AsymmetricEncryptionProvider asymmetricEncryption = new RSAEncryptionProvider();
        FileKeystore ks = new FileKeystore(symmetricEncryption.generateKey(Configuration.AES_KEY_LENGTH), asymmetricEncryption, File.createTempFile("themis_", Configuration.KEYSTORE_SUFFIX));
        assertEquals(0, ks.listReceivers().size());
        return ks;
    }

    @Test
    public void testSaveAndLoadKeystore() throws CryptoException, IOException {
        FileKeystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        ks.addReceiver(USER1_ID, kp1.getPublic());
        byte[] fileKey1 = ks.getSecretKey(USER1_ID, kp1.getPrivate());
        
        KeyPair kp2 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        ks.addReceiver(USER2_ID, kp2.getPublic());
        assertTrue(ks.hasReceiver(USER2_ID));
        
        ks.save();
        File keystore = ks.getKeystore();
        
        ks = new FileKeystore(ks.getAsymmetricEncryption(), keystore);
        assertEquals(0, ks.listReceivers().size());
        ks.load();     
        assertEquals(2, ks.listReceivers().size());
        ks.hasReceiver(USER1_ID);
        ks.hasReceiver(USER2_ID);
        
        byte[] fileKey2 = ks.getSecretKey(USER1_ID, kp1.getPrivate());
        assertArrayEquals(fileKey1, fileKey2);
    }
}
