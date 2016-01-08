package net.laraifox.particlesandbox.opencl;

import java.io.IOException;

import net.laraifox.particlesandbox.interfaces.ICLDataBuffer;
import net.laraifox.particlesandbox.utils.FileUtils;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLProgram;

public class Kernel {
	public static final String KERNEL_NAME = new String("main");

	private CLProgram program;
	private CLKernel kernel;
	private int workDimentions;
	private PointerBuffer globalWorkOffset;
	private PointerBuffer globalWorkSize;
	private PointerBuffer localWorkSize;

	public Kernel(CLContext context, CLDevice device, String filepath, int workDimentions, PointerBuffer globalWorkOffset, PointerBuffer globalWorkSize, PointerBuffer localWorkSize)
			throws IOException {
		this.program = CL10.clCreateProgramWithSource(context, FileUtils.readFile(filepath), null);
		if (CL10.clBuildProgram(program, device, "", null) != CL10.CL_SUCCESS) {
			throw new RuntimeException(program.getBuildInfoString(device, CL10.CL_PROGRAM_BUILD_LOG));
		}

		this.kernel = CL10.clCreateKernel(program, KERNEL_NAME, null);
		this.workDimentions = workDimentions;
		this.globalWorkOffset = globalWorkOffset;
		this.globalWorkSize = globalWorkSize;
		this.localWorkSize = localWorkSize;
	}

	@Override
	public void finalize() {
		CL10.clReleaseKernel(kernel);
		CL10.clReleaseProgram(program);
	}

	public int enqueueNDRangeKernel(CLCommandQueue queue, PointerBuffer eventWaitList, PointerBuffer event) {
		return CL10.clEnqueueNDRangeKernel(queue, kernel, workDimentions, globalWorkOffset, globalWorkSize, localWorkSize, eventWaitList, event);
	}

	public void setArg(int index, ICLDataBuffer buffer) {
		kernel.setArg(index, buffer.getAddress());
	}

	public void setArgs(int[] indices, ICLDataBuffer[] buffers) {
		for (int i = 0; i < indices.length; i++) {
			kernel.setArg(indices[i], buffers[i].getAddress());
		}
	}

	public void setGlobalWorkOffset(int index, long value) {
		globalWorkOffset.put(index, value);
	}

	public void setGlobalWorkSize(int index, long value) {
		globalWorkSize.put(index, value);
	}

	public void setLocalWorkSize(int index, long value) {
		localWorkSize.put(index, value);
	}
}
