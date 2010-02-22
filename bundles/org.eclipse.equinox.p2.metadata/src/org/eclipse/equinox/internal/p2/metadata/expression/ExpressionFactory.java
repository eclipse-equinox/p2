package org.eclipse.equinox.internal.p2.metadata.expression;

import java.util.List;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.query.IQuery;

public class ExpressionFactory implements IExpressionFactory, IExpressionConstants {
	public static final IExpressionFactory INSTANCE = new ExpressionFactory();
	public static final Variable THIS = new Variable(VARIABLE_THIS);
	public static final Variable EVERYTHING = new Variable(VARIABLE_EVERYTHING);

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

	public IExpression at(IExpression target, IExpression key) {
		return new At((Expression) target, (Expression) key);
	}

	@SuppressWarnings("unchecked")
	public IExpression normalize(List<? extends IExpression> operands, int expressionType) {
		return Expression.normalize((List<Expression>) operands, expressionType);
	}

	public IExpression constant(Object value) {
		return Literal.create(value);
	}

	public IEvaluationContext createContext(Object... parameters) {
		return EvaluationContext.create(parameters, (Variable[]) null);
	}

	public IEvaluationContext createContext(IExpression[] variables, Object... parameters) {
		return EvaluationContext.create(parameters, variables);
	}

	public <T> IContextExpression<T> contextExpression(IExpression expr, Object... parameters) {
		return new ContextExpression<T>((Expression) expr, parameters);
	}

	public IFilterExpression filterExpression(IExpression expression) {
		return new LDAPFilter((Expression) expression);
	}

	public IExpression equals(IExpression lhs, IExpression rhs) {
		return new Equals((Expression) lhs, (Expression) rhs, false);
	}

	public IExpression exists(IExpression collection, IExpression lambda) {
		return new Exists((Expression) collection, (LambdaExpression) lambda);
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

	public IExpression lambda(IExpression variable, IExpression body) {
		return new LambdaExpression((Variable) variable, (Expression) body);
	}

	public IExpression latest(IExpression collection) {
		throw failNoQL();
	}

	public IExpression less(IExpression lhs, IExpression rhs) {
		return new Compare((Expression) lhs, (Expression) rhs, true, false);
	}

	public IExpression lessEqual(IExpression lhs, IExpression rhs) {
		return new Compare((Expression) lhs, (Expression) rhs, true, true);
	}

	public IExpression limit(IExpression collection, int count) {
		throw failNoQL();
	}

	public IExpression limit(IExpression collection, IExpression limit) {
		throw failNoQL();
	}

	public IExpression matches(IExpression lhs, IExpression rhs) {
		return new Matches((Expression) lhs, (Expression) rhs);
	}

	public <T> IMatchExpression<T> matchExpression(IExpression expression, Object... parameters) {
		return new MatchExpression<T>((Expression) expression, parameters);
	}

	public IExpression member(IExpression target, String name) {
		if ("empty".equals(name)) //$NON-NLS-1$
			return new Member.EmptyMember((Expression) target);
		if ("length".equals(name)) //$NON-NLS-1$
			return new Member.LengthMember((Expression) target);
		return new Member.DynamicMember((Expression) target, name);
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
		throw failNoQL();
	}

	public IExpression select(IExpression collection, IExpression lambda) {
		throw failNoQL();
	}

	public IExpression thisVariable() {
		return THIS;
	}

	public IExpression toExpression(IQuery<?> query) {
		throw failNoQL();
	}

	public IExpression variable(String name) {
		if (VARIABLE_EVERYTHING.equals(name))
			return EVERYTHING;
		if (VARIABLE_THIS.equals(name))
			return THIS;
		return new Variable(name);
	}

	private static UnsupportedOperationException failNoQL() {
		return new UnsupportedOperationException("org.eclipse.equinox.p2.ql not installed"); //$NON-NLS-1$		
	}
}
