package net.laraifox.particlesandbox.tasks;

public interface IProcessingTask {
	public void process(int threadIndex, int threadCount);
}
