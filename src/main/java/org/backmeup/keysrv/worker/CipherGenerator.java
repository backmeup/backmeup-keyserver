package org.backmeup.keysrv.worker;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.backmeup.keysrv.rest.exceptions.RestWrongDecryptionKeyException;
import org.jboss.resteasy.util.Base64;

public class CipherGenerator {
	public static final int TYPE_PWD = 1;
	public static final int TYPE_OAUTH = 2;

	private static final String ENCODING = "UTF-8";
	private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
	private static final String PWD_ALGORITHM = "PBKDF2WithHmacSHA1";
	private static final String KEY_SPEC = "AES";
	private static final String DECRYPTION_FAILED = "data decryption failed";
	private static final int NUM_PWD_ITERATIONS = 1038;
	private static final int AES_KEY_LENGTH = 256;
	private static final int SALT_LENGTH = 16;
	private static final int IV_LENGTH = 16;

	public CipherGenerator() {
	}

	public byte[] encData(String data, User user) {
		return this.encData(data, user.getPwd());
	}

	public String decData(byte[] encdata, User user) {
		try {
			return this.decData(encdata, user.getPwd());
		} catch (BadPaddingException e) {
			FileLogger.logException(DECRYPTION_FAILED, e);
			throw new RestWrongDecryptionKeyException(user.getBmuId());
		}
	}

	public Map<byte[], byte[]> encData(Map<String, String> data, User user) {
		Map<byte[], byte[]> encData = new HashMap<byte[], byte[]>();

		for (Map.Entry<String, String> element : data.entrySet()) {
			byte[] encKey = this.encData(element.getKey(), user.getPwd());
			byte[] encValue = this.encData(element.getValue(), user.getPwd());

			encData.put(encKey, encValue);
		}

		return encData;
	}

	public Map<String, String> decData(Map<byte[], byte[]> encData, User user) {
		Map<String, String> data = new HashMap<String, String>();

		for (Map.Entry<byte[], byte[]> element : encData.entrySet()) {
			String decKey = "";
			String decValue = "";

			try {
				decKey = this.decData(element.getKey(), user.getPwd());
				decValue = this.decData(element.getValue(), user.getPwd());
			} catch (BadPaddingException e) {
				FileLogger.logException(DECRYPTION_FAILED, e);
				throw new RestWrongDecryptionKeyException(user.getBmuId());
			}

			data.put(decKey, decValue);
		}

		return data;
	}

	/**
	 * Encrypts the data with the given user password.
	 * 
	 * @param data
	 * @param pwd
	 * @return
	 */
	public byte[] encData(String data, String pwd) {
		// Password Salt
		SecureRandom sr = new SecureRandom();
		byte[] salt = new byte[SALT_LENGTH];
		sr.nextBytes(salt);

		byte[] iv = null;
		byte[] encdata = null;

		PBEKeySpec keyspec = new PBEKeySpec(toCharArray(pwd), salt,
				NUM_PWD_ITERATIONS, AES_KEY_LENGTH);

		try {
			Cipher cipher = Cipher.getInstance(AES_ALGORITHM);

			SecretKeyFactory keyfactory = SecretKeyFactory
					.getInstance(PWD_ALGORITHM);

			PBEKey pbekey = (PBEKey) keyfactory.generateSecret(keyspec);
			SecretKey encKey = new SecretKeySpec(pbekey.getEncoded(), KEY_SPEC);

			cipher.init(Cipher.ENCRYPT_MODE, encKey);

			iv = cipher.getIV();

			// encrypt data
			encdata = cipher.doFinal(data.getBytes(ENCODING));
		} catch (Exception e) {
			// would never come up
			FileLogger.logException(
					"something went wrong in encrypt data function", e);
			return new byte[0];
		}

		// create bytearray for salt + iv + encrypted data
		byte[] finaldata = new byte[salt.length + iv.length + encdata.length];

		// copy salt + iv + encrypted data to final array
		System.arraycopy(salt, 0, finaldata, 0, salt.length);
		System.arraycopy(iv, 0, finaldata, salt.length, iv.length);
		System.arraycopy(encdata, 0, finaldata, salt.length + iv.length,
				encdata.length);

		return finaldata;
	}

	/**
	 * Decrypts the data with the given user password.
	 * 
	 * @param encdata
	 * @param pwd
	 * @return
	 * @throws BadPaddingException
	 */
	public String decData(byte[] encdata, String pwd)
			throws BadPaddingException {
		byte[] salt = new byte[SALT_LENGTH];
		byte[] iv = new byte[IV_LENGTH];
		byte[] data = new byte[encdata.length - salt.length - iv.length];
		byte[] decdata = null;
		String finaldata = null;

		System.arraycopy(encdata, 0, salt, 0, salt.length);
		System.arraycopy(encdata, salt.length, iv, 0, iv.length);
		System.arraycopy(encdata, salt.length + iv.length, data, 0, data.length);

		PBEKeySpec keyspec = new PBEKeySpec(toCharArray(pwd), salt,
				NUM_PWD_ITERATIONS, AES_KEY_LENGTH);

		try {
			Cipher cipher = Cipher.getInstance(AES_ALGORITHM);

			SecretKeyFactory keyfactory = SecretKeyFactory
					.getInstance(PWD_ALGORITHM);

			PBEKey pbekey = (PBEKey) keyfactory.generateSecret(keyspec);
			SecretKey encKey = new SecretKeySpec(pbekey.getEncoded(), KEY_SPEC);

			IvParameterSpec ips = new IvParameterSpec(iv);

			cipher.init(Cipher.DECRYPT_MODE, encKey, ips);

			// encrypt data
			decdata = cipher.doFinal(data);
			finaldata = new String(decdata, ENCODING);
		} catch (BadPaddingException badpadding) {
			// wrong key
			throw badpadding;
		} catch (Exception e) {
			// would never come up
			FileLogger.logException(
					"something went wrong in decryption data function", e);
		}

		return finaldata;
	}

	public String generatePassword() {
		SecureRandom sr = new SecureRandom();
		byte[] key = new byte[64];

		sr.nextBytes(key);

		return Base64.encodeBytes(key);
	}

	private boolean isBase64(String pwd) {
		// tests if the password is a 88 chars long base64 conform string
		return pwd.matches("^(([A-Za-z0-9+/]{4}){21})([A-Za-z0-9+/]{2}==)$");
	}

	private char[] toCharArray(String pwd) {
		if (!isBase64(pwd)) {
			return pwd.toCharArray();
		}

		byte[] bytes;
		try {
			bytes = Base64.decode(pwd);
		} catch (IOException e) {
			// Should not come up
			FileLogger.logException("base 64 decoding failed", e);
			return new char[0];
		}

		char[] chars = new char[bytes.length];

		for (int i = 0; i < bytes.length; i++) {
			chars[i] = (char) bytes[i];
		}

		return chars;
	}
}
