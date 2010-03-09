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
package org.eclipse.equinox.internal.p2.metadata.index;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.equinox.internal.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.index.IIndex;

public abstract class Index<T> implements IIndex<T> {

	protected static boolean isIndexedMember(IExpression expr, IExpression variable, String memberName) {
		if (expr instanceof Member) {
			Member member = (Member) expr;
			return member.getOperand() == variable && member.getName().equals(memberName);
		}
		return false;
	}

	protected static Object concatenateUnique(Object previous, Object toAdd) {
		if (previous == null || toAdd == null || toAdd == Boolean.FALSE)
			return toAdd;

		if (previous instanceof ArrayList<?>) {
			@SuppressWarnings("unchecked")
			ArrayList<Object> prevArr = (ArrayList<Object>) previous;
			if (!prevArr.contains(toAdd))
				prevArr.add(toAdd);
			return previous;
		}
		if (previous.equals(toAdd))
			return previous;

		ArrayList<Object> arr = new ArrayList<Object>();
		arr.add(previous);
		arr.add(toAdd);
		return arr;
	}

	protected Object getQueriedIDs(IEvaluationContext ctx, IExpression variable, String memberName, IExpression booleanExpr, Object queriedKeys) {
		IExpression targetExpr = booleanExpr;
		if (booleanExpr instanceof IMatchExpression<?>) {
			targetExpr = ((Unary) targetExpr).operand;
			ctx = ((IMatchExpression<?>) booleanExpr).createContext();
		}
		int type = targetExpr.getExpressionType();
		switch (type) {
			case IExpression.TYPE_EQUALS :
				Binary eqExpr = (Binary) targetExpr;
				IExpression lhs = eqExpr.lhs;
				IExpression rhs = eqExpr.rhs;
				if (isIndexedMember(lhs, variable, memberName)) {
					Object val = safeEvaluate(ctx, rhs);
					if (val == null)
						return null;
					return concatenateUnique(queriedKeys, val);
				}
				if (isIndexedMember(rhs, variable, memberName)) {
					Object val = safeEvaluate(ctx, lhs);
					if (val == null)
						return null;
					return concatenateUnique(queriedKeys, val);
				}

				// Not applicable for indexing
				return null;

			case IExpression.TYPE_AND :
				// AND is OK if at least one of the branches require the queried key
				for (IExpression expr : ExpressionUtil.getOperands(targetExpr)) {
					Object test = getQueriedIDs(ctx, variable, memberName, expr, queriedKeys);
					if (test != null) {
						if (test == Boolean.FALSE)
							// Failing exists so the AND will fail altogether
							return test;

						// It's safe to break here since an and'ing several queries
						// for different keys and the same input will yield false anyway.
						return test;
					}
				}
				return null;

			case IExpression.TYPE_OR :
				// OR is OK if all the branches require the queried key
				for (IExpression expr : ExpressionUtil.getOperands(targetExpr)) {
					Object test = getQueriedIDs(ctx, variable, memberName, expr, queriedKeys);
					if (test == null)
						// This branch did not require the key so index cannot be used
						return null;

					if (test == Boolean.FALSE)
						// Branch will always fail regardless of input, so just ignore
						continue;

					queriedKeys = test;
				}
				return queriedKeys;

			case IExpression.TYPE_EXISTS :
			case IExpression.TYPE_ALL :
				// We must evaluate the lhs to find the referenced keys
				//
				CollectionFilter cf = (CollectionFilter) targetExpr;
				Iterator<?> values;
				try {
					values = cf.getOperand().evaluateAsIterator(ctx);
				} catch (IllegalArgumentException e) {
					return null;
				}
				if (!values.hasNext())
					// No keys are requested but we know that an exists must
					// fail at this point. An all will however succeed regardless
					// of what is used as input.
					return type == IExpression.TYPE_ALL ? null : Boolean.FALSE;

				LambdaExpression lambda = cf.lambda;
				IEvaluationContext lambdaCtx = lambda.prolog(ctx);
				Variable lambdaVar = lambda.getItemVariable();
				IExpression filterExpr = lambda.getOperand();
				do {
					lambdaVar.setValue(lambdaCtx, values.next());
					queriedKeys = getQueriedIDs(lambdaCtx, variable, memberName, filterExpr, queriedKeys);
					if (queriedKeys == null)
						// No use continuing. The expression does not require the key
						return null;
				} while (values.hasNext());
				return queriedKeys;
		}
		return null;
	}

	private static Object safeEvaluate(IEvaluationContext ctx, IExpression expr) {
		try {
			return expr.evaluate(ctx);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
