package org.backmeup.keyserver.fileencryption;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;

import org.backmeup.keyserver.crypto.AsymmetricEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.RSAEncryptionProvider;
import org.backmeup.keyserver.model.CryptoException;
import org.junit.Test;

public class EncryptionStreamTest {

    private static final int RSA_KEY_LENGTH = 2048;
    private static final String USER1_ID = "user1";
    private static final String USER2_ID = "user2";
    private static final String TEST_MSG = "this is a secret test message";
    
    private static class Testfile {
        public File file;
        public EncryptionOutputStream out;
        public Keystore keystore;
        public KeyPair kp;
    }
    
    private Testfile writeTestfile() throws IOException, CryptoException {
        Testfile tf = new Testfile();
        
        AsymmetricEncryptionProvider asymmetricEncryption = new RSAEncryptionProvider();        
        tf.kp = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        
        tf.file = File.createTempFile("themis_", ".dat");
        tf.out = new EncryptionOutputStream(tf.file, USER1_ID, tf.kp.getPublic());
        tf.keystore = tf.out.getKeystore();
        tf.out.write(TEST_MSG.getBytes());
        tf.out.close();
        
        return tf;
    }
    
    private String readTestfile(File file, String userId, PrivateKey key) throws CryptoException, IOException {
        EncryptionInputStream ein = new EncryptionInputStream(file, userId, key);
        byte[] block = new byte[8];
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int read = 0;
        while ((read = ein.read(block)) != -1) {
            buffer.write(block, 0, read);
        }
        ein.close();
        
        return buffer.toString();
    }
    
    @Test
    public void testEncrypt() throws CryptoException, IOException {
        Testfile tf = writeTestfile();
        
        assertEquals(1, tf.keystore.listReceivers().size());
        assertTrue(tf.keystore.hasReceiver(USER1_ID));
        
        assertNotEquals(TEST_MSG, Files.readAllBytes(tf.file.toPath()));
    }
    
    @Test
    public void testDecrypt() throws CryptoException, IOException {
        Testfile tf = writeTestfile();
        
        String message = readTestfile(tf.file, USER1_ID, tf.kp.getPrivate());
        assertEquals(TEST_MSG, message);
    }
    
    @Test
    public void testDecryptFailUnauthorizedUser() throws CryptoException, IOException {
        Testfile tf = writeTestfile();
        KeyPair kp2 = new RSAEncryptionProvider().generateKey(RSA_KEY_LENGTH);
        
        try {
            readTestfile(tf.file, USER2_ID, kp2.getPrivate());
            fail();
        } catch(IOException e) {
            assertEquals("no keystore entry for " + USER2_ID, e.getCause().getMessage());
        }
    }
    
    @Test
    public void testDecryptFailWrongKey() throws CryptoException, IOException {
        Testfile tf = writeTestfile();
        KeyPair kp2 = new RSAEncryptionProvider().generateKey(RSA_KEY_LENGTH);
                
        try {
            readTestfile(tf.file, USER1_ID, kp2.getPrivate());
            fail();
        } catch(IOException e) {
            assertTrue(e.getCause() instanceof CryptoException);
        }
    }
    
    @Test
    public void testMultiUserDecrypt() throws CryptoException, IOException {
        Testfile tf = writeTestfile();
        KeyPair kp2 = new RSAEncryptionProvider().generateKey(RSA_KEY_LENGTH);
        tf.keystore.addReceiver(USER2_ID, kp2.getPublic());
        tf.out.saveKeystore();
        
        assertEquals(2, tf.keystore.listReceivers().size());
        assertTrue(tf.keystore.hasReceiver(USER1_ID));
        assertTrue(tf.keystore.hasReceiver(USER2_ID));
        
        String message = readTestfile(tf.file, USER1_ID, tf.kp.getPrivate());
        assertEquals(TEST_MSG, message);
        
        message = readTestfile(tf.file, USER2_ID, kp2.getPrivate());
        assertEquals(TEST_MSG, message);
    }
    
