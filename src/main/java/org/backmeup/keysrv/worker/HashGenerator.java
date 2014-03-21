package org.backmeup.keysrv.worker;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.xml.bind.DatatypeConverter;

public class HashGenerator {
	private final String ENCODING = "UTF-8";
	private final String HASH_ALGO = "SHA-512";
	private final int SALT_LENGTH = 16;

	public HashGenerator() {
	}

	/**
	 * Returns an sha512 hash (hex formated) of an given string value (UTF-8)
	 * 
	 * @param value
	 * @return
	 */
	private String calcSHA512(String value, boolean salted, byte[] salt) {
		MessageDigest md = null;
		byte[] hash = null;

		if (salt == null) {
			SecureRandom sr = new SecureRandom();
			salt = new byte[SALT_LENGTH];
			sr.nextBytes(salt);
		}

		try {
			md = MessageDigest.getInstance(HASH_ALGO);
			if (salted == true) {
				byte[] value_bytes = value.getBytes(ENCODING);
				byte[] finaldata = new byte[salt.length + value_bytes.length];

				System.arraycopy(salt, 0, finaldata, 0, salt.length);
				System.arraycopy(value_bytes, 0, finaldata, salt.length,
						value_bytes.length);

				byte[] salted_hash = md.digest(finaldata);
				hash = new byte[salt.length + salted_hash.length];

				System.arraycopy(salt, 0, hash, 0, salt.length);
				System.arraycopy(salted_hash, 0, hash, salt.length,
						salted_hash.length);
			} else {
				hash = md.digest(value.getBytes(ENCODING));
			}
		} catch (Exception e) {
			FileLogger.logException(e);
		}

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < hash.length; i++) {
			sb.append(Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(
					1));
		}

		return sb.toString();
	}

	public String calcSaltedSHA512(String value) {
		return this.calcSHA512(value, true, null);
	}

	public String calcSHA512(String value) {
		return this.calcSHA512(value, false, null);
	}

	/**
	 * The function generates a salted hash out of the given value and compares
	 * it to the given hash. If the hash values are identical the function
	 * returns true.
	 * 
	 * @param value
	 * @param hash
	 * @return
	 */
	public boolean isCorrectValue(String value, String hash) {
		byte[] salt = null;

		salt = DatatypeConverter.parseHexBinary(hash.substring(0,
				SALT_LENGTH * 2));

		String check_hash = this.calcSHA512(value, true, salt);

		if (hash.compareTo(check_hash) == 0) {
			return true;
		}

		return false;
	}
}
