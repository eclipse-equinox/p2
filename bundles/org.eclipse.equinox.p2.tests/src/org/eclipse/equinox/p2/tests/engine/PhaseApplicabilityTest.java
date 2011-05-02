/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.engine.phases.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Simple test of the engine API.
 */
public class PhaseApplicabilityTest extends AbstractProvisioningTest {

	public PhaseApplicabilityTest(String name) {
		super(name);
	}

	public PhaseApplicabilityTest() {
		super("");
	}

	public void testCollectPhase() {

		IInstallableUnit iuXv1 = createIU("iuX", Version.create("1.0.0"));
		IInstallableUnit iuXv2 = createIU("iuX", Version.create("2.0.0"));

		Collect collectPhase = new Collect(1) {
			protected boolean isApplicable(InstallableUnitOperand op) {
				return super.isApplicable(op);
			}
		};
		assertFalse(collectPhase.isApplicable(new InstallableUnitOperand(iuXv1, null)));
		assertFalse(collectPhase.isApplicable(new InstallableUnitOperand(iuXv1, iuXv1)));
		assertTrue(collectPhase.isApplicable(new InstallableUnitOperand(null, iuXv1)));
		assertTrue(collectPhase.isApplicable(new InstallableUnitOperand(iuXv1, iuXv2)));
	}

	public void testSizingPhase() {

		IInstallableUnit iuXv1 = createIU("iuX", Version.create("1.0.0"));
		IInstallableUnit iuXv2 = createIU("iuX", Version.create("2.0.0"));

		Sizing sizingPhase = new Sizing(1) {
			protected boolean isApplicable(InstallableUnitOperand op) {
				return super.isApplicable(op);
			}
		};
		assertFalse(sizingPhase.isApplicable(new InstallableUnitOperand(iuXv1, null)));
		assertFalse(sizingPhase.isApplicable(new InstallableUnitOperand(iuXv1, iuXv1)));
		assertTrue(sizingPhase.isApplicable(new InstallableUnitOperand(null, iuXv1)));
		assertTrue(sizingPhase.isApplicable(new InstallableUnitOperand(iuXv1, iuXv2)));
	}

	public void testUnconfigurePhase() {

		IInstallableUnit iuXv1 = createIU("iuX", Version.create("1.0.0"));
		IInstallableUnit iuXv2 = createIU("iuX", Version.create("2.0.0"));

		Unconfigure unconfigurePhase = new Unconfigure(1) {
			protected boolean isApplicable(InstallableUnitOperand op) {
				return super.isApplicable(op);
			}
		};
		assertTrue(unconfigurePhase.isApplicable(new InstallableUnitOperand(iuXv1, null)));
		assertTrue(unconfigurePhase.isApplicable(new InstallableUnitOperand(iuXv1, iuXv1)));
		assertFalse(unconfigurePhase.isApplicable(new InstallableUnitOperand(null, iuXv1)));
		assertTrue(unconfigurePhase.isApplicable(new InstallableUnitOperand(iuXv1, iuXv2)));
	}

	public void testUninstallPhase() {

		IInstallableUnit iuXv1 = createIU("iuX", Version.create("1.0.0"));
		IInstallableUnit iuXv2 = createIU("iuX", Version.create("2.0.0"));

		Uninstall uninstallPhase = new Uninstall(1) {
			protected boolean isApplicable(InstallableUnitOperand op) {
				return super.isApplicable(op);
			}
		};
		assertTrue(uninstallPhase.isApplicable(new InstallableUnitOperand(iuXv1, null)));
		assertFalse(uninstallPhase.isApplicable(new InstallableUnitOperand(iuXv1, iuXv1)));
		assertFalse(uninstallPhase.isApplicable(new InstallableUnitOperand(null, iuXv1)));
		assertTrue(uninstallPhase.isApplicable(new InstallableUnitOperand(iuXv1, iuXv2)));
	}

	public void testInstallPhase() {

		IInstallableUnit iuXv1 = createIU("iuX", Version.create("1.0.0"));
		IInstallableUnit iuXv2 = createIU("iuX", Version.create("2.0.0"));

		Install installPhase = new Install(1) {
			protected boolean isApplicable(InstallableUnitOperand op) {
				return super.isApplicable(op);
			}
		};
		assertFalse(installPhase.isApplicable(new InstallableUnitOperand(iuXv1, null)));
		assertFalse(installPhase.isApplicable(new InstallableUnitOperand(iuXv1, iuXv1)));
		assertTrue(installPhase.isApplicable(new InstallableUnitOperand(null, iuXv1)));
		assertTrue(installPhase.isApplicable(new InstallableUnitOperand(iuXv1, iuXv2)));
	}

	public void testConfigurePhase() {

		IInstallableUnit iuXv1 = createIU("iuX", Version.create("1.0.0"));
		IInstallableUnit iuXv2 = createIU("iuX", Version.create("2.0.0"));

		Configure configurePhase = new Configure(1) {
			protected boolean isApplicable(InstallableUnitOperand op) {
				return super.isApplicable(op);
			}
		};
		assertFalse(configurePhase.isApplicable(new InstallableUnitOperand(iuXv1, null)));
		assertTrue(configurePhase.isApplicable(new InstallableUnitOperand(iuXv1, iuXv1)));
		assertTrue(configurePhase.isApplicable(new InstallableUnitOperand(null, iuXv1)));
		assertTrue(configurePhase.isApplicable(new InstallableUnitOperand(iuXv1, iuXv2)));
	}
}
