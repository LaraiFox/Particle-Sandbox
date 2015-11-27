package net.laraifox.particlesandbox;

import java.io.File;

import net.laraifox.particlesandbox.core.ProgramDisplay;

public class ParticleSimulatorBoot {
	private static final String PROGRAM_NAME = new String("Particle Sandbox");
	private static final String VERSION = new String("3.0.0 alpha");
	private static final String TITLE = new String(PROGRAM_NAME + " " + VERSION);

	private static final int WIDTH = 1280;
	private static final int HEIGHT = 720;

	public static void main(String[] args) {
		String operatingSystem = System.getProperty("os.name").toLowerCase();
		String username = System.getProperty("user.name");

		if (operatingSystem.contains("win")) {
			System.setProperty("org.lwjgl.librarypath", new File("lib/lwjgl/native/windows").getAbsolutePath());
		} else if (operatingSystem.contains("mac")) {
			System.setProperty("org.lwjgl.librarypath", new File("lib/lwjgl/native/macosx").getAbsolutePath());
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", PROGRAM_NAME);
		} else if (operatingSystem.contains("linux")) {
			System.setProperty("org.lwjgl.librarypath", new File("lib/lwjgl/native/linux").getAbsolutePath());
		} else {
			System.err.println("Your Operating System (" + operatingSystem + ") is unrecognised or unsupported");
			new Exception().printStackTrace();
			System.exit(1);
		}

		ProgramDisplay display = new ProgramDisplay(TITLE, WIDTH, HEIGHT, false, false);
		display.start();
	}
}
