package org.eclipse.californium.benchmark;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import org.openjdk.jmh.annotations.Benchmark;

public class CipherBenchmark {

	private static final String BLOCK_CIPHER = "AES";
	
	static AtomicReference<Provider> p = new AtomicReference<>();
	static Object lock = new Object();
	private static Cipher getCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
		if (p.get() == null) {
			synchronized (lock) {
				if (p.get() == null) {
					Cipher cipher = Cipher.getInstance(BLOCK_CIPHER);
					p.set(cipher.getProvider());
					return cipher;
				}
			}
		}
		return Cipher.getInstance(BLOCK_CIPHER, p.get());
	}
	
	@Benchmark
	public void getInstance() throws GeneralSecurityException {
		Cipher.getInstance(BLOCK_CIPHER);
	}

	@Benchmark
	public void getInstance2() throws Exception {
		getCipher();
	}

}
