package org.backmeup.keyserver.fileencryption;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;

import org.backmeup.keyserver.crypto.AsymmetricEncryptionProvider;
import org.backmeup.keyserver.crypto.SymmetricEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.AESEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.RSAEncryptionProvider;
import org.backmeup.keyserver.model.CryptoException;
import org.junit.Test;

public class KeystoreTest {

    public Keystore prepareKeystore() throws CryptoException, IOException {
        SymmetricEncryptionProvider symmetricEncryption = new AESEncryptionProvider("AES/CBC/PKCS5Padding");
        AsymmetricEncryptionProvider asymmetricEncryption = new RSAEncryptionProvider();
        Keystore ks = new Keystore(symmetricEncryption, 256, asymmetricEncryption, File.createTempFile("themis_", ".keys"));
        assertEquals(0, ks.listReceivers().size());
        return ks;
    }
    
    @Test
    public void testAddReceiver() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
       
        KeyPair kp1 = asymmetricEncryption.generateKey(2048);
        ks.addReceiver("user1", kp1.getPublic());
        assertEquals(1, ks.listReceivers().size());
        assertTrue(ks.hasReceiver("user1"));
        
        KeyPair kp2 = asymmetricEncryption.generateKey(2048);
        ks.addReceiver("user2", kp2.getPublic());
        assertEquals(2, ks.listReceivers().size());
        assertTrue(ks.hasReceiver("user2"));
        
        List<String> ids = ks.listReceivers();
        ids.contains("user1");
        ids.contains("user2");
    }
    
    @Test
    public void testAddReceiverTwice() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(2048);
        ks.addReceiver("user1", kp1.getPublic());
        assertTrue(ks.hasReceiver("user1"));
        try {
            ks.addReceiver("user1", kp1.getPublic());
            fail();
        } catch(CryptoException e) {
            assertTrue(e.getMessage().startsWith("keystore entry already exists"));
        }
        assertTrue(ks.hasReceiver("user1"));
        assertEquals(1, ks.listReceivers().size());
    }
    
    @Test
    public void testRemoveReceiver() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(2048);
        ks.addReceiver("user1", kp1.getPublic());
        assertEquals(1, ks.listReceivers().size());
        assertTrue(ks.hasReceiver("user1"));
        assertTrue(ks.removeReceiver("user1"));
        assertEquals(0, ks.listReceivers().size());
        assertFalse(ks.hasReceiver("user1"));
    }
    
    @Test
    public void testRemoveReceiverFail() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(2048);
        ks.addReceiver("user1", kp1.getPublic());
        assertEquals(1, ks.listReceivers().size());
        assertTrue(ks.hasReceiver("user1"));
        assertFalse(ks.removeReceiver("user2"));
        assertEquals(1, ks.listReceivers().size());
        assertTrue(ks.hasReceiver("user1"));
    }
    
    @Test
    public void testGetFileKey() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(2048);
        ks.addReceiver("user1", kp1.getPublic());
        assertTrue(ks.hasReceiver("user1"));
        
        KeyPair kp2 = asymmetricEncryption.generateKey(2048);
        ks.addReceiver("user2", kp2.getPublic());
        assertTrue(ks.hasReceiver("user2"));
        
        byte[] fileKey1 = ks.getFileKey("user1", kp1.getPrivate());
        byte[] fileKey2 = ks.getFileKey("user2", kp2.getPrivate());
        assertNotNull(fileKey1);
        assertNotNull(fileKey2);
        assertArrayEquals(fileKey1, fileKey2);
    }
    
    @Test
    public void testGetFileKeyFail() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(2048);
        try {
            ks.getFileKey("user1", kp1.getPrivate());
            fail();
        } catch(CryptoException e) {
            assertTrue(e.getMessage().startsWith("no keystore entry"));
        }
    }
    
    @Test
    public void testGetFileKeyDecodeFail() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(2048);
        ks.addReceiver("user1", kp1.getPublic());
        assertTrue(ks.hasReceiver("user1"));
        
        KeyPair kp2 = asymmetricEncryption.generateKey(2048);
        try {
            ks.getFileKey("user1", kp2.getPrivate());
            fail();
        } catch(CryptoException e) {
            assertTrue(e.getCause() instanceof BadPaddingException);
        }
    }
    
    @Test
    public void testGetFileKeyBySearch() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(2048);
        ks.addReceiver("user1", kp1.getPublic());
        assertTrue(ks.hasReceiver("user1"));
        
        KeyPair kp2 = asymmetricEncryption.generateKey(2048);
        ks.addReceiver("user2", kp2.getPublic());
        assertTrue(ks.hasReceiver("user2"));
        
        byte[] fileKey1 = ks.getFileKey(kp1.getPrivate());
        byte[] fileKey2 = ks.getFileKey(kp2.getPrivate());
        assertNotNull(fileKey1);
        assertNotNull(fileKey2);
        assertArrayEquals(fileKey1, fileKey2);
    }
    
    @Test
    public void testGetFileKeyBySearchFail() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(2048);
        ks.addReceiver("user1", kp1.getPublic());
        assertTrue(ks.hasReceiver("user1"));
        
        KeyPair kp2 = asymmetricEncryption.generateKey(2048);
        assertFalse(ks.hasReceiver("user2"));
        
        try {
            ks.getFileKey(kp2.getPrivate());
            fail();
        } catch(CryptoException e) {
            assertTrue(e.getMessage().startsWith("no keystore entry matching given private key"));
        }
    }
    
    @Test
    public void testSaveAndLoadKeystore() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        
        KeyPair kp1 = asymmetricEncryption.generateKey(2048);
        ks.addReceiver("user1", kp1.getPublic());
        byte[] fileKey1 = ks.getFileKey("user1", kp1.getPrivate());
        
        KeyPair kp2 = asymmetricEncryption.generateKey(2048);
        ks.addReceiver("user2", kp2.getPublic());
        assertTrue(ks.hasReceiver("user2"));
        
        ks.save();
        File keystore = ks.getKeystore();
        
        ks = new Keystore(ks.getSymmetricEncryption(), 256, ks.getAsymmetricEncryption(), keystore);
        assertEquals(0, ks.listReceivers().size());
        ks.load();     
        assertEquals(2, ks.listReceivers().size());
        ks.hasReceiver("user1");
        ks.hasReceiver("user2");
        
        byte[] fileKey2 = ks.getFileKey("user1", kp1.getPrivate());
        assertArrayEquals(fileKey1, fileKey2);
    }
}
