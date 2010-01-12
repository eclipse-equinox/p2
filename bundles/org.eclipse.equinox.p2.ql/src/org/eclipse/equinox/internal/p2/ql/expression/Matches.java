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
package org.eclipse.equinox.internal.p2.ql.expression;

import java.util.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.ql.IEvaluationContext;
import org.eclipse.equinox.p2.ql.SimplePattern;
import org.osgi.framework.Filter;

/**
 * <p>A class that performs &quot;matching&quot; The actual algorithm used for
 * performing the match varies depending on the types of the items to match.</p>
 * <p>The following things can be matched:</p>
 * <table border="1" cellpadding="3">
 * <tr><th>LHS</th><th>RHS</th><th>Implemented as</th></tr>
 * <tr><td>IProvidedCapability</td><td>IRequiredCapability</td><td>lhs.satisfies(rhs)</td></tr>
 * <tr><td>IInstallableUnit</td><td>IRequiredCapability</td><td>lhs.satisfies(rhs)</td></tr>
 * <tr><td>Version</td><td>VersionRange</td><td>rhs.isIncluded(lhs)</td></tr>
 * <tr><td>IInstallableUnit</td><td>Filter</td><td>rhs.matches(lhs.properties)</td></tr>
 * <tr><td>Map</td><td>Filter</td><td>rhs.match(lhs)</td></tr>
 * <tr><td>String</td><td>Pattern</td><td>rhs.matcher(lhs).matches()</td></tr>
 * <tr><td>&lt;any&gt;</td><td>Class</td><td>rhs.isInstance(lhs)</td></tr>
 * <tr><td>Class</td><td>Class</td><td>rhs.isAssignableFrom(lhs)</td></tr>
 * </table>
 */
final class Matches extends Binary {
	Matches(Expression lhs, Expression rhs) {
		super(lhs, rhs);
		assertNotBoolean(lhs, "lhs"); //$NON-NLS-1$
		assertNotBoolean(rhs, "rhs"); //$NON-NLS-1$
		assertNotCollection(rhs, "rhs"); //$NON-NLS-1$
	}

	public Object evaluate(IEvaluationContext context) {
		Object lval = lhs.evaluate(context);
		Object rval = rhs.evaluate(context);

		if (rval instanceof IRequirement) {
			IRequirement requirement = (IRequirement) rval;
			if (lval instanceof IInstallableUnit)
				return Boolean.valueOf(((IInstallableUnit) lval).satisfies(requirement));
			if (lval instanceof IProvidedCapability)
				return Boolean.valueOf(((IProvidedCapability) lval).satisfies(requirement));

		} else if (rval instanceof VersionRange) {
			if (lval instanceof Version)
				return Boolean.valueOf(((VersionRange) rval).isIncluded((Version) lval));

		} else if (rval instanceof SimplePattern) {
			if (lval instanceof CharSequence)
				return Boolean.valueOf(((SimplePattern) rval).isMatch((CharSequence) lval));

		} else if (rval instanceof IUpdateDescriptor) {
			if (lval instanceof IInstallableUnit)
				return Boolean.valueOf(((IUpdateDescriptor) rval).isUpdateOf((IInstallableUnit) lval));

		} else if (rval instanceof Filter) {
			if (lval instanceof IInstallableUnit)
				return Boolean.valueOf(((Filter) rval).match(new Hashtable<String, String>(((IInstallableUnit) lval).getProperties())));
			if (lval instanceof Dictionary<?, ?>)
				return Boolean.valueOf(((Filter) rval).match((Dictionary<?, ?>) lval));
			if (lval instanceof Map<?, ?>)
				return Boolean.valueOf(((Filter) rval).match(new Hashtable<Object, Object>((Map<?, ?>) lval)));

		} else if (rval instanceof Locale) {
			if (lval instanceof String)
				return Boolean.valueOf(matchLocaleVariants((Locale) rval, (String) lval));

		} else if (rval instanceof Class<?>) {
			Class<?> rclass = (Class<?>) rval;
			return Boolean.valueOf(lval instanceof Class<?> ? rclass.isAssignableFrom((Class<?>) lval) : rclass.isInstance(lval));
		}

		if (lval == null || rval == null)
			return Boolean.FALSE;

		throw new IllegalArgumentException("Cannot match a " + lval.getClass().getName() + " with a " + rval.getClass().getName()); //$NON-NLS-1$//$NON-NLS-2$
	}

	public int getExpressionType() {
		return TYPE_MATCHES;
	}

	private static boolean equals(String a, String b, int startPos, int endPos) {
		if (endPos - startPos != b.length())
			return false;

		int bidx = 0;
		while (startPos < endPos)
			if (a.charAt(startPos++) != b.charAt(bidx++))
				return false;
		return true;
	}

	private static boolean matchLocaleVariants(Locale rval, String lval) {
		int uscore = lval.indexOf('_');
		if (uscore < 0)
			// No country and no variant. Just match language
			return lval.equals(rval.getLanguage());

		if (!equals(lval, rval.getLanguage(), 0, uscore))
			// Language part doesn't match. Give up.
			return false;

		// Check country and variant
		int countryStart = uscore + 1;
		uscore = lval.indexOf('_', countryStart);
		return uscore < 0 ? equals(lval, rval.getCountry(), countryStart, lval.length()) //
				: equals(lval, rval.getCountry(), countryStart, uscore) && equals(lval, rval.getVariant(), uscore + 1, lval.length());
	}

	String getOperator() {
		return OPERATOR_MATCHES;
	}

	boolean isBoolean() {
		return true;
	}
}
