/*******************************************************************************
 *  Copyright (c) 2007, 2023 IBM Corporation and others.
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
 *     EclipseSource - ongoing development
 *     Todor Boev
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import static org.eclipse.equinox.internal.p2.metadata.InstallableUnit.MEMBER_PROVIDED_CAPABILITIES;
import static org.eclipse.equinox.internal.p2.metadata.ProvidedCapability.MEMBER_NAME;
import static org.eclipse.equinox.internal.p2.metadata.ProvidedCapability.MEMBER_NAMESPACE;
import static org.eclipse.equinox.internal.p2.metadata.ProvidedCapability.MEMBER_VERSION;
import static org.eclipse.equinox.p2.metadata.Version.MAX_VERSION;
import static org.eclipse.equinox.p2.metadata.VersionRange.emptyRange;

import java.util.Arrays;
import java.util.List;
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
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public class RequiredCapability extends Requirement implements IRequiredCapability {
	private static final IExpression ALL = ExpressionUtil.parse(
			String.format("%s.exists(x | x.%s == $0 && x.%s == $1)", //$NON-NLS-1$
					MEMBER_PROVIDED_CAPABILITIES, MEMBER_NAME, MEMBER_NAMESPACE));

	private static final IExpression STRICT = ExpressionUtil.parse(
			String.format("%s.exists(x | x.%s == $0 && x.%s == $1 && x.%s == $2)", //$NON-NLS-1$
					MEMBER_PROVIDED_CAPABILITIES, MEMBER_NAME, MEMBER_NAMESPACE, MEMBER_VERSION));

	private static final IExpression OPEN_I = ExpressionUtil.parse(
			String.format("%s.exists(x | x.%s == $0 && x.%s == $1 && x.%s >= $2)", //$NON-NLS-1$
					MEMBER_PROVIDED_CAPABILITIES, MEMBER_NAME, MEMBER_NAMESPACE, MEMBER_VERSION));

	private static final IExpression OPEN_N = ExpressionUtil.parse(
			String.format("%s.exists(x | x.%s == $0 && x.%s == $1 && x.%s > $2)", //$NON-NLS-1$
					MEMBER_PROVIDED_CAPABILITIES, MEMBER_NAME, MEMBER_NAMESPACE, MEMBER_VERSION));

	private static final IExpression CLOSED_II = ExpressionUtil.parse(
			String.format("%s.exists(x | x.%s == $0 && x.%s == $1 && x.%s >= $2 && x.%s <= $3)", //$NON-NLS-1$
					MEMBER_PROVIDED_CAPABILITIES, MEMBER_NAME, MEMBER_NAMESPACE, MEMBER_VERSION, MEMBER_VERSION));

	private static final IExpression CLOSED_IN = ExpressionUtil.parse(
			String.format("%s.exists(x | x.%s == $0 && x.%s == $1 && x.%s >= $2 && x.%s < $3)", //$NON-NLS-1$
					MEMBER_PROVIDED_CAPABILITIES, MEMBER_NAME, MEMBER_NAMESPACE, MEMBER_VERSION, MEMBER_VERSION));

	private static final IExpression CLOSED_NI = ExpressionUtil.parse(
			String.format("%s.exists(x | x.%s == $0 && x.%s == $1 && x.%s > $2 && x.%s <= $3)", //$NON-NLS-1$
					MEMBER_PROVIDED_CAPABILITIES, MEMBER_NAME, MEMBER_NAMESPACE, MEMBER_VERSION, MEMBER_VERSION));

	private static final IExpression CLOSED_NN = ExpressionUtil.parse(
			String.format("%s.exists(x | x.%s == $0 && x.%s == $1 && x.%s > $2 && x.%s < $3)", //$NON-NLS-1$
					MEMBER_PROVIDED_CAPABILITIES, MEMBER_NAME, MEMBER_NAMESPACE, MEMBER_VERSION, MEMBER_VERSION));

	private static final List<IExpression> PREDEFINED = Arrays.asList(
			ALL, STRICT, OPEN_I, OPEN_N, CLOSED_II, CLOSED_IN, CLOSED_NI, CLOSED_NN);

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
		return getNamespace() + "; " + getName() + " " + getRange(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static IMatchExpression<IInstallableUnit> createMatchExpressionFromRange(String namespace, String name,
			VersionRange range) {
		Assert.isNotNull(namespace);
		Assert.isNotNull(name);

		IExpressionFactory factory = ExpressionUtil.getFactory();

		// All versions
		if (range == null || range.equals(emptyRange)) {
			return factory.matchExpression(ALL, name, namespace);
		}

		// Exact version
		if (range.getMinimum().equals(range.getMaximum())) {
			return factory.matchExpression(STRICT, name, namespace, range.getMinimum());
		}

		// Open range
		if (range.getMaximum().equals(MAX_VERSION)) {
			// Open inclusive or non-inclusive
			IExpression expr = range.getIncludeMinimum() ? OPEN_I : OPEN_N;
			return factory.matchExpression(expr, name, namespace, range.getMinimum());
		}

		// Closed range
		IExpression expr = range.getIncludeMinimum()
				// Left inclusive. Right inclusive or non-inclusive
				? (range.getIncludeMaximum() ? CLOSED_II : CLOSED_IN)
				// Left non-inclusive. Right inclusive or non-inclusive
				: (range.getIncludeMaximum() ? CLOSED_NI : CLOSED_NN);
		return factory.matchExpression(expr, name, namespace, range.getMinimum(), range.getMaximum());
	}

	public static String extractName(IMatchExpression<IInstallableUnit> matchExpression) {
		assertVersionRangeRequirement(matchExpression);
		return (String) matchExpression.getParameters()[0];
	}

	public static String extractNamespace(IMatchExpression<IInstallableUnit> matchExpression) {
		assertVersionRangeRequirement(matchExpression);
		return (String) matchExpression.getParameters()[1];
	}

	public static VersionRange extractRange(IMatchExpression<IInstallableUnit> matchExpression) {
		assertVersionRangeRequirement(matchExpression);

		IExpression expr = ExpressionUtil.getOperand(matchExpression);
		Object[] params = matchExpression.getParameters();

		return switch (params.length) {
		case 2 -> emptyRange; // No version parameter
		case 3 -> { // One version parameter: strict or one of the open ranges
			Version v = (Version) params[2];
			if (expr.equals(STRICT)) {
				yield new VersionRange(v, true, v, true);
			}
			yield new VersionRange(v, expr.equals(OPEN_I), MAX_VERSION, true);
		}
		default -> { // Two version parameters: one of the closed ranges
			Version left = (Version) params[2];
			boolean leftInclusive = expr.equals(CLOSED_II) || expr.equals(CLOSED_IN);
			Version right = (Version) params[3];
			boolean rightInclusive = expr.equals(CLOSED_II) || expr.equals(CLOSED_NI);
			yield new VersionRange(left, leftInclusive, right, rightInclusive);
		}
		};
	}

	public static boolean isStrictVersionRequirement(IMatchExpression<IInstallableUnit> matchExpression) {
		return STRICT == ExpressionUtil.getOperand(matchExpression);
	}

	public static boolean isVersionRangeRequirement(IMatchExpression<IInstallableUnit> matchExpression) {
		return PREDEFINED.contains(ExpressionUtil.getOperand(matchExpression));
	}

	private static void assertVersionRangeRequirement(IMatchExpression<IInstallableUnit> matchExpression) {
		if (!isVersionRangeRequirement(matchExpression)) {
			throw new IllegalArgumentException();
		}
	}
}
