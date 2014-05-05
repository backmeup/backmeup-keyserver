package org.backmeup.keysrv.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.backmeup.keyserver.config.Configuration;

public class PGPKeys {
	private static final String PUBLIC_KEY_FILE = Configuration
			.getProperty("keyserver.publickey");
	private static final String PRIVATE_KEY_FILE = Configuration
			.getProperty("keyserver.privatekey");
	// 1 MiB
	private static final int MAX_FILE_SIZE = 1048576;

	private String publickey = "";
	private String privatekey = "";

	public PGPKeys() throws IOException {
		publickey = this.readPGPFile(PUBLIC_KEY_FILE);
		privatekey = this.readPGPFile(PRIVATE_KEY_FILE);
	}

	public String getPrivatekey() {
		return privatekey;
	}

	public String getPublickey() {
		return publickey;
	}

	/**
	 * Reads an pgp Keyfile and returns its content as String.
	 * 
	 * @param pgpfilename
	 * @return
	 * @throws IOException
	 */
	private String readPGPFile(String pgpfilename) throws IOException {
		File pgpfile = new File(pgpfilename);

		if (!pgpfile.exists()) {
			throw new FileNotFoundException("File not found: " + pgpfilename);
		}

		if (!pgpfile.canRead()) {
			throw new IOException("File not readable: " + pgpfilename);
		}

		if ((pgpfile.length() > MAX_FILE_SIZE)
				|| (pgpfile.length() > Integer.MAX_VALUE)) {
			throw new IOException("File \"" + pgpfilename
					+ "\" is bigger than max allowed file size ("
					+ MAX_FILE_SIZE
					+ " bytes), or bigger than Integer.MAX_VALUE");
		}

		// length is checked -> cast to int should be ok
		byte[] data = new byte[(int) pgpfile.length()];

		FileInputStream fis = new FileInputStream(pgpfile);
		try {
			fis.read(data);
		} catch (IOException e) {
			throw e;
		} finally {
			fis.close();
		}

		return new String(data, "UTF-8");
	}
}
