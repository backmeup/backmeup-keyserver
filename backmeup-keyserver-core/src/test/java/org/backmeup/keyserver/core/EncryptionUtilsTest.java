package org.backmeup.keyserver.core;

import static org.backmeup.keyserver.core.EncryptionUtils.concat;
import static org.backmeup.keyserver.core.EncryptionUtils.split;
import static org.backmeup.keyserver.model.KeyserverUtils.fromBase64String;
import static org.backmeup.keyserver.model.KeyserverUtils.toBase64String;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EncryptionUtilsTest {
    @Test
    public void testConcat2() {
        byte[] a = { 0, 1, 2 };
        byte[] b = { 3, 4, 5 };
        byte[] c = concat(a, b);
        assertArrayEquals(new byte[] { 0, 1, 2, 3, 4, 5 }, c);
    }

    @Test
    public void testSplit() {
        byte[] a = { 0, 1, 2, 3, 4, 5 };
        byte[][] s = split(a, 3);
        assertArrayEquals(new byte[] { 0, 1, 2 }, s[0]);
        assertArrayEquals(new byte[] { 3, 4, 5 }, s[1]);
    }

}
