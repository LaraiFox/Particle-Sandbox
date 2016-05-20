package net.laraifox.particlesandbox.tasks;

public class ProcessingThread extends Thread {
	private final TaskManager taskManager;

	private int threadIndex;
	private int threadCount;
	private boolean running;

	private IProcessingTask currentTask;

	public ProcessingThread(int threadIndex, int threadCount, TaskManager taskManager) {
		super("Particle Physics Thread #" + threadIndex);

		this.taskManager = taskManager;

		this.threadIndex = threadIndex;
		this.threadCount = threadCount;
		this.running = true;
	}

	public void closeThread() {
		this.running = false;
	}

	public void run() {
		while (running) {
			try {
				while (!taskManager.threadFlag) {
					taskManager.threadFlag.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			while ((currentTask = taskManager.getNextTask()) != null) {
				currentTask.process(threadIndex, threadCount);
			}
		}
	}
}
