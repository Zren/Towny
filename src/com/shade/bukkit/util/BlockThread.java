package com.shade.bukkit.util;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BlockThread extends Thread {
	private BlockQueue blockQueue;
	private Server server;
	public static final Object NO_MORE_WORK = new Object();
	public static final Object END_JOB = new Object();

	private boolean running;

	private Job currentJob;
	private int blocks, skipped;
	private World currentWorld;

	public BlockThread(Server server, BlockQueue blockQueue) {
		this.blockQueue = blockQueue;
		this.setServer(server);
		setRunning(true);
	}

	public synchronized void setRunning(boolean running) {
		this.running = running;
	}

	@Override
	public void run() {
		blocks = 0;
		skipped = 0;

		try {
			while (running) {
				Object obj = blockQueue.getWork();

				if (obj == NO_MORE_WORK)
					break;

				if (obj == END_JOB)
					onJobFinish(currentJob);

				if (obj instanceof Block) {
					try {
						buildBlock((Block) obj);
					} catch (Exception e) {
						skipped++;
					}
					;
					blocks++;
				}

				if (obj instanceof Job) {
					currentJob = (Job) obj;
					blocks = 0;
					skipped = 0;
				}
			}
		} catch (InterruptedException e) {
		}
		;

		System.out.println("[Blocker] BlockQueue Thread stoped.");
		blockQueue = null;
	}

	public void buildBlock(Block block) {
		try {
			sleep(25);
		} catch (InterruptedException e) {
		}

		if (block.getTypeId() == currentJob.getWorld().getBlockTypeIdAt(
				block.getX(), block.getY(), block.getZ()))
			return;

		// TODO: Set block

	}

	public void setServer(Server server) {
		this.server = server;
	}

	public Server getServer() {
		return server;
	}

	public void onJobFinish(Job job) {
		if (job.isNotify()) {
			Player player = getServer().getPlayer(job.getBoss());
			player.sendMessage("Generated: " + blocks + " Blocks");
			if (skipped > 0)
				player.sendMessage("Skipped: " + skipped + " Blocks");
		}
	}

	public void setCurrentWorld(World currentWorld) {
		this.currentWorld = currentWorld;
	}

	public World getCurrentWorld() {
		return currentWorld;
	}
}
