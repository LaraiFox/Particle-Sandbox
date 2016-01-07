package net.laraifox.particlesandbox.opencl;

import java.io.IOException;

import net.laraifox.particlesandbox.utils.FileUtils;

import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.OpenCLException;

public class Kernel {
	private CLProgram program;
	private CLKernel kernel;

	public Kernel(CLContext context, String filepath) {
		try {
			this.program = CL10.clCreateProgramWithSource(context, FileUtils.readFile(filepath), null);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (CL10.clBuildProgram(program, devices.get(0), "", null) != CL10.CL_SUCCESS) {
			throw new OpenCLException(program.getBuildInfoString(devices.get(0), CL10.CL_PROGRAM_BUILD_LOG));
		}

		this.kernel = CL10.clCreateKernel(program, "main", null);
	}
}
