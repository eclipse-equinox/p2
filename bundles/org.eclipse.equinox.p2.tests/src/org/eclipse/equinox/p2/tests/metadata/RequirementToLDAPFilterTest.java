/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.junit.Test;

/**
 * Tests {@link IMatchExpression#toLDAPString(StringBuilder)} works correctly
 * for {@link RequiredCapability} match expressions covering every predefined
 * version-range template.
 */
public class RequirementToLDAPFilterTest {

	private static final String NS = "org.eclipse.equinox.p2.iu"; //$NON-NLS-1$
	private static final String NAME = "my.bundle"; //$NON-NLS-1$

	private static String ldap(String namespace, String name, VersionRange range) {
		IMatchExpression<IInstallableUnit> expr = RequiredCapability.createMatchExpressionFromRange(namespace, name, range);
		StringBuilder buf = new StringBuilder();
		expr.toLDAPString(buf);
		return buf.toString();
	}

	// ALL  (null / emptyRange -> any version)
	@Test
	public void testAll_nullRange() {
		assertEquals("(&(name=my.bundle)(namespace=org.eclipse.equinox.p2.iu))", ldap(NS, NAME, null)); //$NON-NLS-1$
	}

	@Test
	public void testAll_emptyRange() {
		assertEquals("(&(name=my.bundle)(namespace=org.eclipse.equinox.p2.iu))", ldap(NS, NAME, VersionRange.emptyRange)); //$NON-NLS-1$
	}

	// STRICT  ([1.0.0, 1.0.0])
	@Test
	public void testStrict() {
		assertEquals(
				"(&(name=my.bundle)(namespace=org.eclipse.equinox.p2.iu)(version=1.0.0))", //$NON-NLS-1$
				ldap(NS, NAME, VersionRange.create("[1.0.0,1.0.0]"))); //$NON-NLS-1$
	}

	// OPEN_I  ([1.0.0, ∞)  - inclusive lower bound)
	@Test
	public void testOpenI() {
		assertEquals(
				"(&(name=my.bundle)(namespace=org.eclipse.equinox.p2.iu)(version>=1.0.0))", //$NON-NLS-1$
				ldap(NS, NAME, VersionRange.create("1.0.0"))); //$NON-NLS-1$
	}

	// OPEN_N  ((1.0.0, ∞)  - exclusive lower bound)
	// RFC 4515 has no '>' operator; represented as (!(version<=X))
	@Test
	public void testOpenN() {
		assertEquals(
				"(&(name=my.bundle)(namespace=org.eclipse.equinox.p2.iu)(!(version<=1.0.0)))", //$NON-NLS-1$
				ldap(NS, NAME, VersionRange.create("(1.0.0,)"))); //$NON-NLS-1$
	}

	// CLOSED_II  ([1.0.0, 2.0.0])
	@Test
	public void testClosedII() {
		assertEquals(
				"(&(name=my.bundle)(namespace=org.eclipse.equinox.p2.iu)(version>=1.0.0)(version<=2.0.0))", //$NON-NLS-1$
				ldap(NS, NAME, VersionRange.create("[1.0.0,2.0.0]"))); //$NON-NLS-1$
	}

	// CLOSED_IN  ([1.0.0, 2.0.0))
	@Test
	public void testClosedIN() {
		assertEquals(
				"(&(name=my.bundle)(namespace=org.eclipse.equinox.p2.iu)(version>=1.0.0)(!(version>=2.0.0)))", //$NON-NLS-1$
				ldap(NS, NAME, VersionRange.create("[1.0.0,2.0.0)"))); //$NON-NLS-1$
	}

	// CLOSED_NI  ((1.0.0, 2.0.0])
	@Test
	public void testClosedNI() {
		assertEquals(
				"(&(name=my.bundle)(namespace=org.eclipse.equinox.p2.iu)(!(version<=1.0.0))(version<=2.0.0))", //$NON-NLS-1$
				ldap(NS, NAME, VersionRange.create("(1.0.0,2.0.0]"))); //$NON-NLS-1$
	}

	// CLOSED_NN  ((1.0.0, 2.0.0))
	@Test
	public void testClosedNN() {
		assertEquals(
				"(&(name=my.bundle)(namespace=org.eclipse.equinox.p2.iu)(!(version<=1.0.0))(!(version>=2.0.0)))", //$NON-NLS-1$
				ldap(NS, NAME, VersionRange.create("(1.0.0,2.0.0)"))); //$NON-NLS-1$
	}

	// An expression using a non-LDAP-serialisable operator must throw
	// UnsupportedOperationException. Using ~= (matches) which has no LDAP equivalent.
	@Test
	public void testNonLDAPSerialisableThrows() {
		IMatchExpression<IInstallableUnit> custom = ExpressionUtil.getFactory()
				.matchExpression(ExpressionUtil.parse("id ~= $0"), //$NON-NLS-1$
						ExpressionUtil.getFactory().matchExpression(ExpressionUtil.parse("'some.id'")));
		StringBuilder buf = new StringBuilder();
		assertThrows(UnsupportedOperationException.class,
				() -> custom.toLDAPString(buf));
	}
}
