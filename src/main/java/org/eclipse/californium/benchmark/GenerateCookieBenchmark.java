package org.eclipse.californium.benchmark;

import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.californium.scandium.dtls.CertificateTypeExtension.CertificateType;
import org.eclipse.californium.scandium.dtls.ClientHello;
import org.eclipse.californium.scandium.dtls.CompressionMethod;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.ProtocolVersion;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class GenerateCookieBenchmark {

	@State(Scope.Benchmark)
	public static class OriginalState {

		@Setup(Level.Trial)
		public void doSetup() throws InterruptedException {
			g = new CookieGenerator();
		}

		public CookieGenerator g;
		public ClientHello ch;

		@Setup(Level.Invocation)
		public void prepare() {
			ch = createClientHello(null);
		}
	}

	@State(Scope.Benchmark)
	public static class EnhanceState {

		@Setup(Level.Trial)
		public void doSetup() throws InterruptedException {
			g = new EnhancedCookieGenerator();
		}

		public EnhancedCookieGenerator g;
		public ClientHello ch;

		@Setup(Level.Invocation)
		public void prepare() {
			ch = createClientHello(null);
		}
	}

	@State(Scope.Benchmark)
	public static class EnhanceState2 {

		@Setup(Level.Trial)
		public void doSetup() throws InterruptedException {
			g = new EnhancedCookieGenerator2();
		}

		public EnhancedCookieGenerator2 g;
		public ClientHello ch;

		@Setup(Level.Invocation)
		public void prepare() {
			ch = createClientHello(null);
		}
	}

	
	@Benchmark
	public void testOriginalGenerateCookie(OriginalState state) throws GeneralSecurityException {
		state.g.generateCookie(state.ch);
	}

	@Benchmark
	public void testEnhancedGenerateCookie(EnhanceState state) throws Exception {
		state.g.generateCookie(state.ch);
	}

	@Benchmark
	public void testEnhancedGenerateCookie2(EnhanceState2 state) throws Exception {
		state.g.generateCookie(state.ch);
	}

	private static ClientHello createClientHello(DTLSSession sessionToResume) {
		ClientHello hello = null;
		if (sessionToResume == null) {
			hello = new ClientHello(new ProtocolVersion(), new SecureRandom(),Collections.<CipherSuite> emptyList(),
					Collections.<CertificateType> emptyList(), Collections.<CertificateType> emptyList(),
					new InetSocketAddress(2000));
		} else {
			hello = new ClientHello(new ProtocolVersion(), new SecureRandom(), sessionToResume, null, null);
		}
		hello.addCompressionMethod(CompressionMethod.NULL);
		hello.setMessageSeq(0);
		return hello;
	}

	static int keyLifetime = Integer.MAX_VALUE; // ms

	public static class CookieGenerator {

		// guard access to cookieMacKey
		private Object cookieMacKeyLock = new Object();
		// last time when the master key was generated
		private long lastGenerationDate = System.currentTimeMillis();
		private SecretKey cookieMacKey = new SecretKeySpec(randomBytes(), "MAC");

		/** generate a random byte[] of length 32 **/
		private static byte[] randomBytes() {
			SecureRandom rng = new SecureRandom();
			byte[] result = new byte[32];
			rng.nextBytes(result);
			return result;
		}

		private SecretKey getMacKeyForCookies() {
			synchronized (cookieMacKeyLock) {
				// if the last generation was more than 5 minute ago, let's
				// generate
				// a new key
				if (System.currentTimeMillis() - lastGenerationDate > keyLifetime) {
					cookieMacKey = new SecretKeySpec(randomBytes(), "MAC");
					lastGenerationDate = System.currentTimeMillis();
				}
				return cookieMacKey;
			}

		}

		private byte[] generateCookie(ClientHello clientHello) throws GeneralSecurityException {
			// Cookie = HMAC(Secret, Client-IP, Client-Parameters)
			Mac hmac = Mac.getInstance("HmacSHA256");
			hmac.init(getMacKeyForCookies());
			// Client-IP
			hmac.update(clientHello.getPeer().toString().getBytes());

			// Client-Parameters
			hmac.update((byte) clientHello.getClientVersion().getMajor());
			hmac.update((byte) clientHello.getClientVersion().getMinor());
			hmac.update(clientHello.getRandom().getRandomBytes());
			hmac.update(clientHello.getSessionId().getId());
			hmac.update(CipherSuite.listToByteArray(clientHello.getCipherSuites()));
			hmac.update(CompressionMethod.listToByteArray(clientHello.getCompressionMethods()));
			return hmac.doFinal();
		}
	}

	public static class EnhancedCookieGenerator {

		/** generate a random byte[] of length 32 **/
		private SecureRandom rng = new SecureRandom();
		byte[] result = new byte[32];

		private byte[] randomBytes() {
			rng.nextBytes(result);
			return result;
		}

		private Mac hmac;
		private Object cookieMacKeyLock = new Object();
		private long lastGenerationDate = System.currentTimeMillis();

		private byte[] generateCookie(ClientHello clientHello) throws Exception {
			// Cookie = HMAC(Secret, Client-IP, Client-Parameters)
			Mac hmac = getHMAC();
			// Client-IP
			hmac.update(clientHello.getPeer().toString().getBytes());
			// Client-Parameters
			hmac.update((byte) clientHello.getClientVersion().getMajor());
			hmac.update((byte) clientHello.getClientVersion().getMinor());
			hmac.update(clientHello.getRandom().getRandomBytes());
			hmac.update(clientHello.getSessionId().getId());
			hmac.update(CipherSuite.listToByteArray(clientHello.getCipherSuites()));
			hmac.update(CompressionMethod.listToByteArray(clientHello.getCompressionMethods()));
			return hmac.doFinal();
		}

		private Mac getHMAC() throws NoSuchAlgorithmException, CloneNotSupportedException, InvalidKeyException {
			synchronized (cookieMacKeyLock) {
				if (hmac == null) {
					hmac = Mac.getInstance("HmacSHA256");
					hmac.init(new SecretKeySpec(randomBytes(), "MAC"));
				} else {
					// if the last generation was more than 5 minute ago, let's
					// generate
					// a new key
					if (System.currentTimeMillis() - lastGenerationDate > keyLifetime) {
						hmac.init(new SecretKeySpec(randomBytes(), "MAC"));
						lastGenerationDate = System.currentTimeMillis();
					}
				}
				return (Mac) hmac.clone();
			}
		}
	}

	public static class EnhancedCookieGenerator2 {

		private long lastGenerationDate;
		private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		private Mac hmac;

		private final SecureRandom rng = new SecureRandom();
		byte[] rd = new byte[32];

		private Mac getHMAC() throws NoSuchAlgorithmException, CloneNotSupportedException, InvalidKeyException {
			lock.readLock().lock();
			try {
				if (hmac != null && !isKeyExpired()) {
					return (Mac) hmac.clone();
				}
			} finally {
				lock.readLock().unlock();
			}

			// if key expired or hmac not initialized;
			lock.writeLock().lock();
			try {
				// Recheck state because another thread might have acquired
				// write lock and changed state before we did.
				if (hmac == null) {
					hmac = Mac.getInstance("HmacSHA256");
					hmac.init(generateSecretKey());
				}
				if (isKeyExpired()) {
					hmac.init(generateSecretKey());
				}
				return (Mac) hmac.clone();
			} finally {
				lock.writeLock().unlock();
			}
		}

		private boolean isKeyExpired() {
			return System.currentTimeMillis() - lastGenerationDate > keyLifetime;
		}

		private SecretKeySpec generateSecretKey() {
			lastGenerationDate = System.currentTimeMillis();
			rng.nextBytes(rd);
			return new SecretKeySpec(rd, "MAC");
		}

		public byte[] generateCookie(final ClientHello clientHello) throws GeneralSecurityException, CloneNotSupportedException{
			// Cookie = HMAC(Secret, Client-IP, Client-Parameters)
			final Mac hmac = getHMAC();
			// Client-IP
			hmac.update(clientHello.getPeer().toString().getBytes());
			// Client-Parameters
			hmac.update((byte) clientHello.getClientVersion().getMajor());
			hmac.update((byte) clientHello.getClientVersion().getMinor());
			hmac.update(clientHello.getRandom().getRandomBytes());
			hmac.update(clientHello.getSessionId().getId());
			hmac.update(CipherSuite.listToByteArray(clientHello.getCipherSuites()));
			hmac.update(CompressionMethod.listToByteArray(clientHello.getCompressionMethods()));
			return hmac.doFinal();
		}
	}
}
