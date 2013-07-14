package com.veivo.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaskThreadPool extends ThreadPoolExecutor{
	
	public static TaskThreadPool getThreadPool() {
		return InstanceHolder.instance;
	}
	
	private static class InstanceHolder {
		public static TaskThreadPool instance = new TaskThreadPool();
	}

	private TaskThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}
	
	private TaskThreadPool() {// same as Executors new CachedThreadPool
		this(0, Integer.MAX_VALUE, 200L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
	}
	
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		Log.debug(TaskThreadPool.class, "Running Thread " + t);
	}
	
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		Log.debug(TaskThreadPool.class, "Thread complete " + t);
	}
}
