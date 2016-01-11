package net.laraifox.particlesandbox;

import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_ACCELERATOR;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_CPU;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_DEFAULT;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_GPU;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

import org.lwjgl.LWJGLException;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLPlatform;

import net.laraifox.particlesandbox.core.ProgramDisplay;

public class ParticleSimulatorBoot {
	private static final String PROGRAM_NAME = new String("Particle Sandbox");
	private static final String VERSION = new String("3.0.0 alpha_006");
	private static final String TITLE = new String(PROGRAM_NAME + " " + VERSION);

	private static final int WIDTH = 1280;
	private static final int HEIGHT = 720;

	public static void main(String[] args) {
		try {
			CL.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
		}

		ParticleSimulatorBoot.displayInfo();

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

		CL.destroy();
	}

	private static void displayInfo() {
		for (int platformIndex = 0; platformIndex < CLPlatform.getPlatforms().size(); platformIndex++) {
			CLPlatform platform = CLPlatform.getPlatforms().get(platformIndex);
			System.out.println("\tPlatform #" + platformIndex + ": " + platform.getInfoString(CL10.CL_PLATFORM_NAME));

			List<CLDevice> devices = platform.getDevices(CL10.CL_DEVICE_TYPE_ALL);
			for (int deviceIndex = 0; deviceIndex < devices.size(); deviceIndex++) {
				CLDevice device = devices.get(deviceIndex);

				System.out.printf(Locale.ENGLISH, "  Device #%d(%s): %s\n", deviceIndex, getDeviceType(device.getInfoInt(CL10.CL_DEVICE_TYPE)),
						device.getInfoString(CL10.CL_DEVICE_NAME));

				System.out.printf(Locale.ENGLISH, "Compute Units:  %d @ %d MHz\n", device.getInfoInt(CL10.CL_DEVICE_MAX_COMPUTE_UNITS),
						device.getInfoInt(CL10.CL_DEVICE_MAX_CLOCK_FREQUENCY));

				System.out.printf(Locale.ENGLISH, "Max Work Group: %d \n", device.getInfoInt(CL10.CL_DEVICE_MAX_WORK_GROUP_SIZE));

				System.out.printf(Locale.ENGLISH, "Max Work Group: %d \n", device.getInfoInt(CL10.CL_KERNEL_WORK_GROUP_SIZE));

				System.out.printf(Locale.ENGLISH, "Local memory:   %s\n", formatMemory(device.getInfoLong(CL10.CL_DEVICE_LOCAL_MEM_SIZE)));

				System.out.printf(Locale.ENGLISH, "Global memory:  %s\n", formatMemory(device.getInfoLong(CL10.CL_DEVICE_GLOBAL_MEM_SIZE)));

				System.out.println();
			}
		}
	}
	
	private static String formatMemory(long size) {
		if (size <= 0)
			return "0";
		final String[] units = new String[] {
				"B", "KB", "MB", "GB", "TB"
		};
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
	
	private static String getDeviceType(int i) {
		switch (i) {
		case CL_DEVICE_TYPE_DEFAULT:
			return "DEFAULT";
		case CL_DEVICE_TYPE_CPU:
			return "CPU";
		case CL_DEVICE_TYPE_GPU:
			return "GPU";
		case CL_DEVICE_TYPE_ACCELERATOR:
			return "ACCELERATOR";
		}
		return "?";
	}
}
