package org.backmeup.keyserver.crypto.impl;

import static org.junit.Assert.*;

import org.apache.commons.codec.binary.StringUtils;
import org.backmeup.keyserver.crypto.SymmetricEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.AESEncryptionProvider;
import org.backmeup.keyserver.model.CryptoException;
import org.junit.Test;

public class AESEncryptionProviderTest {

    @Test
    public void testEncryptAndDecrypt() throws CryptoException {
        SymmetricEncryptionProvider ep = new AESEncryptionProvider("AES/CBC/PKCS5Padding");
        byte[] key = StringUtils.getBytesUtf8("mypass which should have 32bytes");
        byte[] encrypted = ep.encrypt(key, StringUtils.getBytesUtf8("mysecrettext"));
        String message = StringUtils.newStringUtf8(ep.decrypt(key, encrypted));
        assertEquals("mysecrettext", message);
    }
}