    @Test
    public void testUserAddAfterRead() throws CryptoException, IOException {
        Testfile tf = writeTestfile();
        KeyPair kp2 = new RSAEncryptionProvider().generateKey(RSA_KEY_LENGTH);
        
        String message = readTestfile(tf.file, USER1_ID, tf.kp.getPrivate());
        assertEquals(TEST_MSG, message);
        
        try {
            readTestfile(tf.file, USER2_ID, kp2.getPrivate());
            fail();
        } catch(IOException e) {
            assertEquals("no keystore entry for " + USER2_ID, e.getCause().getMessage());
        }
        
        EncryptionInputStream ein = new EncryptionInputStream(tf.file, USER1_ID, tf.kp.getPrivate());
        //...
        ein.close();
        Keystore keystore = ein.getKeystore();
        keystore.addReceiver(USER2_ID, kp2.getPublic());
        ein.saveKeystore();
        
        assertEquals(2, keystore.listReceivers().size());
        assertTrue(keystore.hasReceiver(USER1_ID));
        assertTrue(keystore.hasReceiver(USER2_ID));
                
        message = readTestfile(tf.file, USER2_ID, kp2.getPrivate());
        assertEquals(TEST_MSG, message);
    }
    
    @Test
    public void testUserRemoveAfterRead() throws CryptoException, IOException {
        Testfile tf = writeTestfile();
        KeyPair kp2 = new RSAEncryptionProvider().generateKey(RSA_KEY_LENGTH);
        tf.keystore.addReceiver(USER2_ID, kp2.getPublic());
        tf.out.saveKeystore();
        
        assertEquals(2, tf.keystore.listReceivers().size());
        assertTrue(tf.keystore.hasReceiver(USER1_ID));
        assertTrue(tf.keystore.hasReceiver(USER2_ID));
        
        String message = readTestfile(tf.file, USER1_ID, tf.kp.getPrivate());
        assertEquals(TEST_MSG, message);
        
        message = readTestfile(tf.file, USER2_ID, kp2.getPrivate());
        assertEquals(TEST_MSG, message);
        
        EncryptionInputStream ein = new EncryptionInputStream(tf.file, USER1_ID, tf.kp.getPrivate());
        //...
        ein.close();
        Keystore keystore = ein.getKeystore();
        keystore.removeReceiver(USER2_ID);
        ein.saveKeystore();
        
        assertEquals(1, keystore.listReceivers().size());
        assertTrue(keystore.hasReceiver(USER1_ID));
        assertFalse(keystore.hasReceiver(USER2_ID));
        
        try {
            readTestfile(tf.file, USER2_ID, kp2.getPrivate());
            fail();
        } catch(IOException e) {
            assertEquals("no keystore entry for " + USER2_ID, e.getCause().getMessage());
        }
    }
    
    @Test
    public void testUserRemoveWithoutRead() throws CryptoException, IOException {
        Testfile tf = writeTestfile();
        KeyPair kp2 = new RSAEncryptionProvider().generateKey(RSA_KEY_LENGTH);
        tf.keystore.addReceiver(USER2_ID, kp2.getPublic());
        tf.out.saveKeystore();
        
        assertEquals(2, tf.keystore.listReceivers().size());
        assertTrue(tf.keystore.hasReceiver(USER1_ID));
        assertTrue(tf.keystore.hasReceiver(USER2_ID));
        
        String message = readTestfile(tf.file, USER1_ID, tf.kp.getPrivate());
        assertEquals(TEST_MSG, message);
        
        message = readTestfile(tf.file, USER2_ID, kp2.getPrivate());
        assertEquals(TEST_MSG, message);
        
        Keystore ks = EncryptionInputStream.getKeystore(tf.file);
        //load secret key into keystore
        ks.getSecretKey(USER2_ID, kp2.getPrivate());
        ks.removeReceiver(USER2_ID);
        ((FileKeystore) ks).save();
        
        assertEquals(1, ks.listReceivers().size());
        assertTrue(ks.hasReceiver(USER1_ID));
        assertFalse(ks.hasReceiver(USER2_ID));
        
        message = readTestfile(tf.file, USER1_ID, tf.kp.getPrivate());
        assertEquals(TEST_MSG, message);
        
        try {
            readTestfile(tf.file, USER2_ID, kp2.getPrivate());
            fail();
        } catch(IOException e) {
            assertEquals("no keystore entry for " + USER2_ID, e.getCause().getMessage());
        }
    }

    @Test
    public void testHasReceiverWithoutRead() throws CryptoException, IOException {
        Testfile tf = writeTestfile();
        
        assertEquals(1, tf.keystore.listReceivers().size());
        assertTrue(tf.keystore.hasReceiver(USER1_ID));
        assertFalse(tf.keystore.hasReceiver(USER2_ID));
        
        Keystore ks = EncryptionInputStream.getKeystore(tf.file);
        assertEquals(1, ks.listReceivers().size());
        assertTrue(ks.hasReceiver(USER1_ID));
        assertFalse(ks.hasReceiver(USER2_ID));
    }
}
