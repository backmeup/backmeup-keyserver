package org.backmeup.keyserver.model;

import java.util.Calendar;
import java.util.TimeZone;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;


public class KeyserverUtils {
    private KeyserverUtils() {

    }

    public static String toBase64String(byte[] b) {
        return toBase64String(b, true);
    }
    
    public static String toBase64String(byte[] b, boolean urlSafe) {
        return StringUtils.newStringUtf8(Base64.encodeBase64(b, false, urlSafe));
    }

    public static byte[] fromBase64String(String s) {
        return Base64.decodeBase64(StringUtils.getBytesUtf8(s));
    }

    public static Calendar getActTime() {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }
    
    public static Calendar getActTimePlusMinuteOffset(int amount) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.add(Calendar.MINUTE, amount);
        return c;
    }
}
