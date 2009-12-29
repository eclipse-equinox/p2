/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ql;

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

/**
 * An ICapabilityIndex implementation stores instances of {@link IInstallableUnit} so
 * that they are easily retrievable using instances of {@link IRequirement}.
 */
public interface ICapabilityIndex {
	/**
	 * Returns an iterator that will yield all {@link IInstallableUnit} instances that
	 * satisfies at least one of the <code>requirements</code>.
	 * @param requirements An iterator over {@link IRequirement} instances.
	 * @return An iterator over {@link IInstallableUnit} instances. Possibly empty but never <code>null</code>
	 */
	Iterator<IInstallableUnit> satisfiesAny(Iterator<IRequirement> requirements);

	/**
	 * Returns an iterator that will yield all {@link IInstallableUnit} instances that
	 * satisfies all of the <code>requirements</code>.
	 * @param requirements An iterator over {@link IRequirement} instances.
	 * @return An iterator over {@link IInstallableUnit} instances. Possibly empty but never <code>null</code>
	 */
	Iterator<IInstallableUnit> satisfiesAll(Iterator<IRequirement> requirements);
}
