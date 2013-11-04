/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation, Markus Keller - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.text.SimpleDateFormat;
import java.util.*;
import junit.framework.TestCase;

public class TestTimeoutDump {
	static String fgName;
	static int fgTimeoutSeconds;
	static Thread fgTimer;

	public static void setUp(TestCase testCase, int timeoutSeconds) {
		fgName = testCase.getClass().getName() + "#" + testCase.getName();
		fgTimeoutSeconds = timeoutSeconds;
		fgTimer = new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(fgTimeoutSeconds * 1000);
				} catch (InterruptedException e) {
					fgTimer = null;
					return;
				}
				System.err.println("Thread dump " + fgName + " at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(new Date()) + ":");
				Map<Thread, StackTraceElement[]> s = Thread.getAllStackTraces();
				for (Map.Entry<Thread, StackTraceElement[]> entry : s.entrySet()) {
					String name = entry.getKey().getName();
					StackTraceElement[] stack = entry.getValue();
					Exception exception = new Exception(name);
					exception.setStackTrace(stack);
					exception.printStackTrace();
				}
				System.err.flush();
				fgTimer = null;
			}
		};
		fgTimer.start();
	}

	public static void tearDown() {
		if (fgTimer != null) {
			fgTimer.interrupt();
		}
	}
}