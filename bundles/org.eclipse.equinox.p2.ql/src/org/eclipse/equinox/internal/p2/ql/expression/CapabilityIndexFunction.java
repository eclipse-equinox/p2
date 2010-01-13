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

import java.util.Iterator;
import org.eclipse.equinox.internal.p2.metadata.expression.Expression;
import org.eclipse.equinox.internal.p2.metadata.expression.Member;
import org.eclipse.equinox.internal.p2.ql.CapabilityIndex;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.ql.ICapabilityIndex;
import org.eclipse.equinox.p2.ql.IQLExpression;

/**
 * A function that creates a {@link CapabilityIndex} based on a collection of
 * {@link IInstallableUnit} instances.
 */
public final class CapabilityIndexFunction extends Function implements IQLConstants {
	static abstract class CapabilityIndexMethod extends Member implements IQLExpression {
		public CapabilityIndexMethod(Expression operand, String name, Expression[] args) {
			super(operand, name, args);
		}

		final ICapabilityIndex getSelf(IEvaluationContext context) {
			Object self = operand.evaluate(context);
			if (self instanceof ICapabilityIndex)
				return (ICapabilityIndex) self;
			throw new IllegalArgumentException("lhs of member expected to be an ICapabilityIndex implementation"); //$NON-NLS-1$
		}

		public final Object evaluate(IEvaluationContext context) {
			return evaluateAsIterator(context);
		}

		boolean isCollection() {
			return true;
		}
	}

	static final class SatisfiesAny extends CapabilityIndexMethod {

		public SatisfiesAny(Expression operand, Expression[] argExpressions) {
			super(operand, KEYWORD_SATISFIES_ANY, assertLength(argExpressions, 1, 1, KEYWORD_SATISFIES_ANY));
		}

		@SuppressWarnings("unchecked")
		public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
			return getSelf(context).satisfiesAny((Iterator<IRequirement>) argExpressions[0].evaluateAsIterator(context));
		}
	}

	static final class SatisfiesAll extends CapabilityIndexMethod {

		public SatisfiesAll(Expression operand, Expression[] argExpressions) {
			super(operand, KEYWORD_SATISFIES_ALL, assertLength(argExpressions, 1, 1, KEYWORD_SATISFIES_ALL));
		}

		@SuppressWarnings("unchecked")
		public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
			return getSelf(context).satisfiesAll((Iterator<IRequirement>) argExpressions[0].evaluateAsIterator(context));
		}
	}

	public CapabilityIndexFunction(Expression[] operands) {
		super(assertLength(operands, 1, 1, KEYWORD_CAPABILITY_INDEX));
	}

	@SuppressWarnings("unchecked")
	public Object evaluate(IEvaluationContext context) {
		return new CapabilityIndex((Iterator<IInstallableUnit>) operands[0].evaluateAsIterator(context));
	}

	public String getOperator() {
		return KEYWORD_CAPABILITY_INDEX;
	}
}
