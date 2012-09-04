/*******************************************************************************
 *  Copyright (c) 2008, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.util.List;

/**
 * An installable unit patch is an installable unit that alters the required capabilities of another
 * installable unit. 
 * <p>
 * Instances of this class are handle objects and do not necessarily
 * reflect entities that exist in any particular profile or repository. These handle 
 * objects can be created using {@link MetadataFactory}.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 * @see MetadataFactory#createInstallableUnitPatch(org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitPatchDescription)
 */
public interface IInstallableUnitPatch extends IInstallableUnit {
	/**
	 * The applicability scope of a patch describes the installable units that this
	 * patch should be applied to. Specifically, this patch will be applied to all installable
	 * units that satisfy all of the required capabilities in one or more of the given 
	 * required capability arrays. 
	 * <p>
	 * The returned two-dimensional array can be considered
	 * as a boolean expression, where items in the inner array are connected by
	 * AND operators, and each of the arrays are separated by OR operators. For example
	 * a scope of [[r1, r2, r3], [r4, r5]] will match any unit whose provided capabilities
	 * satisfy the expression ((r1 ^ r2 ^ r3) | (r4 ^ r5)).
	 * @noreference This method is not intended to be referenced by clients.
	 */
	IRequirement[][] getApplicabilityScope();

	/**
	 * Returns the requirement changes imposed by the patch.
	 * @return The patch requirement changes.
	 */
	List<IRequirementChange> getRequirementsChange();

	/**
	 * Returns the required capability that defines the lifecycle of this patch. The
	 * patch will be installed into a profile if and only if the lifecycle capability
	 * is satisfied by some IU in the profile. If a future provisioning operation causes
	 * the requirement to no longer be satisfied, the patch will be uninstalled.
	 */
	IRequirement getLifeCycle();
}
