package org.backmeup.keyserver.crypto.impl;

import static org.junit.Assert.*;

import org.apache.commons.codec.binary.StringUtils;
import org.backmeup.keyserver.crypto.HashProvider;
import org.backmeup.keyserver.crypto.impl.MessageDigestHashProvider;
import org.backmeup.keyserver.model.CryptoException;
import org.backmeup.keyserver.model.KeyserverUtils;
import org.junit.Test;

public class MessageDigestHashProviderTest {

    @Test
    public void testHash() throws CryptoException {
        String message = "mytest";
        String expected = "9o0n2hfZ_UlECwuNrBMPT5ii1Maso8kiHNvIsmSyyL4";

        HashProvider hp = new MessageDigestHashProvider("SHA-256");
        String hash = KeyserverUtils.toBase64String(hp.hash(StringUtils.getBytesUtf8(message)));
        assertEquals(expected, hash);
    }

}
