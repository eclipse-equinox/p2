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
 *     Todor Boev
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import static org.eclipse.equinox.internal.p2.metadata.InstallableUnit.MEMBER_PROVIDED_CAPABILITIES;
import static org.eclipse.equinox.internal.p2.metadata.ProvidedCapability.MEMBER_NAME;
import static org.eclipse.equinox.internal.p2.metadata.ProvidedCapability.MEMBER_NAMESPACE;
import static org.eclipse.equinox.internal.p2.metadata.ProvidedCapability.MEMBER_VERSION;

import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
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
	private static final IExpression simpleMatchExpression;

	static {
		IExpressionFactory factory = ExpressionUtil.getFactory();

		IExpression xVar = factory.variable("cap"); //$NON-NLS-1$

		IExpression name = factory.member(xVar, MEMBER_NAME);
		IExpression nameEqual = factory.equals(name, factory.indexedParameter(0));

		IExpression namespace = factory.member(xVar, MEMBER_NAMESPACE);
		IExpression namespaceEqual = factory.equals(namespace, factory.indexedParameter(1));

		IExpression version = factory.member(xVar, MEMBER_VERSION);
		IExpression versionInRange = factory.matches(version, factory.indexedParameter(2));

		IExpression pvMember = factory.member(factory.thisVariable(), MEMBER_PROVIDED_CAPABILITIES);

		// Place nameEqual first to eliminate quickly most non-matching candidates
		simpleMatchExpression = factory.exists(pvMember, factory.lambda(xVar, factory.and(nameEqual, namespaceEqual, versionInRange)));
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
		Object resolvedRange = (range != null) ? range : VersionRange.emptyRange;
		IExpressionFactory factory = ExpressionUtil.getFactory();
		return factory.matchExpression(simpleMatchExpression, name, namespace, resolvedRange);
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
		assertValid(matchExpression);
		Object[] params = matchExpression.getParameters();
		return (VersionRange) params[2];
	}

	public static boolean isVersionStrict(IMatchExpression<IInstallableUnit> matchExpression) {
		if (!isSimpleRequirement(matchExpression)) {
			return false;
		}

		Object[] params = matchExpression.getParameters();
		VersionRange range = (VersionRange) params[2];
		return range.getMinimum().equals(range.getMaximum());
	}

	public static boolean isSimpleRequirement(IMatchExpression<IInstallableUnit> matchExpression) {
		return simpleMatchExpression.equals(ExpressionUtil.getOperand(matchExpression));
	}

	private static void assertValid(IMatchExpression<IInstallableUnit> matchExpression) {
		if (!isSimpleRequirement(matchExpression)) {
			throw new IllegalArgumentException();
		}
	}
}
