package com.shade.bukkit.util;

import java.util.LinkedList;

import org.bukkit.Server;

public class BlockQueue {
	private LinkedList<Object> queue = new LinkedList<Object>();
	private static volatile BlockQueue instance;
	private BlockThread thread;

	public synchronized void addWork(Object obj) {
		queue.addLast(obj);
		notify();
	}

	public synchronized Object getWork() throws InterruptedException {
		while (queue.isEmpty()) {
			wait();
		}
		return queue.removeFirst();
	}

	public static BlockQueue getInstance() throws Exception {
		if (instance == null)
			throw new Exception("BlockQueue has not been initialized yet");

		return instance;
	}

	public static BlockQueue newInstance(Server server) {
		instance = new BlockQueue();
		instance.thread = new BlockThread(server, instance);
		instance.thread.start();

		return instance;
	}

	public BlockThread getThread() {
		return thread;
	}
}
