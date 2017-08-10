package org.eclipse.californium.benchmark;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class LoadAverage {

	public static void main(String[] args) {
		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		System.out.println("Load Average: " + os.getSystemLoadAverage());
		System.out.println("Available Processor: " + os.getAvailableProcessors());

		Thread t1 = new Thread(new Runnable() {

			@Override
			public void run() {
				// Make CPU spin for a bit
				for (double x = 0; x < Double.MAX_VALUE; x++) {
					for (double y = 0; y < Double.MAX_VALUE; y++) {
						double z = y * x;
					}
				}
			}
		});
		t1.start();

		Thread t2 = new Thread(new Runnable() {

			@Override
			public void run() {
				// Make CPU spin for a bit
				for (double x = 0; x < Double.MAX_VALUE; x++) {
					for (double y = 0; y < Double.MAX_VALUE; y++) {
						double z = y * x;
					}
				}
			}
		});
		t2.start();

		try {
			t2.join();
			t1.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Load Average: " + os.getSystemLoadAverage());
	}
}
