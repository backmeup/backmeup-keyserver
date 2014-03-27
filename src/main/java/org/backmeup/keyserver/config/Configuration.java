package org.backmeup.keyserver.config;

import java.io.IOException;
import java.util.Properties;

public final class Configuration {
	private static final Properties properties = new Properties();
	
	public final static String PROPERTYFILE = "keyserver.properties";
	
	static {
		try {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			properties.load(loader.getResourceAsStream(PROPERTYFILE));
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static String getProperty(String key) {
		return properties.getProperty(key);	
	}
	
	public static String getProperty(String key, String defaultValue) {
		String value = getProperty(key);
		if(value != null) {
			return value;
		} else {
			return defaultValue;
		}
	}
}
