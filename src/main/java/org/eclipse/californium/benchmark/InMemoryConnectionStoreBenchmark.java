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

import java.net.InetSocketAddress;
import java.util.Random;

import org.eclipse.californium.scandium.dtls.Connection;
import org.eclipse.californium.scandium.dtls.InMemoryConnectionStore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class InMemoryConnectionStoreBenchmark {

    static final int nbConnection = 100_000;

    @State(Scope.Benchmark)
    public static class OriginalState {

        @Setup(Level.Trial)
        public void doSetup() throws InterruptedException {
            store = new InMemoryConnectionStore(100_000, 1);

            for (int i = 0; i < nbConnection; i++) {
                Connection connection = new Connection(InetSocketAddress.createUnresolved("host" + i, 50000));
                store.put(connection);
            }
            r = new Random();
            Thread.sleep(1000);
        }

        public InMemoryConnectionStore store;
        public Random r;
        public InetSocketAddress socketAddr;
        public Connection connection;

        @Setup(Level.Invocation)
        public void prepare() {
            int i = r.nextInt(nbConnection);
            socketAddr = InetSocketAddress.createUnresolved("host" + i, 50000);
            connection = new Connection(InetSocketAddress.createUnresolved("host"+(nbConnection+r.nextInt(200_000)), 50000));
        }
    }
    
    @State(Scope.Benchmark)
    public static class EnhancedState {

        @Setup(Level.Trial)
        public void doSetup() throws InterruptedException {
            store = new  org.eclipse.californium.benchmark.InMemoryConnectionStore(100_000, 1);

            for (int i = 0; i < nbConnection; i++) {
                Connection connection = new Connection(InetSocketAddress.createUnresolved("host" + i, 50000));
                store.put(connection);
            }
            r = new Random();
            Thread.sleep(1000);
        }

        public org.eclipse.californium.benchmark.InMemoryConnectionStore store;
        public Random r;
        public InetSocketAddress socketAddr;
        public Connection connection;

        @Setup(Level.Invocation)
        public void prepare() {
            int i = r.nextInt(nbConnection);
            socketAddr = InetSocketAddress.createUnresolved("host" + i, 50000);
            connection = new Connection(InetSocketAddress.createUnresolved("host"+(nbConnection+r.nextInt(200_000)), 50000));
        }
    }

    @Benchmark
    public void testOriginalState(OriginalState state) {
        state.store.get(state.socketAddr);
    }
    
    @Benchmark
    public void testEnhanced(EnhancedState state) {
    	state.store.get(state.socketAddr);
    }
}
