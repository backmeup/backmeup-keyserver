package org.backmeup.keyserver.fileencryption;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.util.List;

import javax.crypto.BadPaddingException;

import org.backmeup.keyserver.crypto.AsymmetricEncryptionProvider;
import org.backmeup.keyserver.crypto.SymmetricEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.AESEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.RSAEncryptionProvider;
import org.backmeup.keyserver.model.CryptoException;
import org.junit.Test;

public class KeystoreTest {
    
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
    public void testAddReceiver() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
       
        KeyPair kp1 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        ks.addReceiver(USER1_ID, kp1.getPublic());
        assertEquals(1, ks.listReceivers().size());
        assertTrue(ks.hasReceiver(USER1_ID));
        
        KeyPair kp2 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        ks.addReceiver(USER2_ID, kp2.getPublic());
        assertEquals(2, ks.listReceivers().size());
        assertTrue(ks.hasReceiver(USER2_ID));
        
        List<String> ids = ks.listReceivers();
        ids.contains(USER1_ID);
        ids.contains(USER2_ID);
    }
    
    @Test
    public void testAddReceiverTwice() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        ks.addReceiver(USER1_ID, kp1.getPublic());
        assertTrue(ks.hasReceiver(USER1_ID));
        try {
            ks.addReceiver(USER1_ID, kp1.getPublic());
            fail();
        } catch(CryptoException e) {
            assertTrue(e.getMessage().startsWith("keystore entry already exists"));
        }
        assertTrue(ks.hasReceiver(USER1_ID));
        assertEquals(1, ks.listReceivers().size());
    }
    
    @Test
    public void testRemoveReceiver() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        ks.addReceiver(USER1_ID, kp1.getPublic());
        assertEquals(1, ks.listReceivers().size());
        assertTrue(ks.hasReceiver(USER1_ID));
        assertTrue(ks.removeReceiver(USER1_ID));
        assertEquals(0, ks.listReceivers().size());
        assertFalse(ks.hasReceiver(USER1_ID));
    }
    
    @Test
    public void testRemoveReceiverFail() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        ks.addReceiver(USER1_ID, kp1.getPublic());
        assertEquals(1, ks.listReceivers().size());
        assertTrue(ks.hasReceiver(USER1_ID));
        assertFalse(ks.removeReceiver(USER2_ID));
        assertEquals(1, ks.listReceivers().size());
        assertTrue(ks.hasReceiver(USER1_ID));
    }
    
    @Test
    public void testGetFileKey() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        ks.addReceiver(USER1_ID, kp1.getPublic());
        assertTrue(ks.hasReceiver(USER1_ID));
        
        KeyPair kp2 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        ks.addReceiver(USER2_ID, kp2.getPublic());
        assertTrue(ks.hasReceiver(USER2_ID));
        
        byte[] fileKey1 = ks.getSecretKey(USER1_ID, kp1.getPrivate());
        byte[] fileKey2 = ks.getSecretKey(USER2_ID, kp2.getPrivate());
        assertNotNull(fileKey1);
        assertNotNull(fileKey2);
        assertArrayEquals(fileKey1, fileKey2);
    }
    
    @Test
    public void testGetFileKeyFail() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        try {
            ks.getSecretKey(USER1_ID, kp1.getPrivate());
            fail();
        } catch(CryptoException e) {
            assertTrue(e.getMessage().startsWith("no keystore entry"));
        }
    }
    
    @Test
    public void testGetFileKeyDecodeFail() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        ks.addReceiver(USER1_ID, kp1.getPublic());
        assertTrue(ks.hasReceiver(USER1_ID));
        
        KeyPair kp2 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        try {
            ks.getSecretKey(USER1_ID, kp2.getPrivate());
            fail();
        } catch(CryptoException e) {
            assertTrue(e.getCause() instanceof BadPaddingException);
        }
    }
    
    @Test
    public void testGetFileKeyBySearch() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        ks.addReceiver(USER1_ID, kp1.getPublic());
        assertTrue(ks.hasReceiver(USER1_ID));
        
        KeyPair kp2 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        ks.addReceiver(USER2_ID, kp2.getPublic());
        assertTrue(ks.hasReceiver(USER2_ID));
        
        byte[] fileKey1 = ks.getSecretKey(kp1.getPrivate());
        byte[] fileKey2 = ks.getSecretKey(kp2.getPrivate());
        assertNotNull(fileKey1);
        assertNotNull(fileKey2);
        assertArrayEquals(fileKey1, fileKey2);
    }
    
    @Test
    public void testGetFileKeyBySearchFail() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        ks.addReceiver(USER1_ID, kp1.getPublic());
        assertTrue(ks.hasReceiver(USER1_ID));
        
        KeyPair kp2 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        assertFalse(ks.hasReceiver(USER2_ID));
        
        try {
            ks.getSecretKey(kp2.getPrivate());
            fail();
        } catch(CryptoException e) {
            assertTrue(e.getMessage().startsWith("no keystore entry matching given private key"));
        }
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
