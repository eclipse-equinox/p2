/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.util.Enumeration;
import java.util.Vector;
import junit.framework.*;

public class ReconcilerTestSuite extends TestSuite {

	private Test INITIALIZE;
	private Test CLEANUP;
	private String propertyToPlatformArchive;

	public ReconcilerTestSuite() {
		super();
		INITIALIZE = getInitializationTest();
		CLEANUP = getCleanUpTest();
	}

	public ReconcilerTestSuite(String propertyToPlatformArchive) {
		super();
		this.propertyToPlatformArchive = propertyToPlatformArchive;
		INITIALIZE = getInitializationTest();
		CLEANUP = getCleanUpTest();
	}

	protected String getPlatformArchive() {
		return propertyToPlatformArchive;
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestSuite#tests()
	 */
	public Enumeration tests() {
		Vector result = new Vector();
		result.add(INITIALIZE);
		for (Enumeration e = super.tests(); e.hasMoreElements();)
			result.add(e.nextElement());
		result.add(CLEANUP);
		return result.elements();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestSuite#testCount()
	 */
	public int testCount() {
		return super.testCount() + 2;
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestSuite#testAt(int)
	 */
	public Test testAt(int index) {
		if (index == 0)
			return INITIALIZE;
		if (index == testCount() - 1)
			return CLEANUP;
		return super.testAt(index - 1);
	}

	public Test getInitializationTest() {
		return new AbstractReconcilerTest("initialize", propertyToPlatformArchive);
	}

	public Test getCleanUpTest() {
		return new AbstractReconcilerTest("cleanup");
	}

	/**
	 * Runs the tests and collects their result in a TestResult.
	 * 
	 * We must override this method in order to run against JUnit4 which doesn't
	 * invoke tests().
	 */
	public void run(TestResult result) {
		for (Enumeration e = tests(); e.hasMoreElements();) {
			Test each = (Test) e.nextElement();
			if (result.shouldStop())
				break;
			runTest(each, result);
		}
	}

}
