/*******************************************************************************
 *  Copyright (c) 2007, 2016 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.*;

/**
 * A required capability represents some external constraint on an {@link IInstallableUnit}.
 * Each capability represents something an {@link IInstallableUnit} needs that
 * it expects to be provided by another {@link IInstallableUnit}. Capabilities are
 * entirely generic, and are intended to be capable of representing anything that
 * an {@link IInstallableUnit} may need either at install time, or at runtime.
 * <p>
 * Capabilities are segmented into namespaces.  Anyone can introduce new 
 * capability namespaces. Some well-known namespaces are introduced directly
 * by the provisioning framework.
 * 
 * @see IInstallableUnit#NAMESPACE_IU_ID
 */
public class RequiredCapability implements IRequiredCapability, IMemberProvider {
	private final IMatchExpression<IInstallableUnit> filter;
	private final IMatchExpression<IInstallableUnit> matchExpression;

	public static final String MEMBER_FILTER = "filter"; //$NON-NLS-1$
	public static final String MEMBER_MIN = "min"; //$NON-NLS-1$
	public static final String MEMBER_MAX = "max"; //$NON-NLS-1$
	public static final String MEMBER_GREEDY = "greedy"; //$NON-NLS-1$
	public static final String MEMBER_MATCH = "match"; //$NON-NLS-1$

	private final boolean greedy;
	private final int min;
	private final int max;
	private String description;

	private static final IExpression allVersionsExpression;
	private static final IExpression range_II_Expression;
	private static final IExpression range_IN_Expression;
	private static final IExpression range_NI_Expression;
	private static final IExpression range_NN_Expression;
	private static final IExpression strictVersionExpression;
	private static final IExpression openEndedExpression;
	private static final IExpression openEndedNonInclusiveExpression;

	static {
		IExpressionFactory factory = ExpressionUtil.getFactory();
		IExpression xVar = factory.variable("x"); //$NON-NLS-1$
		IExpression nameEqual = factory.equals(factory.member(xVar, ProvidedCapability.MEMBER_NAME), factory.indexedParameter(0));
		IExpression namespaceEqual = factory.equals(factory.member(xVar, ProvidedCapability.MEMBER_NAMESPACE), factory.indexedParameter(1));

		IExpression versionMember = factory.member(xVar, ProvidedCapability.MEMBER_VERSION);

		IExpression versionCmpLow = factory.indexedParameter(2);
		IExpression versionEqual = factory.equals(versionMember, versionCmpLow);
		IExpression versionGt = factory.greater(versionMember, versionCmpLow);
		IExpression versionGtEqual = factory.greaterEqual(versionMember, versionCmpLow);

		IExpression versionCmpHigh = factory.indexedParameter(3);
		IExpression versionLt = factory.less(versionMember, versionCmpHigh);
		IExpression versionLtEqual = factory.lessEqual(versionMember, versionCmpHigh);

		IExpression pvMember = factory.member(factory.thisVariable(), InstallableUnit.MEMBER_PROVIDED_CAPABILITIES);
		allVersionsExpression = factory.exists(pvMember, factory.lambda(xVar, factory.and(nameEqual, namespaceEqual)));
		strictVersionExpression = factory.exists(pvMember, factory.lambda(xVar, factory.and(nameEqual, namespaceEqual, versionEqual)));
		openEndedExpression = factory.exists(pvMember, factory.lambda(xVar, factory.and(nameEqual, namespaceEqual, versionGtEqual)));
		openEndedNonInclusiveExpression = factory.exists(pvMember, factory.lambda(xVar, factory.and(nameEqual, namespaceEqual, versionGt)));
		range_II_Expression = factory.exists(pvMember, factory.lambda(xVar, factory.and(nameEqual, namespaceEqual, versionGtEqual, versionLtEqual)));
		range_IN_Expression = factory.exists(pvMember, factory.lambda(xVar, factory.and(nameEqual, namespaceEqual, versionGtEqual, versionLt)));
		range_NI_Expression = factory.exists(pvMember, factory.lambda(xVar, factory.and(nameEqual, namespaceEqual, versionGt, versionLtEqual)));
		range_NN_Expression = factory.exists(pvMember, factory.lambda(xVar, factory.and(nameEqual, namespaceEqual, versionGt, versionLt)));
	}

