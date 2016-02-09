package net.laraifox.particlesandbox.core;

import java.util.HashMap;

public class Configuration {
	private static HashMap<EnumConfigKey, String> configMap = new HashMap<EnumConfigKey, String>();

	static {
		for (EnumConfigKey key : EnumConfigKey.values()) {
			configMap.put(key, key.getDefaultValue());
		}
	}

	private Configuration() {
	}

	private static String getValue(EnumConfigKey key) {
		if (configMap.get(key) == null) {
			throw new RuntimeException("Unknown configuration key requested! '" + key + "'");
		}

		return configMap.get(key);
	}

	public static void setValue(EnumConfigKey key, String value) {
		configMap.put(key, value);
	}

	public static boolean getBoolean(EnumConfigKey key) {
		return Boolean.valueOf(Configuration.getValue(key));
	}

	public static int getInteger(EnumConfigKey key) {
		return Integer.valueOf(Configuration.getValue(key));
	}

	public static float getFloat(EnumConfigKey key) {
		return Float.valueOf(Configuration.getValue(key));
	}

	public static String getString(EnumConfigKey key) {
		return String.valueOf(Configuration.getValue(key));
	}
}
