package com.pandaismyname1.ultraterraforged.concurrent.task;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

public abstract class LazyCallable<T> implements Callable<T>, Future<T>, Supplier<T> {
	private StampedLock lock;
	protected volatile T value;

	public LazyCallable() {
		this.lock = new StampedLock();
		this.value = null;
	}

	@Override
	public T call() {
		long optRead = this.lock.tryOptimisticRead();
		T result = this.value;
		if (this.lock.validate(optRead) && result != null) {
			return result;
		}
		long read = this.lock.readLock();
		try {
			result = this.value;
			if (result != null) {
				return result;
			}
		} finally {
			this.lock.unlockRead(read);
		}
		long write = this.lock.writeLock();
		try {
			result = this.value;
			if (result == null) {
				result = this.create();
				Objects.requireNonNull(result);
				this.value = result;
			}
			return result;
		} finally {
			this.lock.unlockWrite(write);
		}
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		long optRead = this.lock.tryOptimisticRead();
		boolean done = this.value != null;
		if (this.lock.validate(optRead)) {
			return done;
		}
		long read = this.lock.readLock();
		try {
			return this.value != null;
		} finally {
			this.lock.unlockRead(read);
		}
	}

	@Override
	public T get() {
		return this.call();
	}

	@Override
	public T get(long timeout, TimeUnit unit) {
		return this.call();
	}

	protected abstract T create();
}
