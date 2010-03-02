/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *      Sonatype Inc - Refactoring
 *******************************************************************************/
package org.eclipse.equinox.p2.planner;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * Helper method to decide on the way the installable units are being included.
 * @since 2.0
 */
public class ProfileInclusionRules {
	private ProfileInclusionRules() {
		//Can't instantiate profile inclusion rules
	}

	/**
	 * Provide an inclusion rule to for the installation of the given installable unit
	 * @param iu the iu to be installed.
	 * @return an opaque token to be passed to the {@link IProfileChangeRequest#setInstallableUnitInclusionRules(IInstallableUnit, String)}
	 */
	public static String createStrictInclusionRule(IInstallableUnit iu) {
		return "STRICT"; //$NON-NLS-1$
	}

	/**
	 * Provide an inclusion rule to optionally install the installable unit being specified
	 * @param iu the iu to be installed.
	 * @return an opaque token to be passed to the {@link IProfileChangeRequest#setInstallableUnitInclusionRules(IInstallableUnit, String)}
	 */
	public static String createOptionalInclusionRule(IInstallableUnit iu) {
		return "OPTIONAL"; //$NON-NLS-1$
	}
}
