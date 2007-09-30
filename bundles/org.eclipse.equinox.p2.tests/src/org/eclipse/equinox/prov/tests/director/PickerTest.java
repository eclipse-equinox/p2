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
package org.eclipse.equinox.prov.tests.director;

import junit.framework.TestCase;
import org.eclipse.equinox.internal.prov.director.Picker;
import org.eclipse.equinox.prov.metadata.*;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * Testing of the {@link Picker} class.
 */
public class PickerTest extends TestCase {
	InstallableUnit unitVersion5;
	private Picker picker;

	public PickerTest() {
		super(""); //$NON-NLS-1$
	}

	public PickerTest(String name) {
		super(name);
	}

	private RequiredCapability[] createRequiredCapabilities(String namespace, String name, VersionRange range, String filter) {
		return new RequiredCapability[] {new RequiredCapability(namespace, name, range, filter, false, false)};
	}

	protected void setUp() throws Exception {
		super.setUp();
		Version version = new Version(5, 0, 0);

		//create some sample IUs to be available for the picker
		unitVersion5 = new InstallableUnit();
		unitVersion5.setId("required");
		unitVersion5.setVersion(version);
		unitVersion5.setCapabilities(new ProvidedCapability[] {new ProvidedCapability("test.capability", "test", version)});

		InstallableUnit[] units = new InstallableUnit[] {unitVersion5};
		picker = new Picker(units, null);

	}

	/**
	 * Tests picking an IU that requires a capability, and the available
	 * provided capability is above the required capability's version range.
	 */
	public void testRequiredBelowVersionRange() {

		//an IU whose required capability falls outside available range
		RequiredCapability[] required = createRequiredCapabilities("test.capability", "test", new VersionRange("[2.0,5.0)"), null);

		IInstallableUnit[][] result = picker.findInstallableUnit(null, null, required, false);
		assertEquals("1.0", 0, result[0].length + result[1].length);
	}

	/**
	 * Tests picking an IU that requires a capability, and the available
	 * provided capability is above the required capability's version range.
	 */
	public void testRequiredWithinVersionRange() {

		//in middle of range
		RequiredCapability[] required = createRequiredCapabilities("test.capability", "test", new VersionRange("[2.0,6.0)"), null);
		IInstallableUnit[][] result = picker.findInstallableUnit(null, null, required, false);
		assertEquals("1.0", 1, result.length);
		assertEquals("1.1", unitVersion5, result[0]);

		//on lower bound
		required = createRequiredCapabilities("test.capability", "test", new VersionRange("[5.0,6.0)"), null);
		result = picker.findInstallableUnit(null, null, required, false);
		assertEquals("1.0", 1, result[0].length + result[1].length);
		assertEquals("1.1", unitVersion5, result[1][0]);

		//on upper bound
		required = createRequiredCapabilities("test.capability", "test", new VersionRange("[1.0,5.0]"), null);
		result = picker.findInstallableUnit(null, null, required, false);
		assertEquals("1.0", 1, result[0].length + result[1].length);
		assertEquals("1.1", unitVersion5, result[1][0]);
	}

	/**
	 * Tests picking an IU that requires a capability, and the available
	 * provided capability is above the required capability's version range.
	 */
	public void testRequiredAboveVersionRange() {

		//an IU whose required capability falls outside available range
		RequiredCapability[] required = createRequiredCapabilities("test.capability", "test", new VersionRange("[5.1,6.0)"), null);

		IInstallableUnit[][] result = picker.findInstallableUnit(null, null, required, false);
		assertEquals("1.0", 0, result[0].length + result[1].length);
	}

}
