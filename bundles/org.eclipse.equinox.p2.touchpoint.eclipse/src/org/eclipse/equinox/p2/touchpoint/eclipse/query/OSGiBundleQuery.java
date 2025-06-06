/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc. - converted into expression based query
 *******************************************************************************/
package org.eclipse.equinox.p2.touchpoint.eclipse.query;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.ExpressionMatchQuery;

/**
 * A query matching every {@link IInstallableUnit} that describes an OSGi bundle.
 * @since 2.0
 */
public final class OSGiBundleQuery extends ExpressionMatchQuery<IInstallableUnit> {

	private static final IMatchExpression<IInstallableUnit> bundleTest = ExpressionUtil.getFactory().matchExpression(ExpressionUtil.parse("providedCapabilities.exists(p | p.namespace == 'osgi.bundle')")); //$NON-NLS-1$

	public OSGiBundleQuery() {
		super(IInstallableUnit.class, bundleTest);
	}

	/**
	 * Test if the {@link IInstallableUnit} describes an OSGi bundle.
	 * @param iu the element being tested.
	 * @return <code>true</code> if the parameter describes an OSGi bundle.
	 */
	public static boolean isOSGiBundle(IInstallableUnit iu) {
		return bundleTest.isMatch(iu);
	}
}