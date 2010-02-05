/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.util.Arrays;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorSelector.MirrorInfo;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * 
 */
public class MirrorSelectorTest extends AbstractProvisioningTest {
	public void testSorting() {
		MirrorInfo one = new MirrorInfo("one", 0);
		MirrorInfo two = new MirrorInfo("two", 1);
		MirrorInfo three = new MirrorInfo("three", 2);
		MirrorInfo four = new MirrorInfo("four", 3);

		MirrorInfo[] sorted = new MirrorInfo[] {one, two, three, four};
		Arrays.sort(sorted);
		assertOrder("1.0", sorted, one, two, three, four);

		//make sure order on initial rank is correct
		sorted = new MirrorInfo[] {four, two, three, one};
		Arrays.sort(sorted);
		assertOrder("1.1", sorted, one, two, three, four);

		//increase the failure count and ensure order is correctly updated
		one.incrementFailureCount();
		one.incrementFailureCount();
		two.incrementFailureCount();
		Arrays.sort(sorted);
		assertOrder("1.2", sorted, three, four, two, one);

		//go back to default order
		one = new MirrorInfo("one", 0);
		two = new MirrorInfo("two", 1);
		three = new MirrorInfo("three", 2);
		four = new MirrorInfo("four", 3);
		sorted = new MirrorInfo[] {one, two, three, four};

		//set bit rate and ensure order is updated
		one.setBytesPerSecond(100L);
		two.setBytesPerSecond(400L);
		three.setBytesPerSecond(800L);
		four.setBytesPerSecond(600L);
		Arrays.sort(sorted);
		assertOrder("1.3", sorted, three, four, two, one);

		//introduce a failure and ensure order is updated but that
		//the failure isn't put last
		three.incrementFailureCount();
		Arrays.sort(sorted);
		assertOrder("1.4", sorted, four, two, three, one);
	}

	private void assertOrder(String message, MirrorInfo[] sorted, MirrorInfo one, MirrorInfo two, MirrorInfo three, MirrorInfo four) {
		assertTrue(message + ".1", sorted[0] == one);
		assertTrue(message + ".1", sorted[1] == two);
		assertTrue(message + ".1", sorted[2] == three);
		assertTrue(message + ".1", sorted[3] == four);
	}
}
