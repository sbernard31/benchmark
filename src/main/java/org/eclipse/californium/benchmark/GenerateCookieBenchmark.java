package org.eclipse.californium.benchmark;

import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertLevel;
import org.eclipse.californium.scandium.dtls.CertificateTypeExtension.CertificateType;
import org.eclipse.californium.scandium.dtls.ClientHello;
import org.eclipse.californium.scandium.dtls.CompressionMethod;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.DtlsHandshakeException;
import org.eclipse.californium.scandium.dtls.ProtocolVersion;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class GenerateCookieBenchmark {

	@State(Scope.Thread)
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

	@State(Scope.Thread)
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
	
	@State(Scope.Thread)
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
	public void testOriginalGenerateCookie(OriginalState state) {
		state.g.generateCookie(state.ch);
	}

	@Benchmark
	public void testEnhancedGenerateCookie(EnhanceState state) {
		state.g.generateCookie(state.ch);
	}
	
	@Benchmark
	public void testEnhancedGenerateCookie2(EnhanceState2 state) {
		state.g.generateCookie(state.ch);
	}

	private static ClientHello createClientHello(DTLSSession sessionToResume) {
		ClientHello hello = null;
		if (sessionToResume == null) {
			hello = new ClientHello(new ProtocolVersion(), new SecureRandom(),
					Collections.<CertificateType> emptyList(), Collections.<CertificateType> emptyList(),
					new InetSocketAddress(2000));
		} else {
			hello = new ClientHello(new ProtocolVersion(), new SecureRandom(), sessionToResume, null, null);
		}
		hello.addCipherSuite(CipherSuite.TLS_PSK_WITH_AES_128_CCM_8);
		hello.addCipherSuite(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8);
		hello.addCompressionMethod(CompressionMethod.NULL);
		hello.setMessageSeq(0);
		return hello;
	}
	
	static int cookieLifeTime = 1000; //ms

	public static class CookieGenerator {

		/** generate a random byte[] of length 32 **/
		private byte[] randomBytes() {
			SecureRandom rng = new SecureRandom();
			byte[] result = new byte[32];
			rng.nextBytes(result);
			return result;
		}

		private SecretKey cookieMacKey = new SecretKeySpec(randomBytes(), "MAC");
		private Object cookieMacKeyLock = new Object();

		private SecretKey getMacKeyForCookies() {
			synchronized (cookieMacKeyLock) {
				// if the last generation was more than 5 minute ago, let's
				// generate
				// a new key
				if (System.currentTimeMillis() - lastGenerationDate > cookieLifeTime) {
					cookieMacKey = new SecretKeySpec(randomBytes(), "MAC");
					lastGenerationDate = System.currentTimeMillis();
				}
				return cookieMacKey;
			}

		}

		private long lastGenerationDate = System.currentTimeMillis();

		private byte[] generateCookie(ClientHello clientHello) {
			try {
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
			} catch (GeneralSecurityException e) {
				throw new DtlsHandshakeException("Cannot compute cookie for peer", AlertDescription.INTERNAL_ERROR,
						AlertLevel.FATAL, clientHello.getPeer(), e);
			}
		}
	}

		
	public static class EnhancedCookieGenerator {

		/** generate a random byte[] of length 32 **/
		private SecureRandom rng = new SecureRandom();

		private byte[] randomBytes() {
			byte[] result = new byte[32];
			rng.nextBytes(result);
			return result;
		}

		private SecretKey cookieMacKey = new SecretKeySpec(randomBytes(), "MAC");
		private Object cookieMacKeyLock = new Object();

		private SecretKey getMacKeyForCookies() {
			synchronized (cookieMacKeyLock) {
				// if the last generation was more than 5 minute ago, let's
				// generate
				// a new key
				if (System.currentTimeMillis() - lastGenerationDate > cookieLifeTime) {
					cookieMacKey = new SecretKeySpec(randomBytes(), "MAC");
					lastGenerationDate = System.currentTimeMillis();
				}
				return cookieMacKey;
			}

		}

		private long lastGenerationDate = System.currentTimeMillis();

		Mac hmac;

		private byte[] generateCookie(ClientHello clientHello) {
			try {
				// Cookie = HMAC(Secret, Client-IP, Client-Parameters)
				Mac hmac = getHMAC();
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
			} catch (GeneralSecurityException e) {
				throw new DtlsHandshakeException("Cannot compute cookie for peer", AlertDescription.INTERNAL_ERROR,
						AlertLevel.FATAL, clientHello.getPeer(), e);
			}
		}

		private Mac getHMAC() throws NoSuchAlgorithmException {
			if (hmac == null) {
				hmac = Mac.getInstance("HmacSHA256");
			}
			return hmac;
		}
	}
	
	public static class EnhancedCookieGenerator2 {

		/** generate a random byte[] of length 32 **/
		private SecureRandom rng = new SecureRandom();

		private byte[] randomBytes() {
			byte[] result = new byte[32];
			rng.nextBytes(result);
			return result;
		}

		private SecretKey cookieMacKey = new SecretKeySpec(randomBytes(), "MAC");
		private ReentrantReadWriteLock cookieMacKeyLock = new ReentrantReadWriteLock();

		private SecretKey getMacKeyForCookies() {
			cookieMacKeyLock.readLock().lock();
			// if the last generation was more than 5 minute ago, let's generate
			// a new key
			try {
				if (!(System.currentTimeMillis() - lastGenerationDate > cookieLifeTime)) {
					return cookieMacKey;
				}
			} finally {
				cookieMacKeyLock.readLock().unlock();
			}

			// Must release read lock before acquiring write lock
			cookieMacKeyLock.writeLock().lock();
			try {
				// Recheck state because another thread might have acquired
				// write lock and changed state before we did.
				if (System.currentTimeMillis() - lastGenerationDate > cookieLifeTime) {
					cookieMacKey = new SecretKeySpec(randomBytes(), "MAC");
					lastGenerationDate = System.currentTimeMillis();
				}
				return cookieMacKey;
			} finally {
				cookieMacKeyLock.writeLock().unlock();
			}

		}

		private long lastGenerationDate = System.currentTimeMillis();

		Mac hmac;

		private byte[] generateCookie(ClientHello clientHello) {
			try {
				// Cookie = HMAC(Secret, Client-IP, Client-Parameters)
				Mac hmac = getHMAC();
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
			} catch (GeneralSecurityException e) {
				throw new DtlsHandshakeException("Cannot compute cookie for peer", AlertDescription.INTERNAL_ERROR,
						AlertLevel.FATAL, clientHello.getPeer(), e);
			}
		}

		private Mac getHMAC() throws NoSuchAlgorithmException {
			if (hmac == null) {
				hmac = Mac.getInstance("HmacSHA256");
			}
			return hmac;
		}
	}

}
