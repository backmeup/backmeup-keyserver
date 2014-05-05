package org.backmeup.keysrv.worker;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.xml.bind.DatatypeConverter;

public class HashGenerator {
	private static final String ENCODING = "UTF-8";
	private static final String HASH_ALGO = "SHA-512";
	private static final int SALT_LENGTH = 16;

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
		byte[] localSalt = salt;

		if (localSalt == null) {
			SecureRandom sr = new SecureRandom();
			localSalt = new byte[SALT_LENGTH];
			sr.nextBytes(localSalt);
		}

		try {
			md = MessageDigest.getInstance(HASH_ALGO);
			if (salted) {
				byte[] valueBytes = value.getBytes(ENCODING);
				byte[] finaldata = new byte[localSalt.length + valueBytes.length];

				System.arraycopy(localSalt, 0, finaldata, 0, localSalt.length);
				System.arraycopy(valueBytes, 0, finaldata, localSalt.length,
						valueBytes.length);

				byte[] saltedHash = md.digest(finaldata);
				hash = new byte[localSalt.length + saltedHash.length];

				System.arraycopy(localSalt, 0, hash, 0, localSalt.length);
				System.arraycopy(saltedHash, 0, hash, localSalt.length,
						saltedHash.length);
			} else {
				hash = md.digest(value.getBytes(ENCODING));
			}
		} catch (Exception e) {
			FileLogger.logException("Error in sha512 calculation", e);
			return null;
		}

		StringBuilder sb = new StringBuilder();
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

		String checkHash = this.calcSHA512(value, true, salt);

		if (hash.compareTo(checkHash) == 0) {
			return true;
		}

		return false;
	}
}
