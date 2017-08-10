/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.californium.benchmark;

import java.util.Random;

import org.eclipse.californium.scandium.util.ByteArrayUtils;
import org.eclipse.leshan.util.Hex;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class ToHexBenchmark {

	@State(Scope.Thread)
	public static class MyState {

		public byte[] payload = new byte[256];
		public Random r = new Random();

		@Setup(Level.Invocation)
		public void prepare() {
			r.nextBytes(payload);
		}
	}

	@Benchmark
	public void testScandiumToHex(MyState state) {
		ByteArrayUtils.toHexString(state.payload);
	}

	@Benchmark
	public void testApacheToHex(MyState state) {
		Hex.encodeHex(state.payload).toString();
	}

	@Benchmark
	public void testStackOverFlowToHex(MyState state) {
		bytesToHex(state.payload);
	}

	@Benchmark
	public void testBoaksToHex(MyState state) {
		bytesToHex(state.payload);
	}

	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	private final static char[] BIN_TO_HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	public static String boaksBytesToHex(byte[] byteArray) {
		char[] bytesHexadecimal = new char[byteArray.length * 2];
		for (int src = 0, dest = 0; src < byteArray.length; src++) {
			int value = byteArray[src] & 0xFF;
			bytesHexadecimal[dest++] = BIN_TO_HEX_ARRAY[value >>> 4];
			bytesHexadecimal[dest++] = BIN_TO_HEX_ARRAY[value & 0x0F];
		}
		return new String(bytesHexadecimal);
	}
}
