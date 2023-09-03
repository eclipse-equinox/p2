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
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.Objects;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.metadata.expression.IMemberProvider;

/**
 * A requirement represents some external constraint on an {@link IInstallableUnit}.
 * Each requirement represents something an {@link IInstallableUnit} needs that
 * it expects to be provided by another {@link IInstallableUnit}. Requirements are
 * entirely generic, and are intended to be capable of representing anything that
 * an {@link IInstallableUnit} may need either at install time, or at runtime.
 * <p>
 * Instances of this class are handle objects and do not necessarily
 * reflect entities that exist in any particular profile or repository. These handle 
 * objects can be created using {@link MetadataFactory}.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 * @see IProvidedCapability
 * @see MetadataFactory#createRequirement(String, String, VersionRange, String, boolean, boolean, boolean)
 */
public class Requirement implements IRequirement, IMemberProvider {
	public static final String MEMBER_FILTER = "filter"; //$NON-NLS-1$
	public static final String MEMBER_MIN = "min"; //$NON-NLS-1$
	public static final String MEMBER_MAX = "max"; //$NON-NLS-1$
	public static final String MEMBER_GREEDY = "greedy"; //$NON-NLS-1$
	public static final String MEMBER_MATCH = "match"; //$NON-NLS-1$

	protected final IMatchExpression<IInstallableUnit> filter;
	protected final IMatchExpression<IInstallableUnit> matchExpression;
	protected final boolean greedy;
	protected final int min;
	protected final int max;
	protected final String description;

	public Requirement(IMatchExpression<IInstallableUnit> requirement, IMatchExpression<IInstallableUnit> filter,
			int min, int max, boolean greedy, String description) {
		this.matchExpression = requirement;
		this.filter = filter;
		this.min = min;
		this.max = max;
		this.greedy = greedy;
		this.description = description;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		// Expression
		result.append(matchExpression);

		// Parameters
		Object[] params = matchExpression.getParameters();
		if (params.length > 0) {
			result.append(" ("); //$NON-NLS-1$
			for (int i = 0; i < params.length; i++) {
				if (i > 0) {
					result.append(", "); //$NON-NLS-1$
				}
				result.append(params[i]);
			}
			result.append(')');
		}

		return result.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(filter, matchExpression, min, max, greedy, matchExpression);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return obj instanceof IRequirement other //
				&& Objects.equals(filter, other.getFilter()) //
				&& min == other.getMin() && max == other.getMax() //
				&& greedy == other.isGreedy() //
				&& matchExpression.equals(other.getMatches());
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public int getMin() {
		return min;
	}

	@Override
	public int getMax() {
		return max;
	}

	@Override
	public boolean isGreedy() {
		return greedy;
	}

	@Override
	public IMatchExpression<IInstallableUnit> getFilter() {
		return filter;
	}

	@Override
	public IMatchExpression<IInstallableUnit> getMatches() {
		return matchExpression;
	}

	@Override
	public boolean isMatch(IInstallableUnit candidate) {
		return matchExpression.isMatch(candidate);
	}

	@Override
	public Object getMember(String memberName) {
		return switch (memberName) {
		case MEMBER_FILTER -> filter;
		case MEMBER_MIN -> min;
		case MEMBER_MAX -> max;
		case MEMBER_GREEDY -> greedy;
		case MEMBER_MATCH -> matchExpression;
		default -> throw new IllegalArgumentException("No such member: " + memberName); //$NON-NLS-1$
		};
	}
}
