/******************************************************************************* 
* Copyright (c) 2009, 2012 IBM and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   IBM - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;

/**
 * A requirement represents some external constraint on an {@link IInstallableUnit}.
 * Each requirement represents something an {@link IInstallableUnit} needs that
 * it expects to be provided by another {@link IInstallableUnit}. Requirements are
 * entirely generic, and are intended to be capable of representing anything that
 * an {@link IInstallableUnit} may need either at install time, or at runtime.
 * <p>
 * Instances of this class are handle objects and do not necessarily
 * reflect entities that exist in any particular profile or repository. These handle 
 * objects can be created using {@link MetadataFactory}.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 * @see IProvidedCapability
 * @see MetadataFactory#createRequirement(String, String, VersionRange, String, boolean, boolean, boolean)
 */
public interface IRequirement {

	/**
	 * Returns the minimum cardinality of the requirement. That is, the minimum
	 * number of capabilities that must be provided that match this requirement before
	 * this requirement is considered fully satisfied.  A minimum cardinality of 0 indicates 
	 * that the requirement is optional.
	 * 
	 * @return the minimum cardinality of this requirement
	 */
	int getMin();

	/**
	 * Returns the maximum cardinality of the requirement. That is, the maximum
	 * number of capabilities that are permitted to be present that satisfy this requirement.
	 * A maximum cardinality of 0 indicates that there must <em>not</em> be
	 * any installable unit in the system that satisfies this requirement.
	 * 
	 * @return the maximum cardinality of this requirement
	 */
	int getMax();

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @since 2.0
	 */
	IMatchExpression<IInstallableUnit> getFilter();

	/**
	 * Returns a boolean match expression that will return true for any
	 * {@link IInstallableUnit} that matches the requirement.
	 * @return A boolean match expression for installable unit matching.
	 */
	IMatchExpression<IInstallableUnit> getMatches();

	/**
	 * Returns whether the provided capabilities of the given installable unit satisfy
	 * this requirement.
	 * 
	 * @param iu the installable unit to check for matching capabilities
	 * @return <code>true</code> if the given installable unit satisfies this
	 * requirement, and <code>false</code> otherwise.
	 */
	boolean isMatch(IInstallableUnit iu);

	/**
	 * Returns whether this requirement should cause extra installable units
	 * to be installed in order to satisfy it. 
	 * @return <code>true</code> if additional installable units should be installed
	 * to satisfy this requirement, and <code>false</code> otherwise
	 */
	boolean isGreedy();

	/**
	 * Returns a textual description of this requirement.
	 * 
	 * @return a textual description of this requirement
	 */
	String getDescription();

}