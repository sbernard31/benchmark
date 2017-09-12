package org.eclipse.californium.benchmark;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import eu.javaspecialists.tjsn.concurrency.stripedexecutor.StripedExecutorService;
import eu.javaspecialists.tjsn.concurrency.stripedexecutor.StripedRunnable;

public class StripedExecutorBenchmark {

	@State(Scope.Benchmark)
	public static class FixedState {

		@Setup(Level.Trial)
		public void doSetup() throws InterruptedException {
			executor = Executors.newFixedThreadPool(10);
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			executor.shutdownNow();
		}

		public ExecutorService executor;
		public Runnable runnable;

		@Setup(Level.Invocation)
		public void prepare() {
			runnable = new Runnable() {
				@Override
				public void run() {
				}
			};
		}
	}

	@State(Scope.Benchmark)
	public static class SingleState {

		@Setup(Level.Trial)
		public void doSetup() throws InterruptedException {
			executor = Executors.newSingleThreadExecutor();
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			executor.shutdownNow();
		}

		public ExecutorService executor;
		public Runnable runnable;

		@Setup(Level.Invocation)
		public void prepare() {
			runnable = new Runnable() {
				@Override
				public void run() {
				}
			};
		}
	}

	@State(Scope.Benchmark)
	public static class PatchedStripedState {

		@Setup(Level.Trial)
		public void doSetup() throws InterruptedException {
			executor = new org.eclipse.californium.benchmark.StripedExecutorService();
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			executor.shutdownNow();
		}

		public ExecutorService executor;
		public Runnable runnable;
		public Random r = new Random();

		@Setup(Level.Invocation)
		public void prepare() {
			runnable = new StripedRunnable() {
				Object stripe = r.nextInt(10);
				@Override
				public Object getStripe() {
					return stripe;
				}

				@Override
				public void run() {
				}
			};
		}
	}

	@State(Scope.Benchmark)
	public static class StripedState {

		@Setup(Level.Trial)
		public void doSetup() throws InterruptedException {
			executor = new StripedExecutorService();
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			executor.shutdownNow();
		}

		public ExecutorService executor;
		public Runnable runnable;
		public Random r = new Random();

		@Setup(Level.Invocation)
		public void prepare() {
			runnable = new StripedRunnable() {
				Object stripe = r.nextInt(10);
				
				@Override
				public Object getStripe() {
					return stripe;
				}

				@Override
				public void run() {
				}
			};
		}
	}

	@Benchmark
	public void testSingleExecutor(SingleState state) {
		state.executor.execute(state.runnable);
	}

	@Benchmark
	public void testFixedExecutor(FixedState state) {
		state.executor.execute(state.runnable);
	}

	@Benchmark
	public void testStripedExecutor(StripedState state) {
		state.executor.execute(state.runnable);
	}

	@Benchmark
	public void testPatchedStripedExecutor(PatchedStripedState state) {
		state.executor.execute(state.runnable);
	}
}