	/**
	 * TODO replace booleans with int options flag.
	 */
	public RequiredCapability(String namespace, String name, VersionRange range, String filter, boolean optional, boolean multiple) {
		this(namespace, name, range, filter, optional, multiple, true);
	}

	public RequiredCapability(IMatchExpression<IInstallableUnit> requirement, IMatchExpression<IInstallableUnit> filter, int min, int max, boolean greedy, String description) {
		this.matchExpression = requirement;
		this.filter = filter;
		this.min = min;
		this.max = max;
		this.greedy = greedy;
		this.description = description;
	}

	public RequiredCapability(String namespace, String name, VersionRange range, String filter, boolean optional, boolean multiple, boolean greedy) {
		this(namespace, name, range, filter == null ? (IMatchExpression<IInstallableUnit>) null : InstallableUnit.parseFilter(filter), optional ? 0 : 1, multiple ? Integer.MAX_VALUE : 1, greedy, null);
	}

	public RequiredCapability(String namespace, String name, VersionRange range, IMatchExpression<IInstallableUnit> filter, int min, int max, boolean greedy, String description) {
		Assert.isNotNull(namespace);
		Assert.isNotNull(name);
		IExpressionFactory factory = ExpressionUtil.getFactory();
		if (range == null || range.equals(VersionRange.emptyRange)) {
			matchExpression = factory.matchExpression(allVersionsExpression, name, namespace);
		} else {
			if (range.getMinimum().equals(range.getMaximum())) {
				// Explicit version appointed
				matchExpression = factory.matchExpression(strictVersionExpression, name, namespace, range.getMinimum());
			} else {
				if (range.getMaximum().equals(Version.MAX_VERSION)) {
					// Open ended
					matchExpression = factory.matchExpression(range.getIncludeMinimum() ? openEndedExpression : openEndedNonInclusiveExpression, name, namespace, range.getMinimum());
				} else {
					matchExpression = factory.matchExpression(//
							range.getIncludeMinimum() ? (range.getIncludeMaximum() ? range_II_Expression : range_IN_Expression) //
									: (range.getIncludeMaximum() ? range_NI_Expression : range_NN_Expression), //
							name, namespace, range.getMinimum(), range.getMaximum());
				}
			}
		}
		this.min = min;
		this.max = max;
		this.greedy = greedy;
		this.filter = filter;
		this.description = description;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (!(obj instanceof IRequirement))
			return false;

		IRequirement other = (IRequirement) obj;
		if (filter == null) {
			if (other.getFilter() != null)
				return false;
		} else if (!filter.equals(other.getFilter()))
			return false;

		return min == other.getMin() && max == other.getMax() && greedy == other.isGreedy() && matchExpression.equals(other.getMatches());
	}

	public String getName() {
		return (String) matchExpression.getParameters()[0];
	}

	public String getNamespace() {
		return (String) matchExpression.getParameters()[1];
	}

	/**
	 * Returns the range of versions that satisfy this required capability. Returns
	 * an empty version range ({@link VersionRange#emptyRange} if any version
	 * will satisfy the capability.
	 * @return the range of versions that satisfy this required capability.
	 */
	public VersionRange getRange() {
		return extractRange(matchExpression);
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		result = prime * result + matchExpression.hashCode();
		return result;
	}

