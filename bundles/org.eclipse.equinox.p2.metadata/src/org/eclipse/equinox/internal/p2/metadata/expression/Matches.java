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
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.util.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.osgi.framework.Filter;

/**
 * <p>A class that performs &quot;matching&quot; The actual algorithm used for
 * performing the match varies depending on the types of the items to match.</p>
 * <p>The following things can be matched:</p>
 * <table border="1" cellpadding="3">
 * <tr><th>LHS</th><th>RHS</th><th>Implemented as</th></tr>
 * <tr><td>{@link String}</td><td>{@link SimplePattern}</td><td>rhs.isMatch(lhs)</td></tr>
 * <tr><td>{@link String}</td><td>{@link LDAPApproximation}</td><td>rhs.isMatch(lhs)</td></tr>
 * <tr><td>&lt;any&gt;</td><td>{@link Class}</td><td>rhs.isInstance(lhs)</td></tr>
 * <tr><td>{@link Class}</td><td>{@link Class}</td><td>rhs.isAssignableFrom(lhs)</td></tr>
 * </table>
 */
public class Matches extends Binary {
	protected Matches(Expression lhs, Expression rhs) {
		super(lhs, rhs);
	}

	public Object evaluate(IEvaluationContext context) {
		return Boolean.valueOf(match(lhs.evaluate(context), rhs.evaluate(context)));
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	protected boolean match(Object lval, Object rval) {
		if (lval == null || rval == null)
			return false;

		if (rval instanceof IRequirement) {
			IRequirement requirement = (IRequirement) rval;
			if (lval instanceof IInstallableUnit)
				return Boolean.valueOf(((IInstallableUnit) lval).satisfies(requirement));
		} else if (rval instanceof VersionRange) {
			VersionRange range = (VersionRange) rval;
			if (lval instanceof Version)
				return Boolean.valueOf(range.isIncluded((Version) lval));
			if (lval instanceof String)
				return range.isIncluded(Version.create((String) lval));
		} else if (rval instanceof SimplePattern) {
			if (lval instanceof CharSequence)
				return ((SimplePattern) rval).isMatch((CharSequence) lval);
			if (lval instanceof Character || lval instanceof Number || lval instanceof Boolean)
				return ((SimplePattern) rval).isMatch(lval.toString());
		} else if (rval instanceof LDAPFilter) {
			return ((LDAPFilter) rval).isMatch(MemberProvider.create(lval, true));
		} else if (rval instanceof Filter) {
			if (lval instanceof IInstallableUnit)
				return Boolean.valueOf(((Filter) rval).match(new Hashtable<String, String>(((IInstallableUnit) lval).getProperties())));
			// TODO Below we use raw types for simplicity;
			// we could convert to Dictionary<String, ?> but that is work and the filter impl
			// must still handle (and ignore) non String keys.
			if (lval instanceof Dictionary<?, ?>)
				return Boolean.valueOf(((Filter) rval).match((Dictionary<String, ?>) lval));
			if (lval instanceof Map<?, ?>)
				return Boolean.valueOf(((Filter) rval).match(new Hashtable((Map<?, ?>) lval)));
		} else if (rval instanceof Locale) {
			if (lval instanceof String)
				return Boolean.valueOf(matchLocaleVariants((Locale) rval, (String) lval));
		} else if (rval instanceof IMatchExpression<?>) {
			IMatchExpression<Object> me = (IMatchExpression<Object>) rval;
			return me.isMatch(lval);
		} else if (rval instanceof IUpdateDescriptor) {
			if (lval instanceof IInstallableUnit)
				return Boolean.valueOf(((IUpdateDescriptor) rval).isUpdateOf((IInstallableUnit) lval));
		} else if (rval instanceof LDAPApproximation) {
			if (lval instanceof CharSequence)
				return ((LDAPApproximation) rval).isMatch((CharSequence) lval);
			if (lval instanceof Character || lval instanceof Number || lval instanceof Boolean)
				return ((LDAPApproximation) rval).isMatch(lval.toString());
		} else if (rval instanceof Class<?>) {
			Class<?> rclass = (Class<?>) rval;
			return lval instanceof Class<?> ? rclass.isAssignableFrom((Class<?>) lval) : rclass.isInstance(lval);
		}
		throw new IllegalArgumentException("Cannot match a " + lval.getClass().getName() + " with a " + rval.getClass().getName()); //$NON-NLS-1$//$NON-NLS-2$
	}

	public int getExpressionType() {
		return TYPE_MATCHES;
	}

	public String getOperator() {
		return OPERATOR_MATCHES;
	}

	public void toLDAPString(StringBuffer buf) {
		if (!(rhs instanceof Literal))
			throw new UnsupportedOperationException();

		boolean escapeWild = true;
		Object val = rhs.evaluate(null);
		buf.append('(');
		appendLDAPAttribute(buf);
		if (val instanceof LDAPApproximation) {
			buf.append(getOperator());
		} else if (val instanceof SimplePattern) {
			buf.append('=');
			escapeWild = false;
		} else
			throw new UnsupportedOperationException();
		appendLDAPEscaped(buf, val.toString(), escapeWild);
		buf.append(')');
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

}
