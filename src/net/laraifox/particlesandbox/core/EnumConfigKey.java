package net.laraifox.particlesandbox.core;

public enum EnumConfigKey {
	DISPLAY_WIDTH("1280"),
	DISPLAY_HEIGHT("720"),
	DISPLAY_FULLSCREEN("false"),
	DISPLAY_RESIZABLE("false"),
	DISPLAY_VSYNC("false"),

	WORLD_WIDTH("3200"),
	WORLD_HEIGHT("1800"),

	INITIAL_PARTICLE_COUNT("50000"),
	MAX_PARTICLE_COUNT("1000000"),
	MAX_GRAVITY_WELL_COUNT("250");

	private final String DEFAULT_VALUE;

	private EnumConfigKey(String defaultValue) {
		this.DEFAULT_VALUE = defaultValue;
	}

	public String getDefaultValue() {
		return DEFAULT_VALUE;
	}
}
