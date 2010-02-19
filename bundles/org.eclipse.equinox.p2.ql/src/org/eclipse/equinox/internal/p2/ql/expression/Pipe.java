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

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.equinox.internal.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.index.IIndex;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.ql.IQLExpression;
import org.eclipse.equinox.p2.ql.IQLFactory;

public class Pipe extends NAry implements IQLExpression {

	private class NoIndexProvider implements IIndexProvider<Object> {
		private Iterator<Object> everything;

		NoIndexProvider() { //
		}

		public IIndex<Object> getIndex(String memberName) {
			return null;
		}

		public Iterator<Object> everything() {
			return everything;
		}

		@SuppressWarnings("unchecked")
		void setEverything(Iterator<?> everything) {
			this.everything = (Iterator<Object>) everything;
		}
	}

	private static Expression[] makePipeable(Expression[] operands) {
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
					pipeables.add(makePipeableOfBooleans(booleans));
					booleans.clear();
				}
				pipeables.add(operand);
			} else
				booleans.add(operand);
		}
		if (!booleans.isEmpty())
			pipeables.add(makePipeableOfBooleans(booleans));
		return pipeables.toArray(new Expression[pipeables.size()]);
	}

	private static Expression makePipeableOfBooleans(ArrayList<Expression> booleans) {
		IQLFactory factory = (IQLFactory) ExpressionUtil.getFactory();
		Expression boolExpr = booleans.get(0);
		int top = booleans.size();
		if (top > 1)
			boolExpr = (Expression) factory.and(booleans.toArray(new IExpression[top]));
		return (Expression) factory.select(ExpressionFactory.EVERYTHING, factory.lambda(ExpressionFactory.THIS, boolExpr));
	}

	protected Pipe(Expression[] operands) {
		super(makePipeable(assertLength(operands, 2, "pipe"))); //$NON-NLS-1$
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
		NoIndexProvider noIndexProvider = new NoIndexProvider();
		nextContext.setIndexProvider(noIndexProvider);
		for (int idx = 1; idx < operands.length; ++idx) {
			Expression expr = operands[idx];
			noIndexProvider.setEverything(iterator);
			everything.setValue(nextContext, new Everything<Object>(elementClass, noIndexProvider));
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
