/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.KeyWithLocale;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.index.IIndex;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;

public class Pipe extends NAry {

	private class NoIndexProvider implements IIndexProvider<Object> {
		private final IIndexProvider<?> indexProvider;
		private Everything<Object> everything;

		NoIndexProvider(IIndexProvider<?> indexProvider) { //
			this.indexProvider = indexProvider;
		}

		public IIndex<Object> getIndex(String memberName) {
			return null;
		}

		public Iterator<Object> everything() {
			return everything.getCopy();
		}

		public Object getManagedProperty(Object client, String memberName, Object key) {
			if (indexProvider != null)
				return indexProvider.getManagedProperty(client, memberName, key);
			if (client instanceof IInstallableUnit && memberName.equals(InstallableUnit.MEMBER_TRANSLATED_PROPERTIES)) {
				IInstallableUnit iu = (IInstallableUnit) client;
				return key instanceof KeyWithLocale ? iu.getProperty(((KeyWithLocale) key).getKey()) : iu.getProperty(key.toString());
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		void setEverything(Everything<?> everything) {
			this.everything = (Everything<Object>) everything;
		}
	}

	public static Expression createPipe(Expression[] operands) {
		// We expect two types of expressions. The ones that act on THIS
		// i.e. boolean match expressions or the ones that act EVERYTHING
		// by iterating a collection.
		//
		// Our task here is to convert all booleans into collections so
		// that:
		//  <boolean expression> becomes select(x | <boolean expression)
		//
		// If we find consecutive boolean expressions, we can actually
		// make one more optimization:
		//  <expr1>, <expr2> becomes select(x | <expr1> && <expr2>)

		IExpressionFactory factory = ExpressionUtil.getFactory();
		ArrayList<Expression> pipeables = new ArrayList<Expression>();
		ArrayList<Expression> booleans = new ArrayList<Expression>();
		VariableFinder finder = new VariableFinder(ExpressionFactory.EVERYTHING);
		for (int idx = 0; idx < operands.length; ++idx) {
			Expression operand = operands[idx];
			finder.reset();
			operand.accept(finder);
			if (finder.isFound()) {
				if (!booleans.isEmpty()) {
					// Concatenate all found booleans.
					pipeables.add(makePipeableOfBooleans(factory, booleans));
					booleans.clear();
				}
				pipeables.add(operand);
			} else
				booleans.add(operand);
		}

		if (!booleans.isEmpty()) {
			if (pipeables.isEmpty())
				return normalizeBoolean(factory, booleans);
			pipeables.add(makePipeableOfBooleans(factory, booleans));
		}
		int top = pipeables.size();
		if (top > 1)
			return new Pipe(pipeables.toArray(new Expression[top]));
		return (top == 1) ? pipeables.get(0) : Literal.TRUE_CONSTANT;
	}

	private static Expression normalizeBoolean(IExpressionFactory factory, ArrayList<Expression> booleans) {
		int top = booleans.size();
		Expression boolExpr;
		if (top > 1)
			boolExpr = (Expression) factory.and(booleans.toArray(new IExpression[top]));
		else if (top == 1)
			boolExpr = booleans.get(0);
		else
			boolExpr = Literal.TRUE_CONSTANT;
		return boolExpr;
	}

	private static Expression makePipeableOfBooleans(IExpressionFactory factory, ArrayList<Expression> booleans) {
		Expression boolExpr = normalizeBoolean(factory, booleans);
		Object[] params = null;
		if (boolExpr instanceof MatchExpression<?>) {
			MatchExpression<?> matchExpr = (MatchExpression<?>) boolExpr;
			boolExpr = (Expression) matchExpr.getPredicate();
			params = matchExpr.getParameters();
			if (params.length == 0)
				params = null;
		}
		Expression expr = (Expression) factory.select(ExpressionFactory.EVERYTHING, factory.lambda(ExpressionFactory.THIS, boolExpr));
		if (params != null)
			expr = new ContextExpression<Object>(expr, params);
		return expr;
	}

	private Pipe(Expression[] operands) {
		super(operands);
	}

	public int getExpressionType() {
		return TYPE_PIPE;
	}

	@Override
	public String getOperator() {
		return "pipe"; //$NON-NLS-1$
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		return evaluateAsIterator(context);
	}

	@Override
	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		Iterator<?> iterator = operands[0].evaluateAsIterator(context);
		if (operands.length == 0 || !iterator.hasNext())
			return iterator;

		Class<Object> elementClass = Object.class;
		Variable everything = ExpressionFactory.EVERYTHING;
		IEvaluationContext nextContext = EvaluationContext.create(context, everything);
		NoIndexProvider noIndexProvider = new NoIndexProvider(context.getIndexProvider());
		everything.setValue(nextContext, noIndexProvider);
		nextContext.setIndexProvider(noIndexProvider);
		for (int idx = 1; idx < operands.length; ++idx) {
			Expression expr = operands[idx];
			noIndexProvider.setEverything(new Everything<Object>(elementClass, iterator, expr));
			iterator = expr.evaluateAsIterator(nextContext);
			if (!iterator.hasNext())
				break;
		}
		return iterator;
	}

	@Override
	public int getPriority() {
		return PRIORITY_COLLECTION;
	}
}
