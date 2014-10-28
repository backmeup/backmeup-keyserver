package org.backmeup.keyserver.core.crypto.impl;

import static org.junit.Assert.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.backmeup.keyserver.core.crypto.CryptoException;
import org.backmeup.keyserver.core.crypto.HashProvider;
import org.backmeup.keyserver.core.crypto.impl.MessageDigestHashProvider;
import org.junit.Test;

public class MessageDigestHashProviderTest {

    @Test
    public void testHash() throws CryptoException {
        String message = "mytest";
        String expected = "9o0n2hfZ/UlECwuNrBMPT5ii1Maso8kiHNvIsmSyyL4=";

        HashProvider hp = new MessageDigestHashProvider("SHA-256");
        String hash = StringUtils.newStringUtf8(Base64.encodeBase64(hp.hash(StringUtils.getBytesUtf8(message))));
        assertEquals(expected, hash);
    }

}
