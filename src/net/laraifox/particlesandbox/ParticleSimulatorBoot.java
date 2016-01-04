package net.laraifox.particlesandbox;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Locale;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLProgram;

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

		displayInfo();

//		try {
//			ParticleSimulatorBoot.openCLTesting();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (LWJGLException e) {
//			e.printStackTrace();
//		}

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

	// Data buffers to store the input and result data in
	static final FloatBuffer a = UtilCL.toFloatBuffer(new float[] {
			1, 2, 3, 4, 5, 6, 7, 8, 9, 10
	});
	static final FloatBuffer b = UtilCL.toFloatBuffer(new float[] {
			9, 8, 7, 6, 5, 4, 3, 2, 1, 0
	});
	static final FloatBuffer answer = BufferUtils.createFloatBuffer(a.capacity());

	private static void openCLTesting() throws IOException, LWJGLException {
		// Initialize OpenCL and create a context and command queue
		CL.create();

		CLPlatform platform = CLPlatform.getPlatforms().get(0);
		List<CLDevice> devices = platform.getDevices(CL10.CL_DEVICE_TYPE_GPU);
		CLContext context = CLContext.create(platform, devices, null, null, null);
		CLCommandQueue queue = CL10.clCreateCommandQueue(context, devices.get(0), CL10.CL_QUEUE_PROFILING_ENABLE, null);

		// Allocate memory for our two input buffers and our result buffer
		CLMem aMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, a, null);
		CL10.clEnqueueWriteBuffer(queue, aMem, 1, 0, a, null, null);
		CLMem bMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, b, null);
		CL10.clEnqueueWriteBuffer(queue, bMem, 1, 0, b, null, null);
		CL10.clFinish(queue);

		// Load the source from a resource file
		String source = UtilCL.getResourceAsString("./res/kernels/Particle Movement.cl");

		// Create our program and kernel
		CLProgram program = CL10.clCreateProgramWithSource(context, source, null);
		CL10.clBuildProgram(program, devices.get(0), "", null);
		System.out.println(program.getBuildInfoString(devices.get(0), CL10.CL_PROGRAM_BUILD_LOG));
		// sum has to match a kernel method name in the OpenCL source
		CLKernel kernel = CL10.clCreateKernel(program, "main", null);

		// Execution our kernel
		PointerBuffer kernel1DGlobalWorkSize = BufferUtils.createPointerBuffer(1);
		kernel1DGlobalWorkSize.put(0, a.capacity());
		kernel.setArg(0, aMem);
		kernel.setArg(1, bMem);
		CL10.clEnqueueNDRangeKernel(queue, kernel, 1, null, kernel1DGlobalWorkSize, null, null, null);

		// Read the results memory back into our result buffer
		CL10.clEnqueueReadBuffer(queue, aMem, 1, 0, a, null, null);
		CL10.clEnqueueReadBuffer(queue, bMem, 1, 0, b, null, null);
		CL10.clFinish(queue);

		float[] aArray = new float[a.capacity()];
		float[] bArray = new float[b.capacity()];

		a.get(aArray);
		b.get(bArray);

		// Print the result memory
		for (int i = 0; i < aArray.length; i++)
			System.out.print(aArray[i] + " ");
		System.out.println("+");
		for (int i = 0; i < aArray.length; i++)
			System.out.print(bArray[i] + " ");
		System.out.println("=");

		// Clean up OpenCL resources
		CL10.clReleaseKernel(kernel);
		CL10.clReleaseProgram(program);
		CL10.clReleaseMemObject(aMem);
		CL10.clReleaseMemObject(bMem);
		CL10.clReleaseCommandQueue(queue);
		CL10.clReleaseContext(context);
		CL.destroy();

		System.exit(0);
	}

	private static void displayInfo() {
		for (int platformIndex = 0; platformIndex < CLPlatform.getPlatforms().size(); platformIndex++) {
			CLPlatform platform = CLPlatform.getPlatforms().get(platformIndex);
			System.out.println("\tPlatform #" + platformIndex + ": " + platform.getInfoString(CL10.CL_PLATFORM_NAME));

			List<CLDevice> devices = platform.getDevices(CL10.CL_DEVICE_TYPE_ALL);
			for (int deviceIndex = 0; deviceIndex < devices.size(); deviceIndex++) {
				CLDevice device = devices.get(deviceIndex);

				System.out.printf(Locale.ENGLISH, "  Device #%d(%s): %s\n", deviceIndex, UtilCL.getDeviceType(device.getInfoInt(CL10.CL_DEVICE_TYPE)),
						device.getInfoString(CL10.CL_DEVICE_NAME));

				System.out.printf(Locale.ENGLISH, "Compute Units:  %d @ %d MHz\n", device.getInfoInt(CL10.CL_DEVICE_MAX_COMPUTE_UNITS),
						device.getInfoInt(CL10.CL_DEVICE_MAX_CLOCK_FREQUENCY));

				System.out.printf(Locale.ENGLISH, "Max Work Group: %d \n", device.getInfoInt(CL10.CL_DEVICE_MAX_WORK_GROUP_SIZE));

				System.out.printf(Locale.ENGLISH, "Max Work Group: %d \n", device.getInfoInt(CL10.CL_KERNEL_WORK_GROUP_SIZE));

				System.out.printf(Locale.ENGLISH, "Local memory:   %s\n", UtilCL.formatMemory(device.getInfoLong(CL10.CL_DEVICE_LOCAL_MEM_SIZE)));

				System.out.printf(Locale.ENGLISH, "Global memory:  %s\n", UtilCL.formatMemory(device.getInfoLong(CL10.CL_DEVICE_GLOBAL_MEM_SIZE)));

				System.out.println();
			}
		}
	}
}
