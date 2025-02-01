package com.janilla.website;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T> {

	public static <T> Lazy<T> of(Supplier<T> supplier) {
		return new Lazy<T>(supplier);
	}

	final Supplier<T> supplier;

	final Lock lock = new ReentrantLock();

	volatile boolean got;

	T result;

	private Lazy(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	@Override
	public T get() {
		if (!got) {
			lock.lock();
			try {
				if (!got) {
					result = supplier.get();
					got = true;
				}
			} finally {
				lock.unlock();
			}
		}
		return result;
	}
}