	public boolean isGreedy() {
		return greedy;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		if (matchExpression.getParameters().length == 0)
			return matchExpression.toString();

		if (!isSimpleRequirement(matchExpression)) {
			result.append(matchExpression.toString()).append(' ');
			for (int i = 0; i < matchExpression.getParameters().length; i++) {
				result.append(matchExpression.getParameters()[i]).append(' ');
			}
			return result.toString();
		}

		if (IInstallableUnit.NAMESPACE_IU_ID.equals(getNamespace())) {
			//print nothing for an IU id dependency because this is the default (most common) case
			result.append(""); //$NON-NLS-1$
		} else if ("osgi.bundle".equals(getNamespace())) { //$NON-NLS-1$
			result.append("bundle"); //$NON-NLS-1$
		} else if ("java.package".equals(getNamespace())) { //$NON-NLS-1$
			result.append("package"); //$NON-NLS-1$
		} else {
			result.append(getNamespace());
		}
		if (result.length() > 0)
			result.append(' ');
		result.append(getName());
		result.append(' ');
		VersionRange range = getRange();
		//for an exact version match, print a simpler expression
		if (range.getMinimum().equals(range.getMaximum()))
			result.append('[').append(range.getMinimum()).append(']');
		else
			result.append(range);
		return result.toString();
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	public IMatchExpression<IInstallableUnit> getMatches() {
		return matchExpression;
	}

	public IMatchExpression<IInstallableUnit> getFilter() {
		return filter;
	}

	public boolean isMatch(IInstallableUnit candidate) {
		return matchExpression.isMatch(candidate);
	}

	public static boolean isVersionStrict(IMatchExpression<IInstallableUnit> matchExpression) {
		return ExpressionUtil.getOperand(matchExpression) == strictVersionExpression;
	}

	public static String extractName(IMatchExpression<IInstallableUnit> matchExpression) {
		assertValid(matchExpression);
		return (String) matchExpression.getParameters()[0];
	}

	public static String extractNamespace(IMatchExpression<IInstallableUnit> matchExpression) {
		assertValid(matchExpression);
		return (String) matchExpression.getParameters()[1];
	}

	public static VersionRange extractRange(IMatchExpression<IInstallableUnit> matchExpression) {
		IExpression expr = assertValid(matchExpression);
		Object[] params = matchExpression.getParameters();
		if (params.length < 3)
			return VersionRange.emptyRange;
		Version v = (Version) params[2];
		if (params.length < 4) {
			if (expr.equals(strictVersionExpression))
				return new VersionRange(v, true, v, true);
			return new VersionRange(v, expr.equals(openEndedExpression), Version.MAX_VERSION, true);
		}
		Version h = (Version) params[3];
		return new VersionRange(v, expr.equals(range_II_Expression) || expr.equals(range_IN_Expression), h, expr.equals(range_II_Expression) || expr.equals(range_NI_Expression));
	}

	public static boolean isSimpleRequirement(IMatchExpression<IInstallableUnit> matchExpression) {
		return isPredefined(ExpressionUtil.getOperand(matchExpression));
	}

	private static IExpression assertValid(IMatchExpression<IInstallableUnit> matchExpression) {
		IExpression expr = ExpressionUtil.getOperand(matchExpression);
		if (!isPredefined(expr))
			throw new IllegalArgumentException();
		return expr;
	}

	private static boolean isPredefined(IExpression expr) {
		return expr.equals(allVersionsExpression) || expr.equals(range_II_Expression) || expr.equals(range_IN_Expression) || expr.equals(range_NI_Expression) || expr.equals(range_NN_Expression) || expr.equals(strictVersionExpression) || expr.equals(openEndedExpression) || expr.equals(openEndedNonInclusiveExpression);
	}

	public Object getMember(String memberName) {
		// It is OK to use identity comparisons here since
		// a) All constant valued strings are always interned
		// b) The Member constructor always interns the name
		//
		if (MEMBER_FILTER == memberName)
			return filter;
		if (MEMBER_MIN == memberName)
			return Integer.valueOf(min);
		if (MEMBER_MAX == memberName)
			return Integer.valueOf(max);
		if (MEMBER_GREEDY == memberName)
			return Boolean.valueOf(greedy);
		if (MEMBER_MATCH == memberName)
			return matchExpression;
		throw new IllegalArgumentException("No such member: " + memberName); //$NON-NLS-1$
	}

	public String getDescription() {
		return description;
	}
}
