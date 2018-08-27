/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.junit.Test;

public class RequirementParsingTest {

	@Test
	public void testIRequirementType() {
		String namespace = "osgi.ee";
		String ldap = "(&(osgi.ee=JavaSE)(version=1.7))";
		String match = "providedCapabilities.exists(pc | pc.namespace == '" + namespace + "' && pc.attributes ~= filter('" + ldap + "'))";

		IExpression expr = ExpressionUtil.parse(match);
		IMatchExpression<IInstallableUnit> matchExpr = ExpressionUtil.getFactory().matchExpression(expr);

		IRequirement req = MetadataFactory.createRequirement(matchExpr, (IMatchExpression<IInstallableUnit>) null, 0, 1, false);
		assertFalse(req instanceof IRequiredCapability);
	}

	@Test
	public void testIRquiredCapabilityType() {
		String namespace = "java.package";
		String name = "org.example";
		VersionRange range = VersionRange.create("[1, 2)");

		IRequirement req = null;

		req = MetadataFactory.createRequirement(namespace, name, range, (IMatchExpression<IInstallableUnit>) null, 0, 1, false);
		assertTrue(req instanceof IRequiredCapability);

		req = MetadataFactory.createRequirement(namespace, name, range, (IMatchExpression<IInstallableUnit>) null, 0, 1, false, null);
		assertTrue(req instanceof IRequiredCapability);

		req = MetadataFactory.createRequirement(namespace, name, range, (IMatchExpression<IInstallableUnit>) null, false, false);
		assertTrue(req instanceof IRequiredCapability);

		req = MetadataFactory.createRequirement(namespace, name, range, (String) null, false, false, false);
		assertTrue(req instanceof IRequiredCapability);
	}

	@Test
	public void testIRquiredCapabilityDetection() {
		String namespace = "java.package";
		String name = "org.example";
		VersionRange range = VersionRange.create("[1, 2)");

		IMatchExpression<IInstallableUnit> matchExpr = RequiredCapability.createMatchExpressionFromRange(namespace, name, range);

		IRequirement req = null;

		req = MetadataFactory.createRequirement(matchExpr, (IMatchExpression<IInstallableUnit>) null, 0, 1, false);
		assertTrue(req instanceof IRequiredCapability);

		req = MetadataFactory.createRequirement(matchExpr, (IMatchExpression<IInstallableUnit>) null, 0, 1, false, null);
		assertTrue(req instanceof IRequiredCapability);
	}
}
