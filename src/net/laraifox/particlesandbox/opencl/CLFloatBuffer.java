package net.laraifox.particlesandbox.opencl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import net.laraifox.particlesandbox.interfaces.ICLDataBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLMem;

public class CLFloatBuffer implements ICLDataBuffer {
	private FloatBuffer buffer;
	private CLMem address;

	public CLFloatBuffer(int capacity, CLContext context, int flags, IntBuffer errorCodeReturn) {
		this.buffer = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
		this.address = CL10.clCreateBuffer(context, flags, buffer, errorCodeReturn);
	}

	@Override
	public void finalize() {
		CL10.clReleaseMemObject(address);
	}

	public int enqueueCopyBuffer(CLCommandQueue queue, CLFloatBuffer dest, long src_offset, long dest_offset, long size, PointerBuffer event_wait_list, PointerBuffer event) {
		return CL10.clEnqueueCopyBuffer(queue, this.address, dest.address, src_offset, dest_offset, size, event_wait_list, event);
	}

	public int enqueueWriteBuffer(CLCommandQueue queue, int blocking_write, long offset, PointerBuffer event_wait_list, PointerBuffer event) {
		return CL10.clEnqueueWriteBuffer(queue, address, blocking_write, offset, buffer, event_wait_list, event);
	}

	public int enqueueReadBuffer(CLCommandQueue queue, int blocking_write, long offset, PointerBuffer event_wait_list, PointerBuffer event) {
		return CL10.clEnqueueReadBuffer(queue, address, blocking_write, offset, buffer, event_wait_list, event);
	}

	public FloatBuffer getBuffer() {
		return buffer;
	}

	public CLMem getAddress() {
		return address;
	}
}
