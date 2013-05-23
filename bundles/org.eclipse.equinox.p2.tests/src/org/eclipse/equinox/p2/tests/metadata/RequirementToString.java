/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *     Ericsson AB - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

//These tests are not here to force the toString of a requirement.
//They are here to ensure that the toString of a requirement does not become unreadable because this ends up being presented in the p2 UI and in the tycho build.
//As such if there are better ways present all this information, by all mean change the test.
public class RequirementToString extends AbstractProvisioningTest {
	public void testRequirementWithEmptyRange() {
		IRequirement req = MetadataFactory.createRequirement("expectedNameSpace", "expectedName", VersionRange.emptyRange, null, false, false);
		assertEquals("expectedNameSpace expectedName 0.0.0", req.toString());
	}

	public void testStandardRequirement() {
		IRequirement req = MetadataFactory.createRequirement("expectedNameSpace", "expectedName", new VersionRange("[1.0.0, 2.0.0)"), null, false, false);
		assertEquals("expectedNameSpace expectedName [1.0.0,2.0.0)", req.toString());
	}

	public void testFancyRequirement() {
		Object[] expressionParameters = new Object[] {"expectedId1, expectedVersion1", "expectedId2, expectedVersion2"};
		IMatchExpression<IInstallableUnit> iuMatcher = ExpressionUtil.getFactory().<IInstallableUnit> matchExpression(ExpressionUtil.parse("(id == $0 && version == $1) || (id == $2 && version == $3)"), expressionParameters);
		IRequirement req = MetadataFactory.createRequirement(iuMatcher, null, 1, 1, true);
		assertEquals("id == $0 && version == $1 || id == $2 && version == $3 expectedId1, expectedVersion1 expectedId2, expectedVersion2", req.toString().trim());
	}
}
