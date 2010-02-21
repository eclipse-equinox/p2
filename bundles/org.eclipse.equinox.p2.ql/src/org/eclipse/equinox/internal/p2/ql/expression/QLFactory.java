package org.eclipse.equinox.internal.p2.ql.expression;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.ql.IQLFactory;
import org.eclipse.equinox.p2.query.IQuery;

public class QLFactory extends ExpressionFactory implements IQLFactory, IQLConstants {
	@SuppressWarnings("hiding")
	public static final IQLFactory INSTANCE = new QLFactory();

	protected static final Map<String, Constructor<?>> functionMap;

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

	public IExpression at(IExpression target, IExpression key) {
		return new At((Expression) target, (Expression) key);
	}

	public IExpression intersect(IExpression c1, IExpression c2) {
		return new Intersect((Expression) c1, (Expression) c2);
	}

	public IExpression array(IExpression... operands) {
		return new Array(convertArray(operands));
	}

	public IExpression assignment(IExpression variable, IExpression expression) {
		return new Assignment((Variable) variable, (Expression) expression);
	}

	public IExpression collect(IExpression collection, IExpression lambda) {
		return new Collect((Expression) collection, (LambdaExpression) lambda);
	}

	public IExpression condition(IExpression test, IExpression ifTrue, IExpression ifFalse) {
		return new Condition((Expression) test, (Expression) ifTrue, (Expression) ifFalse);
	}

	public <T> org.eclipse.equinox.p2.metadata.expression.IContextExpression<T> contextExpression(IExpression expr, Object... parameters) {
		return new ContextExpression<T>((Expression) expr, parameters);
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

	public IExpression lambda(IExpression variable, IExpression[] assignments, IExpression body) {
		if (assignments.length == 0)
			return super.lambda(variable, body);
		Assignment[] asgns = new Assignment[assignments.length];
		System.arraycopy(assignments, 0, asgns, 0, assignments.length);
		return new CurryedLambdaExpression((Variable) variable, asgns, (Expression) body);
	}

	public IExpression latest(IExpression collection) {
		return new Latest((Expression) collection);
	}

	public IExpression limit(IExpression collection, int count) {
		return new Limit((Expression) collection, Literal.create(new Integer(count)));
	}

	public IExpression limit(IExpression collection, IExpression limit) {
		return new Limit((Expression) collection, (Expression) limit);
	}

	public IExpression matches(IExpression lhs, IExpression rhs) {
		return new Matches((Expression) lhs, (Expression) rhs);
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

	public IExpression union(IExpression c1, IExpression c2) {
		return new Union((Expression) c1, (Expression) c2);
	}

	public IExpression pipe(IExpression... operands) {
		return Pipe.createPipe(convertArray(operands));
	}

	public IExpression select(IExpression collection, IExpression lambda) {
		return new Select((Expression) collection, (LambdaExpression) lambda);
	}

	public IExpression traverse(IExpression collection, IExpression lambda) {
		return new Traverse((Expression) collection, (LambdaExpression) lambda);
	}

	public IExpression toExpression(IQuery<?> query) {
		Literal queryConstant = Literal.create(query);
		return new WrappedIQuery(new Expression[] {queryConstant});
	}

	public IExpression unique(IExpression collection, IExpression cache) {
		return new Unique((Expression) collection, (Expression) cache);
	}
}
