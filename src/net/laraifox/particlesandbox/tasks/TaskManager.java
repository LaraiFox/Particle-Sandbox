package net.laraifox.particlesandbox.tasks;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {
	protected Boolean threadFlag = new Boolean(false);

	private final Thread[] processingThreads;

	private List<IProcessingTask> taskQueue;

	public TaskManager() {
		this.processingThreads = new ProcessingThread[(int) Math.ceil(Runtime.getRuntime().availableProcessors() / 2.0f)];
		for (int i = 0; i < processingThreads.length; i++) {
			processingThreads[i] = new ProcessingThread(i, processingThreads.length, this);
		}

		this.taskQueue = new ArrayList<IProcessingTask>();
	}

	public void cleanUp() {
		for (int i = 0; i < processingThreads.length; i++) {
			((ProcessingThread) processingThreads[i]).closeThread();
		}

		threadFlag = true;
		threadFlag.notifyAll();
	}

	public void addTask(IProcessingTask task) {
		taskQueue.add(task);

		threadFlag = true;
		threadFlag.notifyAll();
	}

	public IProcessingTask getNextTask() {
		if (taskQueue.size() <= 0) {
			synchronized (threadFlag) {
				threadFlag = false;
			}

			return null;
		}

		synchronized (taskQueue) {
			return taskQueue.remove(0);
		}
	}

}
