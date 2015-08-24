package org.backmeup.keyserver.fileencryption;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

    public Keystore prepareKeystore() throws CryptoException, IOException {
        SymmetricEncryptionProvider symmetricEncryption = new AESEncryptionProvider(Configuration.AES_MODE);
        AsymmetricEncryptionProvider asymmetricEncryption = new RSAEncryptionProvider();
        Keystore ks = new Keystore(symmetricEncryption.generateKey(Configuration.AES_KEY_LENGTH), asymmetricEncryption);
        assertEquals(0, ks.listReceivers().size());
        return ks;
    }
    
    @Test
    public void testEntry() throws CryptoException, IOException {
        Keystore ks = prepareKeystore();
        AsymmetricEncryptionProvider asymmetricEncryption = ks.getAsymmetricEncryption();
        KeyPair kp1 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        
        Keystore.Entry e1 = ks.new Entry(USER1_ID);
        assertEquals(USER1_ID, e1.getId());
        byte[] key = new byte[]{1,2,3,4};
        
        e1.setSecretKey(kp1.getPublic(), key);
        assertArrayEquals(key, e1.getSecretKey(kp1.getPrivate()));
        
        String encoded = e1.save();
        Keystore.Entry loaded = ks.new Entry();
        loaded.load(encoded);
        assertEquals(e1, loaded);
        assertArrayEquals(key, loaded.getSecretKey(kp1.getPrivate()));
        
        try {
            loaded = ks.new Entry();
            loaded.load("fail");
            fail();
        } catch(CryptoException e) {
            assertEquals("cannot parse keystore entry: fail", e.getMessage());
        }
        
        Keystore.Entry e2 = ks.new Entry(USER1_ID);
        e2.setSecretKey(kp1.getPublic(), new byte[]{5,6,7,8});
        Keystore.Entry e3 = ks.new Entry(USER2_ID);
        e3.setSecretKey(kp1.getPublic(), new byte[]{1,2,3,4});
        
        assertEquals(USER1_ID.hashCode(), e1.hashCode());
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotEquals(e1.hashCode(), e3.hashCode());
        assertEquals(e1, e1);
        assertEquals(e1, e2);
        assertNotEquals(e1, e3);
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
    public void testUnloadedKeystore() throws CryptoException, IOException {
        AsymmetricEncryptionProvider asymmetricEncryption = new RSAEncryptionProvider();
        Keystore ks = new Keystore(asymmetricEncryption);
        assertEquals(0, ks.listReceivers().size());
        
        KeyPair kp1 = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        
        try {
            ks.addReceiver(USER1_ID, kp1.getPublic());
            fail();
        } catch(IllegalStateException e) {
            assertEquals(Keystore.EXC_SECRET_KEY_NOT_LOADED, e.getMessage());
        }
        try {
            ks.removeReceiver(USER1_ID);
            fail();
        } catch(IllegalStateException e) {
            assertEquals(Keystore.EXC_SECRET_KEY_NOT_LOADED, e.getMessage());
        }
        try {
            ks.save(new OutputStreamWriter(new FileOutputStream(File.createTempFile("themis_", Configuration.KEYSTORE_SUFFIX)), "UTF-8"));
            fail();
        } catch(IllegalStateException e) {
            assertEquals(Keystore.EXC_SECRET_KEY_NOT_LOADED, e.getMessage());
        }
        
        SymmetricEncryptionProvider symmetricEncryption = new AESEncryptionProvider(Configuration.AES_MODE);
        ks.setSecretKey(symmetricEncryption.generateKey(Configuration.AES_KEY_LENGTH));
        ks.addReceiver(USER1_ID, kp1.getPublic());
        assertTrue(ks.hasReceiver(USER1_ID));
        
        try {
            ks.setSecretKey(symmetricEncryption.generateKey(Configuration.AES_KEY_LENGTH));
            fail();
        } catch(IllegalStateException e) {
            assertEquals("cannot set secret key if receivers already exist", e.getMessage());
        }
        
    }
}
