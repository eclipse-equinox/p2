/*******************************************************************************
 * Copyright (c) 2011, 2016 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.query.IQuery;

public class ExpressionFactory implements IExpressionFactory, IExpressionConstants {
	public static final Variable EVERYTHING = new Variable(VARIABLE_EVERYTHING);
	protected static final Map<String, Constructor<?>> functionMap;
	public static final IExpressionFactory INSTANCE = new ExpressionFactory();

	public static final Variable THIS = new Variable(VARIABLE_THIS);

	static {
		Class<?>[] args = new Class[] {Expression[].class};
		Map<String, Constructor<?>> f = new HashMap<String, Constructor<?>>();
		try {
			f.put(KEYWORD_BOOLEAN, BooleanFunction.class.getConstructor(args));
			f.put(KEYWORD_FILTER, FilterFunction.class.getConstructor(args));
			f.put(KEYWORD_VERSION, VersionFunction.class.getConstructor(args));
			f.put(KEYWORD_RANGE, RangeFunction.class.getConstructor(args));
			f.put(KEYWORD_CLASS, ClassFunction.class.getConstructor(args));
			f.put(KEYWORD_SET, SetFunction.class.getConstructor(args));
			f.put(KEYWORD_IQUERY, WrappedIQuery.class.getConstructor(args));
			functionMap = Collections.unmodifiableMap(f);
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	protected static Expression[] convertArray(IExpression[] operands) {
		Expression[] ops = new Expression[operands.length];
		System.arraycopy(operands, 0, ops, 0, operands.length);
		return ops;
	}

	public IExpression all(IExpression collection, IExpression lambda) {
		return new All((Expression) collection, (LambdaExpression) lambda);
	}

	public IExpression and(IExpression... operands) {
		if (operands.length == 0)
			return Literal.TRUE_CONSTANT;
		if (operands.length == 1)
			return operands[0];
		return new And(convertArray(operands));
	}

	public IExpression array(IExpression... operands) {
		return new Array(convertArray(operands));
	}

	public IExpression assignment(IExpression variable, IExpression expression) {
		return new Assignment((Variable) variable, (Expression) expression);
	}

	public IExpression at(IExpression target, IExpression key) {
		return new At((Expression) target, (Expression) key);
	}

	public IExpression collect(IExpression collection, IExpression lambda) {
		return new Collect((Expression) collection, (LambdaExpression) lambda);
	}

	public IExpression condition(IExpression test, IExpression ifTrue, IExpression ifFalse) {
		return new Condition((Expression) test, (Expression) ifTrue, (Expression) ifFalse);
	}

	public IExpression constant(Object value) {
		return Literal.create(value);
	}

	@SuppressWarnings("unchecked")
	public <T> IContextExpression<T> contextExpression(IExpression expression, Object... parameters) {

		if (expression instanceof IContextExpression<?>) {
			if (parameters.length > 0)
				// Not good.
				throw new IllegalArgumentException("IContextExpression cannot be parameterized (it already is)"); //$NON-NLS-1$
			return (IContextExpression<T>) expression;
		}
		if (expression instanceof IMatchExpression<?>)
			throw new IllegalArgumentException("IMatchExpression cannot be turned into a context expression"); //$NON-NLS-1$
		return new ContextExpression<T>((Expression) expression, parameters);
	}

	public IEvaluationContext createContext(IExpression[] variables, Object... parameters) {
		return EvaluationContext.create(parameters, variables);
	}

	public IEvaluationContext createContext(Object... parameters) {
		return EvaluationContext.create(parameters, (Variable[]) null);
	}

	public IExpression equals(IExpression lhs, IExpression rhs) {
		return new Equals((Expression) lhs, (Expression) rhs, false);
	}

	public IExpression exists(IExpression collection, IExpression lambda) {
		return new Exists((Expression) collection, (LambdaExpression) lambda);
	}

	public IFilterExpression filterExpression(IExpression expression) {
		return new LDAPFilter((Expression) expression);
	}

	public IExpression first(IExpression collection, IExpression lambda) {
		return new First((Expression) collection, (LambdaExpression) lambda);
	}

	public IExpression flatten(IExpression collection) {
		return new Flatten((Expression) collection);
	}

	public IExpression function(Object function, IExpression... args) {
		try {
			return (IExpression) ((Constructor<?>) function).newInstance(new Object[] {convertArray(args)});
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (InvocationTargetException e) {
			Throwable t = e.getCause();
			if (t instanceof RuntimeException)
				throw (RuntimeException) t;
			throw new RuntimeException(t);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public Map<String, ? extends Object> getFunctionMap() {
		return functionMap;
	}

	public IExpression greater(IExpression lhs, IExpression rhs) {
		return new Compare((Expression) lhs, (Expression) rhs, false, false);
	}

	public IExpression greaterEqual(IExpression lhs, IExpression rhs) {
		return new Compare((Expression) lhs, (Expression) rhs, false, true);
	}

	public IExpression indexedParameter(int index) {
		return new Parameter(index);
	}

	public IExpression intersect(IExpression c1, IExpression c2) {
		return new Intersect((Expression) c1, (Expression) c2);
	}

	public IExpression lambda(IExpression variable, IExpression body) {
		return new LambdaExpression((Variable) variable, (Expression) body);
	}

	public IExpression lambda(IExpression variable, IExpression[] assignments, IExpression body) {
		if (assignments.length == 0)
			return lambda(variable, body);
		Assignment[] asgns = new Assignment[assignments.length];
		System.arraycopy(assignments, 0, asgns, 0, assignments.length);
		return new CurryedLambdaExpression((Variable) variable, asgns, (Expression) body);
	}

	public IExpression latest(IExpression collection) {
		return new Latest((Expression) collection);
	}

	public IExpression less(IExpression lhs, IExpression rhs) {
		return new Compare((Expression) lhs, (Expression) rhs, true, false);
	}

	public IExpression lessEqual(IExpression lhs, IExpression rhs) {
		return new Compare((Expression) lhs, (Expression) rhs, true, true);
	}

	public IExpression limit(IExpression collection, IExpression limit) {
		return new Limit((Expression) collection, (Expression) limit);
	}

	public IExpression limit(IExpression collection, int count) {
		return new Limit((Expression) collection, Literal.create(Integer.valueOf(count)));
	}

	public IExpression matches(IExpression lhs, IExpression rhs) {
		return new Matches((Expression) lhs, (Expression) rhs);
	}

	@SuppressWarnings("unchecked")
	public <T> IMatchExpression<T> matchExpression(IExpression expression, Object... parameters) {
		if (expression instanceof IMatchExpression<?>) {
			if (parameters.length > 0)
				// Not good.
				throw new IllegalArgumentException("IMatchExpression cannot be parameterized (it already is)"); //$NON-NLS-1$
			return (IMatchExpression<T>) expression;
		}
		if (expression instanceof IContextExpression<?>)
			throw new IllegalArgumentException("IContextExpression cannot be turned into a match expression"); //$NON-NLS-1$
		return new MatchExpression<T>((Expression) expression, parameters);
	}

	public IExpression member(IExpression target, String name) {
		if ("empty".equals(name)) //$NON-NLS-1$
			return new Member.EmptyMember((Expression) target);
		if ("length".equals(name)) //$NON-NLS-1$
			return new Member.LengthMember((Expression) target);
		return new Member.DynamicMember((Expression) target, name);
	}

	public IExpression memberCall(IExpression target, String name, IExpression... args) {
		if (args.length == 0)
			return member(target, name);

		Expression[] eargs = convertArray(args);

		// Insert functions that takes arguments here

		//
		StringBuffer bld = new StringBuffer();
		bld.append("Don't know how to do a member call with "); //$NON-NLS-1$
		bld.append(name);
		bld.append('(');
		Expression.elementsToString(bld, null, eargs);
		bld.append(')');
		throw new IllegalArgumentException(bld.toString());
	}

	@SuppressWarnings("unchecked")
	public IExpression normalize(List<? extends IExpression> operands, int expressionType) {
		return Expression.normalize((List<Expression>) operands, expressionType);
	}

	public IExpression not(IExpression operand) {
		if (operand instanceof Equals) {
			Equals eq = (Equals) operand;
			return new Equals(eq.lhs, eq.rhs, !eq.negate);
		}
		if (operand instanceof Compare) {
			Compare cmp = (Compare) operand;
			return new Compare(cmp.lhs, cmp.rhs, !cmp.compareLess, !cmp.equalOK);
		}
		if (operand instanceof Not)
			return ((Not) operand).operand;

		return new Not((Expression) operand);
	}

	public IExpression or(IExpression... operands) {
		if (operands.length == 0)
			return Literal.TRUE_CONSTANT;
		if (operands.length == 1)
			return operands[0];
		return new Or(convertArray(operands));
	}

	public IExpression pipe(IExpression... operands) {
		return Pipe.createPipe(convertArray(operands));
	}

	public IExpression select(IExpression collection, IExpression lambda) {
		return new Select((Expression) collection, (LambdaExpression) lambda);
	}

	public IExpression thisVariable() {
		return THIS;
	}

	public IExpression toExpression(IQuery<?> query) {
		Literal queryConstant = Literal.create(query);
		return new WrappedIQuery(new Expression[] {queryConstant});
	}

	public IExpression traverse(IExpression collection, IExpression lambda) {
		return new Traverse((Expression) collection, (LambdaExpression) lambda);
	}

	public IExpression union(IExpression c1, IExpression c2) {
		return new Union((Expression) c1, (Expression) c2);
	}

	public IExpression unique(IExpression collection, IExpression cache) {
		return new Unique((Expression) collection, (Expression) cache);
	}

	public IExpression variable(String name) {
		if (VARIABLE_EVERYTHING.equals(name))
			return EVERYTHING;
		if (VARIABLE_THIS.equals(name))
			return THIS;
		return new Variable(name);
	}
}
