package org.backmeup.keyserver.core.crypto.impl;

import static org.junit.Assert.*;

import java.security.KeyPair;

import javax.crypto.BadPaddingException;

import org.apache.commons.codec.binary.StringUtils;
import org.backmeup.keyserver.core.crypto.AsymmetricEncryptionProvider;
import org.backmeup.keyserver.model.CryptoException;
import org.junit.Test;

public class RSAEncryptionProviderTest {

    @Test
    public void testEncryptAndDecrypt() throws CryptoException {
        AsymmetricEncryptionProvider ep = new RSAEncryptionProvider();
        KeyPair kp = ep.generateKey(2048);
        
        byte[] encrypted = ep.encrypt(kp.getPublic(), StringUtils.getBytesUtf8("mysecrettext"));
        String message = StringUtils.newStringUtf8(ep.decrypt(kp.getPrivate(), encrypted));
        assertEquals("mysecrettext", message);
    }
    
    @Test
    public void testEncryptAndDecryptFail() throws CryptoException {
        AsymmetricEncryptionProvider ep = new RSAEncryptionProvider();
        KeyPair kp = ep.generateKey(2048);
        KeyPair kp2 = ep.generateKey(2048);
        
        byte[] encrypted = ep.encrypt(kp.getPublic(), StringUtils.getBytesUtf8("mysecrettext"));
        
        try {
            StringUtils.newStringUtf8(ep.decrypt(kp2.getPrivate(), encrypted));
            fail();
        } catch(CryptoException e) {
            assertTrue(e.getCause() instanceof BadPaddingException);
        }
    }
}