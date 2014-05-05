package org.backmeup.keyserver.config;

import java.io.IOException;
import java.util.Properties;

public final class Configuration {
	private static final Properties PROPERTIES = new Properties();

	public static final String PROPERTYFILE = "keyserver.properties";

	private Configuration () {
	}
	
	static {
		try {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			PROPERTIES.load(loader.getResourceAsStream(PROPERTYFILE));
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static String getProperty(String key) {
		return PROPERTIES.getProperty(key);
	}

	public static String getProperty(String key, String defaultValue) {
		String value = getProperty(key);
		if (value != null) {
			return value;
		} else {
			return defaultValue;
		}
	}
}
