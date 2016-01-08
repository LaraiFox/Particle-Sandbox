package net.laraifox.particlesandbox.interfaces;

import org.lwjgl.opencl.CLMem;

public interface ICLDataBuffer {
	public CLMem getAddress();
}
