package org.backmeup.tests.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {
	private Properties properties;
	
	public Configuration() {
		properties = new Properties();
	}

	// Searches for the property with the specified key in the system 
	// properties. If it can not be found, the search continues in the 
	// specified property file. If the property does not exist, this
	// method returns null.
	public String getProperty(String key) {
		String value = System.getProperty(key);
		if(value == null) {
			value = properties.getProperty(key);
		}
		return value;
		
	}
	
	// Searches for the property with the specified key in the system 
	// properties. If it can not be found, the search continues in the 
	// specified property file. If the property does not exist, this
	// method returns the default value provided as second argument.
	public String getProperty(String key, String defaultValue) {
		String value = getProperty(key);
		if(value != null) {
			return value;
		} else {
			return defaultValue;
		}
	}
	
	// Reads a property list from the input byte stream.
	// Stream remains open after this method returns.
	public void load(InputStream inStream) throws IOException {
		properties.load(inStream);
	}
}
