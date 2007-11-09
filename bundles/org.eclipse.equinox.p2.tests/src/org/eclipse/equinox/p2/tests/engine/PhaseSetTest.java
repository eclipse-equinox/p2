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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.ResolvedInstallableUnit;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Simple test of the engine API.
 */
public class PhaseSetTest extends AbstractProvisioningTest {
	public PhaseSetTest(String name) {
		super(name);
	}

	public PhaseSetTest() {
		super("");
	}

	public void testNullPhases() {
		try {
			new PhaseSet(null) {
				// empty PhaseSet
			};
		} catch (IllegalArgumentException exepcted) {
			return;
		}
		fail();
	}

	public void testEmptyPhases() {
		Profile profile = new Profile("test");
		PhaseSet phaseSet = new PhaseSet(new Phase[] {}) {
			// empty PhaseSet
		};
		Operand op = new Operand(new ResolvedInstallableUnit(createIU("iu")), null);
		Operand[] operands = new Operand[] {op};

		IStatus result = phaseSet.perform(new EngineSession(profile), profile, operands, new NullProgressMonitor());
		assertTrue(result.isOK());
	}
}
