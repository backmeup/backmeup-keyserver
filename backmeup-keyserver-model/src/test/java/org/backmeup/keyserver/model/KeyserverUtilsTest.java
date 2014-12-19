package org.backmeup.keyserver.model;

import static org.backmeup.keyserver.model.KeyserverUtils.fromBase64String;
import static org.backmeup.keyserver.model.KeyserverUtils.toBase64String;
import static org.junit.Assert.*;

import org.junit.Test;

public class KeyserverUtilsTest {

    @Test
    public void testToBase64String() {
        String b64 = toBase64String(new byte[] { 0, 1, 2 });
        assertEquals("AAEC", b64);
    }

    @Test
    public void testFromBase64String() {
        byte[] data = fromBase64String("AAEC");
        assertArrayEquals(new byte[] { 0, 1, 2 }, data);
    }
    
    @Test
    public void testToBase64StringStandard() {
        byte[] bytes = new byte[] {-10, -115, 39, -38, 23, -39, -3, 73, 68, 11, 15, -82, 54, -80, 76, 61, 62, 98, -117, 83, 26, -78, -113, 36, -120, 115, 111, 34, -55, -110, -53, 34, -8, 2};
        String b64 = toBase64String(bytes, false);
        assertEquals("9o0n2hfZ/UlECw+uNrBMPT5ii1Maso8kiHNvIsmSyyL4Ag==", b64);
    }
    
    @Test
    public void testToBase64StringURLSafe() {
        byte[] bytes = new byte[] {-10, -115, 39, -38, 23, -39, -3, 73, 68, 11, 15, -82, 54, -80, 76, 61, 62, 98, -117, 83, 26, -78, -113, 36, -120, 115, 111, 34, -55, -110, -53, 34, -8, 2};
        String b64 = toBase64String(bytes, true);
        assertEquals("9o0n2hfZ_UlECw-uNrBMPT5ii1Maso8kiHNvIsmSyyL4Ag", b64);
    }
}
