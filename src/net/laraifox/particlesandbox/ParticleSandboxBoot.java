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

import net.laraifox.particlesandbox.core.Configuration;
import net.laraifox.particlesandbox.core.EnumConfigKey;
import net.laraifox.particlesandbox.core.ProgramDisplay;

public class ParticleSandboxBoot {
	private static final String PROGRAM_NAME = new String("Particle Sandbox");
	private static final String VERSION = new String("3.0.0 alpha_006");
	private static final String TITLE = new String(PROGRAM_NAME + " " + VERSION);

	private static final int DEFAULT_WIDTH = 1280;
	private static final int DEFAULT_HEIGHT = 720;

	public static void main(String[] args) {
		try {
			CL.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
		}

		ParticleSandboxBoot.displayInfo();

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

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			switch (arg) {
			case "-w":
				if (args.length <= i + 1) {
					System.err.println("The desired width should be directly after -w parameter!");
				}
				try {
					Configuration.setValue(EnumConfigKey.DISPLAY_WIDTH, String.valueOf(Integer.valueOf(args[++i])));
				} catch (NumberFormatException e) {
					e.printStackTrace();
					System.err.println("The desired width after -w parameter must be a number!");
				}
				continue;
			case "-h":
				if (args.length <= i + 1) {
					System.err.println("The desired height should be directly after -h parameter!");
				}
				try {
					Configuration.setValue(EnumConfigKey.DISPLAY_HEIGHT, String.valueOf(Integer.valueOf(args[++i])));
				} catch (NumberFormatException e) {
					e.printStackTrace();
					System.err.println("The desired height after -h parameter must be a number!");
				}
				continue;
			case "-f":
				Configuration.setValue(EnumConfigKey.DISPLAY_FULLSCREEN, String.valueOf(true));
				break;
			case "-help":
				System.out.println("-f                 - Changes the window mode to fullscreen if supported.");
				System.out.println("-h <number>        - Specifies the desired window height.");
				System.out.println("-help              - Prints all command line arguments available.");
				System.out.println("-v                 - Sets the window to use v-sync.");
				System.out.println("-w <number>        - Specifies the desired window width.");
				System.exit(0);
			default:
				break;
			}
		}

		ProgramDisplay display = new ProgramDisplay(TITLE);
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

				System.out.printf(Locale.ENGLISH, "  Device #%d(%s): %s\n", deviceIndex, getDeviceType(device.getInfoInt(CL10.CL_DEVICE_TYPE)), device.getInfoString(
						CL10.CL_DEVICE_NAME));

				System.out.printf(Locale.ENGLISH, "Compute Units:  %d @ %d MHz\n", device.getInfoInt(CL10.CL_DEVICE_MAX_COMPUTE_UNITS), device.getInfoInt(
						CL10.CL_DEVICE_MAX_CLOCK_FREQUENCY));

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
