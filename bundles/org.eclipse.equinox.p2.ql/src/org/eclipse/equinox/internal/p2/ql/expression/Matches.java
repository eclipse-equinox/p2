/*******************************************************************************
 * Copyright (c) 2010 Cloudsmith Inc. and others.
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
import org.eclipse.equinox.internal.p2.metadata.expression.Expression;
import org.eclipse.equinox.internal.p2.metadata.expression.LDAPApproximation;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.SimplePattern;
import org.osgi.framework.Filter;

/**
 * <p>A class that performs &quot;matching&quot; The actual algorithm used for
 * performing the match varies depending on the types of the items to match.</p>
 * <p>The following things can be matched:</p>
 * <table border="1" cellpadding="3">
 * <tr><th>LHS</th><th>RHS</th><th>Implemented as</th></tr>
 * <tr><td>IProvidedCapability</td><td>IRequirement</td><td>lhs.satisfies(rhs)</td></tr>
 * <tr><td>IInstallableUnit</td><td>IRequirement</td><td>lhs.satisfies(rhs)</td></tr>
 * <tr><td>Version</td><td>VersionRange</td><td>rhs.isIncluded(lhs)</td></tr>
 * <tr><td>IInstallableUnit</td><td>Filter</td><td>rhs.matches(lhs.properties)</td></tr>
 * <tr><td>Map</td><td>Filter</td><td>rhs.match(lhs)</td></tr>
 * <tr><td>{@link String}</td><td>{@link SimplePattern}</td><td>rhs.isMatch(lhs)</td></tr>
 * <tr><td>{@link String}</td><td>{@link LDAPApproximation}</td><td>rhs.isMatch(lhs)</td></tr>
 * <tr><td>&lt;any&gt;</td><td>{@link Class}</td><td>rhs.isInstance(lhs)</td></tr>
 * <tr><td>{@link Class}</td><td>{@link Class}</td><td>rhs.isAssignableFrom(lhs)</td></tr>
 * </table>
 */
final class Matches extends org.eclipse.equinox.internal.p2.metadata.expression.Matches {
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

	Matches(Expression lhs, Expression rhs) {
		super(lhs, rhs);
	}

	protected boolean match(Object lval, Object rval) {
		if (rval instanceof IRequirement) {
			IRequirement requirement = (IRequirement) rval;
			if (lval instanceof IInstallableUnit)
				return Boolean.valueOf(((IInstallableUnit) lval).satisfies(requirement));
		} else if (rval instanceof VersionRange) {
			if (lval instanceof Version)
				return Boolean.valueOf(((VersionRange) rval).isIncluded((Version) lval));

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
		}
		return super.match(lval, rval);
	}
}
