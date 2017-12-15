/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
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
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.expression.IExpressionFactory;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;

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
public class RequiredCapability extends Requirement implements IRequiredCapability {
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
	 * TODO Remove. This is a private impl class. Users must call the analogous MetadataFactory.createRequirement()
	 * @deprecated To be removed once CBI is fixed.
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	@Deprecated
	public RequiredCapability(String namespace, String name, VersionRange range, String filter, boolean optional, boolean multiple) {
		this(namespace, name, range, filter, optional, multiple, true);
	}

	/**
	 * TODO Remove. This is a private impl class. Users must call the analogous MetadataFactory.createRequirement()
	 * @deprecated To be removed once CBI is fixed.
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	@Deprecated
	public RequiredCapability(String namespace, String name, VersionRange range, String filter, boolean optional, boolean multiple, boolean greedy) {
		this(namespace, name, range, InstallableUnit.parseFilter(filter), optional ? 0 : 1, multiple ? Integer.MAX_VALUE : 1, greedy, null);
	}

	public RequiredCapability(String namespace, String name, VersionRange range, IMatchExpression<IInstallableUnit> filter, int min, int max, boolean greedy, String description) {
		super(createMatchExpressionFromRange(namespace, name, range), filter, min, max, greedy, description);
	}

	@Override
	public String getNamespace() {
		return extractNamespace(matchExpression);
	}

	@Override
	public String getName() {
		return extractName(matchExpression);
	}

	@Override
	public VersionRange getRange() {
		return extractRange(matchExpression);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		// Namespace
		result.append(getNamespace());
		result.append(' ');

		// Name
		result.append(getName());
		result.append(' ');

		// Version range
		VersionRange range = getRange();
		result.append(range);

		return result.toString();
	}

	public static IMatchExpression<IInstallableUnit> createMatchExpressionFromRange(String namespace, String name, VersionRange range) {
		Assert.isNotNull(namespace);
		Assert.isNotNull(name);

		// Empty range - matches everything
		IExpressionFactory factory = ExpressionUtil.getFactory();
		if (range == null || range.equals(VersionRange.emptyRange)) {
			return factory.matchExpression(allVersionsExpression, name, namespace);
		}

		// Point range - matches exactly one version
		if (range.getMinimum().equals(range.getMaximum())) {
			return factory.matchExpression(strictVersionExpression, name, namespace, range.getMinimum());
		}

		// Semi-closed ranged - matches a minimum version
		if (range.getMaximum().equals(Version.MAX_VERSION)) {
			return factory.matchExpression(range.getIncludeMinimum() ? openEndedExpression : openEndedNonInclusiveExpression, name, namespace, range.getMinimum());
		}

		// Closed range - matches version between a minimum and a maximum
		return factory.matchExpression(//
				range.getIncludeMinimum() ? (range.getIncludeMaximum() ? range_II_Expression : range_IN_Expression) //
						: (range.getIncludeMaximum() ? range_NI_Expression : range_NN_Expression), //
				name, namespace, range.getMinimum(), range.getMaximum());
	}

	public static String extractNamespace(IMatchExpression<IInstallableUnit> matchExpression) {
		assertValid(matchExpression);
		return (String) matchExpression.getParameters()[1];
	}

	public static String extractName(IMatchExpression<IInstallableUnit> matchExpression) {
		assertValid(matchExpression);
		return (String) matchExpression.getParameters()[0];
	}

	public static VersionRange extractRange(IMatchExpression<IInstallableUnit> matchExpression) {
		IExpression expr = assertValid(matchExpression);
		Object[] params = matchExpression.getParameters();
		if (params.length < 3) {
			return VersionRange.emptyRange;
		}
		Version v = (Version) params[2];
		if (params.length < 4) {
			if (expr.equals(strictVersionExpression)) {
				return new VersionRange(v, true, v, true);
			}
			return new VersionRange(v, expr.equals(openEndedExpression), Version.MAX_VERSION, true);
		}
		Version h = (Version) params[3];
		return new VersionRange(v, expr.equals(range_II_Expression) || expr.equals(range_IN_Expression), h, expr.equals(range_II_Expression) || expr.equals(range_NI_Expression));
	}

	public static boolean isVersionStrict(IMatchExpression<IInstallableUnit> matchExpression) {
		return ExpressionUtil.getOperand(matchExpression) == strictVersionExpression;
	}

	public static boolean isSimpleRequirement(IMatchExpression<IInstallableUnit> matchExpression) {
		return isPredefined(ExpressionUtil.getOperand(matchExpression));
	}

	private static IExpression assertValid(IMatchExpression<IInstallableUnit> matchExpression) {
		IExpression expr = ExpressionUtil.getOperand(matchExpression);
		if (!isPredefined(expr)) {
			throw new IllegalArgumentException();
		}
		return expr;
	}

	private static boolean isPredefined(IExpression expr) {
		return expr.equals(allVersionsExpression) || expr.equals(range_II_Expression) || expr.equals(range_IN_Expression) || expr.equals(range_NI_Expression) || expr.equals(range_NN_Expression) || expr.equals(strictVersionExpression) || expr.equals(openEndedExpression) || expr.equals(openEndedNonInclusiveExpression);
	}
}
