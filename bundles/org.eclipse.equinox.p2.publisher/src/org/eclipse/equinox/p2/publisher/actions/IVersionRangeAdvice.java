/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 *
 * This program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import java.util.Optional;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public interface IVersionRangeAdvice extends IPublisherAdvice {

	public static final String NS_FEATURE = PublisherHelper.NAMESPACE_ECLIPSE_TYPE + "." //$NON-NLS-1$
			+ PublisherHelper.TYPE_ECLIPSE_FEATURE;
	public static final String NS_IU = IInstallableUnit.NAMESPACE_IU_ID;

	/**
	 * Returns the {@link VersionRange} for the given id in the given namespace.
	 *
	 * @param namespace the namespace in which to look for advice
	 * @param id        the id for the item in the given namespace
	 * @return an {@link Optional} describing the {@link VersionRange} found.
	 */
	public Optional<VersionRange> getVersionRange(String namespace, String id);

}
