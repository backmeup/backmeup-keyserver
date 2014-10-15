package org.backmeup.keyserver.core;

import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

public class KeyserverUtils {
	public static String toBase64String(byte[] b) {
		return StringUtils.newStringUtf8(Base64.encodeBase64(b));
	}
	
	public static byte[] fromBase64String(String s) {
		return Base64.decodeBase64(StringUtils.getBytesUtf8(s));
	}
	
	public static byte[] concat(byte[] a, byte[] b) {
			byte[] result = new byte[a.length + b.length];
			System.arraycopy(a, 0, result, 0, a.length);
			System.arraycopy(b, 0, result, a.length, b.length);
			return result;
	}
	
	public static byte[] concat(byte[] a, byte[] b, byte[] c) {
		byte[] result = new byte[a.length + b.length + c.length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		System.arraycopy(c, 0, result, a.length+b.length, c.length);
		return result;
	}
	
	public static byte[][] split(byte[] a, int offset) {
		byte[][] arrs = new byte[2][];
		arrs[0] = Arrays.copyOf(a, offset);
		arrs[1] = Arrays.copyOfRange(a, offset, a.length);
		return arrs;
	}
}
