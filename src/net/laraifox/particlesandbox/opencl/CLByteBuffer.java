package net.laraifox.particlesandbox.opencl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import net.laraifox.particlesandbox.interfaces.ICLDataBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLMem;

public class CLByteBuffer implements ICLDataBuffer {
	private ByteBuffer buffer;
	private CLMem address;

	public CLByteBuffer(int capacity, CLContext context, int flags, IntBuffer errorCodeReturn) {
		this.buffer = ByteBuffer.allocateDirect(capacity * Byte.BYTES).order(ByteOrder.nativeOrder());
		this.address = CL10.clCreateBuffer(context, flags, buffer, errorCodeReturn);
	}

	@Override
	public void finalize() {
		CL10.clReleaseMemObject(address);
	}

	public int enqueueCopyBuffer(CLCommandQueue queue, CLByteBuffer dest, long src_offset, long dest_offset, long size, PointerBuffer event_wait_list, PointerBuffer event) {
		return CL10.clEnqueueCopyBuffer(queue, this.address, dest.address, src_offset, dest_offset, size, event_wait_list, event);
	}

	public int enqueueWriteBuffer(CLCommandQueue queue, int blocking_write, long offset, PointerBuffer event_wait_list, PointerBuffer event) {
		return CL10.clEnqueueWriteBuffer(queue, address, blocking_write, offset, buffer, event_wait_list, event);
	}

	public int enqueueReadBuffer(CLCommandQueue queue, int blocking_write, long offset, PointerBuffer event_wait_list, PointerBuffer event) {
		return CL10.clEnqueueReadBuffer(queue, address, blocking_write, offset, buffer, event_wait_list, event);
	}
	
	public byte get() {
		return buffer.get();
	}

	public byte get(int i) {
		return buffer.get(i);
	}

	public void put(byte value) {
		buffer.put(value);
	}

	public void put(int i, byte value) {
		buffer.put(i, value);
	}

	public void rewind() {
		buffer.rewind();
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}

	public CLMem getAddress() {
		return address;
	}
}
