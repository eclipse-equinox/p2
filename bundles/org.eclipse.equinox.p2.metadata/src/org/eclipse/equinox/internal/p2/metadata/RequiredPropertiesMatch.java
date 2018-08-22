/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     Todor Boev
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import static org.eclipse.equinox.internal.p2.metadata.InstallableUnit.MEMBER_PROVIDED_CAPABILITIES;
import static org.eclipse.equinox.internal.p2.metadata.ProvidedCapability.MEMBER_NAMESPACE;
import static org.eclipse.equinox.internal.p2.metadata.ProvidedCapability.MEMBER_PROPERTIES;

import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.expression.IExpressionFactory;
import org.eclipse.equinox.p2.metadata.expression.IFilterExpression;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;

/**
 * A required capability match represents some external constraint on an {@link IInstallableUnit}.
 * <p>
 * This is a flavor of the general {@link IRequirement} that searches for
 * a capability that has {@link IProvidedCapability#getProperties() properties} that match a given expression.
 * I.e. this is much more limited that an arbitrary match expression executed over all metadata of the IU.
 */
public class RequiredPropertiesMatch extends Requirement {
	/**
	 * Argument $0 must evaluate to a String
	 * Argument $2 must evaluate to an expression compatible with the match operator "~="
	 */
	private static final IExpression PROPERTIES_MATCH = ExpressionUtil.parse(
			String.format("%s.exists(cap | cap.%s == $0 && cap.%s ~= $1)", //$NON-NLS-1$
					MEMBER_PROVIDED_CAPABILITIES, MEMBER_NAMESPACE, MEMBER_PROPERTIES));

	public RequiredPropertiesMatch(String namespace, IFilterExpression attrFilter, IMatchExpression<IInstallableUnit> envFilter, int min, int max, boolean greedy, String description) {
		super(createMatchExpressionFromFilter(namespace, attrFilter), envFilter, min, max, greedy, description);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append(extractNamespace(getMatches()));
		result.append("; "); //$NON-NLS-1$
		result.append(extractPropertiesMatch(getMatches()));

		return result.toString();
	}

	public static IMatchExpression<IInstallableUnit> createMatchExpressionFromFilter(String namespace, IFilterExpression attrFilter) {
		Assert.isNotNull(namespace);
		Assert.isNotNull(attrFilter);
		IExpressionFactory factory = ExpressionUtil.getFactory();
		return factory.matchExpression(PROPERTIES_MATCH, namespace, attrFilter);
	}

	public static String extractNamespace(IMatchExpression<IInstallableUnit> matchExpression) {
		assertValid(matchExpression);
		return (String) matchExpression.getParameters()[0];
	}

	public static IFilterExpression extractPropertiesMatch(IMatchExpression<IInstallableUnit> matchExpression) {
		assertValid(matchExpression);
		return (IFilterExpression) matchExpression.getParameters()[1];
	}

	public static boolean isPropertiesMatchRequirement(IMatchExpression<IInstallableUnit> matchExpression) {
		return PROPERTIES_MATCH.equals(ExpressionUtil.getOperand(matchExpression));
	}

	private static void assertValid(IMatchExpression<IInstallableUnit> matchExpression) {
		if (!isPropertiesMatchRequirement(matchExpression)) {
			throw new IllegalArgumentException();
		}
	}
}
