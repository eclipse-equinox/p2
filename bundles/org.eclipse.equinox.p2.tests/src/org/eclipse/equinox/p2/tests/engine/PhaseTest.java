/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import junit.framework.TestCase;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.helpers.MultiStatus;
import org.eclipse.equinox.p2.engine.*;

/**
 * Simple test of the engine API.
 */
public class PhaseTest extends TestCase {
	public PhaseTest(String name) {
		super(name);
	}

	public PhaseTest() {
		super("");
	}

	public void testNullPhaseId() {
		try {
			new TestPhase(null, 1, "xyz");
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testEmptyPhaseId() {
		try {
			new TestPhase("", 1, "xyz");
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testNegativeWeight() {
		try {
			new TestPhase("xyz", -1, "xyz");
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testZeroWeight() {
		try {
			new TestPhase("xyz", 0, "xyz");
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testNullPhaseName() {
		try {
			new TestPhase("xyz", 1, null);
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testEmptyPhaseName() {
		try {
			new TestPhase("xyz", 1, "");
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public static class TestPhase extends Phase {

		protected TestPhase(String phaseId, int weight, String phaseName) {
			super(phaseId, weight, phaseName);
		}

		protected void perform(MultiStatus status, EngineSession session, Profile profile, Operand[] deltas, IProgressMonitor monitor) {
			//empty
		}
	}
}
