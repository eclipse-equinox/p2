package org.eclipse.equinox.internal.p2.ql.expression;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.eclipse.equinox.internal.p2.ql.parser.IParserConstants;
import org.eclipse.equinox.p2.ql.*;

public class ExpressionFactory implements IExpressionFactory, IParserConstants {
	public static final IExpressionFactory INSTANCE = new ExpressionFactory();

	private static final Map functionMap;

	static {
		Class[] args = new Class[] {Expression[].class};
		Map f = new HashMap();
		try {
			f.put(KEYWORD_BOOLEAN, BooleanFunction.class.getConstructor(args));
			f.put(KEYWORD_FILTER, FilterFunction.class.getConstructor(args));
			f.put(KEYWORD_VERSION, VersionFunction.class.getConstructor(args));
			f.put(KEYWORD_RANGE, RangeFunction.class.getConstructor(args));
			f.put(KEYWORD_SET, SetFunction.class.getConstructor(args));
			f.put(KEYWORD_CLASS, ClassFunction.class.getConstructor(args));
			f.put(KEYWORD_IQUERY, WrappedIQuery.class.getConstructor(args));
			f.put(KEYWORD_CAPABILITY_INDEX, CapabilityIndexFunction.class.getConstructor(args));
			functionMap = Collections.unmodifiableMap(f);
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static Expression[] convertArray(IExpression[] operands) {
		Expression[] ops = new Expression[operands.length];
		System.arraycopy(operands, 0, ops, 0, operands.length);
		return ops;
	}

	private ExpressionFactory() {
		// Maintain singleton
	}

	public IExpression all(IExpression collection, IExpression lambda) {
		return new All((Expression) collection, (LambdaExpression) lambda);
	}

	public IExpression and(IExpression[] operands) {
		return new And(convertArray(operands));
	}

	public IExpression array(IExpression[] operands) {
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
		return Constant.create(value);
	}

	public IContextExpression contextExpression(IExpression expr) {
		return new ContextExpression((Expression) expr);
	}

	public IExpression dynamicMember(IExpression target, String name) {
		return new Member.DynamicMember((Expression) target, name);
	}

	public IExpression equals(IExpression lhs, IExpression rhs) {
		return new Equals((Expression) lhs, (Expression) rhs, false);
	}

	public IExpression exists(IExpression collection, IExpression lambda) {
		return new Exists((Expression) collection, (LambdaExpression) lambda);
	}

	public IExpression first(IExpression collection, IExpression lambda) {
		return new First((Expression) collection, (LambdaExpression) lambda);
	}

	public IExpression flatten(IExpression collection) {
		return new Flatten((Expression) collection);
	}

	public IExpression function(Object function, IExpression[] args) {
		try {
			return (IExpression) ((Constructor) function).newInstance(new Object[] {convertArray(args)});
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

	public Map getFunctionMap() {
		return functionMap;
	}

	public IExpression greater(IExpression lhs, IExpression rhs) {
		return new Compare((Expression) lhs, (Expression) rhs, false, false);
	}

	public IExpression indexedParameter(int index) {
		return new Parameter.Indexed(index);
	}

	public IExpression keyedParameter(String key) {
		return new Parameter.Keyed(key);
	}

	public IExpression lambda(IExpression variable, IExpression body) {
		return new LambdaExpression((Variable) variable, (Expression) body);
	}

	public IExpression lambda(IExpression variable, IExpression body, IExpression[] assignments) {
		Assignment[] asgns = new Assignment[assignments.length];
		System.arraycopy(assignments, 0, asgns, 0, assignments.length);
		return new LambdaExpression((Variable) variable, (Expression) body, asgns);
	}

	public IExpression latest(IExpression collection) {
		return new Latest((Expression) collection);
	}

	public IExpression less(IExpression lhs, IExpression rhs) {
		return new Compare((Expression) lhs, (Expression) rhs, true, false);
	}

	public IExpression limit(IExpression collection, IExpression limit) {
		return new Limit((Expression) collection, (Expression) limit);
	}

	public IExpression matches(IExpression lhs, IExpression rhs) {
		return new Matches((Expression) lhs, (Expression) rhs);
	}

	public IMatchExpression matchExpression(IExpression expr) {
		return new MatchExpression((Expression) expr);
	}

	public IExpression member(IExpression target, String name) {
		return new Member.DynamicMember((Expression) target, name);
	}

	public IExpression memberCall(IExpression target, String name, IExpression[] args) {
		if (args.length == 0)
			return member(target, name);

		Expression[] eargs = convertArray(args);
		if (KEYWORD_SATISFIES_ANY.equals(name))
			return new Member.CapabilityIndex_satisfiesAny((Expression) target, eargs);
		if (KEYWORD_SATISFIES_ALL.equals(name))
			return new Member.CapabilityIndex_satisfiesAll((Expression) target, eargs);

		StringBuffer bld = new StringBuffer();
		bld.append("Don't know how to do a member call with "); //$NON-NLS-1$
		bld.append(name);
		bld.append('(');
		Array.elementsToString(bld, eargs);
		bld.append(')');
		throw new IllegalArgumentException(bld.toString());
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

	public IExpression or(IExpression[] operands) {
		return new Or(convertArray(operands));
	}

	public IExpression pipe(IExpression[] operands) {
		if (operands.length == 0)
			return null;

		Expression pipe = (Expression) operands[0];
		for (int idx = 1; idx < operands.length; ++idx)
			pipe = ((Expression) operands[idx]).pipeFrom(pipe);
		return pipe;
	}

	public IExpression select(IExpression collection, IExpression lambda) {
		return new Select((Expression) collection, (LambdaExpression) lambda);
	}

	public IExpression traverse(IExpression collection, IExpression lambda) {
		return new Traverse((Expression) collection, (LambdaExpression) lambda);
	}

	public IExpression unique(IExpression collection, IExpression cache) {
		return new Unique((Expression) collection, (Expression) cache);
	}

	public IExpression variable(String name) {
		return Variable.create(name);
	}
}
