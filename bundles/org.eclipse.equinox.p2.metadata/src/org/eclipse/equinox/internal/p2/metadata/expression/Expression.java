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

import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.query.IQueryResult;

/**
 * The base class of the expression tree.
 */
public abstract class Expression implements IExpression, Comparable<Expression>, IExpressionConstants {

	static final Expression[] emptyArray = new Expression[0];

	public static void appendOperand(StringBuffer bld, Variable rootVariable, Expression operand, int priority) {
		if (priority < operand.getPriority()) {
			bld.append('(');
			operand.toString(bld, rootVariable);
			bld.append(')');
		} else
			operand.toString(bld, rootVariable);
	}

	public static Expression[] assertLength(Expression[] operands, int minLength, int maxLength, String operand) {
		if (operands == null)
			operands = emptyArray;
		if (operands.length < minLength)
			throw new IllegalArgumentException("Not enough operands for " + operand); //$NON-NLS-1$
		if (operands.length > maxLength)
			throw new IllegalArgumentException("Too many operands for " + operand); //$NON-NLS-1$
		return operands;
	}

	public static Expression[] assertLength(Expression[] operands, int length, String operand) {
		if (operands == null)
			operands = emptyArray;
		if (operands.length < length)
			throw new IllegalArgumentException("Not enough operands for " + operand); //$NON-NLS-1$
		return operands;
	}

	public static int compare(Expression[] arr1, Expression[] arr2) {
		int max = arr1.length;
		if (max > arr2.length)
			max = arr2.length;
		for (int idx = 0; idx < max; ++idx) {
			int cmp = arr1[idx].compareTo(arr2[idx]);
			if (cmp != 0)
				return cmp;
		}
		if (max == arr2.length) {
			if (max < arr1.length)
				return 1;
			return 0;
		}
		return -1;
	}

	public static boolean equals(Expression[] arr1, Expression[] arr2) {
		int idx = arr1.length;
		if (idx != arr2.length)
			return false;
		while (--idx >= 0)
			if (!arr1[idx].equals(arr2[idx]))
				return false;
		return true;
	}

	public static int hashCode(Expression[] arr) {
		int idx = arr.length;
		int result = 1;
		while (--idx >= 0)
			result = 31 * result + arr[idx].hashCode();
		return result;
	}

	public static void elementsToString(StringBuffer bld, Variable rootVariable, Expression[] elements) {
		int top = elements.length;
		if (top > 0) {
			elements[0].toString(bld, rootVariable);
			for (int idx = 1; idx < top; ++idx) {
				bld.append(", "); //$NON-NLS-1$
				appendOperand(bld, rootVariable, elements[idx], PRIORITY_MAX);
			}
		}
	}

	public static List<String> getIndexCandidateMembers(Class<?> elementClass, Variable itemVariable, Expression operand) {
		MembersFinder finder = new MembersFinder(elementClass, itemVariable);
		operand.accept(finder);
		return finder.getMembers();
	}

	/**
	 * Let the visitor visit this instance and all expressions that this
	 * instance contains.
	 * @param visitor The visiting visitor.
	 * @return <code>true</code> if the visitor should continue visiting, <code>false</code> otherwise.
	 */
	public boolean accept(IExpressionVisitor visitor) {
		return visitor.visit(this);
	}

	public int compareTo(Expression e) {
		int cmp = getPriority() - e.getPriority();
		if (cmp == 0) {
			int e1 = getExpressionType();
			int e2 = e.getExpressionType();
			cmp = e1 > e2 ? 1 : (e1 == e2 ? 0 : -1);
		}
		return cmp;
	}

	public boolean equals(Object e) {
		if (e == this)
			return true;
		if (e == null || getClass() != e.getClass())
			return false;
		return getExpressionType() == ((Expression) e).getExpressionType();
	}

	/**
	 * Evaluate this expression with given context and variables.
	 * @param context The evaluation context
	 * @return The result of the evaluation.
	 */
	public abstract Object evaluate(IEvaluationContext context);

	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		Object value = evaluate(context);
		if (!(value instanceof Iterator<?>))
			value = RepeatableIterator.create(value);
		return (Iterator<?>) value;
	}

	public abstract String getOperator();

	public abstract int getPriority();

	public boolean isReferenceTo(Variable variable) {
		return this == variable;
	}

	public final String toLDAPString() {
		StringBuffer bld = new StringBuffer();
		toLDAPString(bld);
		return bld.toString();
	}

	public void toLDAPString(StringBuffer buf) {
		throw new UnsupportedOperationException();
	}

	public final String toString() {
		StringBuffer bld = new StringBuffer();
		toString(bld);
		return bld.toString();
	}

	public void toString(StringBuffer bld) {
		toString(bld, ExpressionFactory.THIS);
	}

	public abstract void toString(StringBuffer bld, Variable rootVariable);

	private static class Compacter {
		private Expression base;

		private List<Expression> parts;

		private int op;

		Compacter(Expression base, int op) {
			this.base = base;
			this.op = op;
		}

		Expression getResultingFilter() {
			if (parts == null)
				return base;

			int partsOp = op == TYPE_AND ? TYPE_OR : TYPE_AND;
			return addFilter(base, normalize(parts, partsOp), op);
		}

		boolean merge(Expression b) {
			Expression[] aArr;
			Expression[] bArr;
			if (base.getExpressionType() == op)
				aArr = getFilterImpls(base);
			else
				aArr = new Expression[] {base};

			if (b.getExpressionType() == op)
				bArr = getFilterImpls(b);
			else
				bArr = new Expression[] {b};

			List<Expression> common = null;
			List<Expression> onlyA = null;

			int atop = aArr.length;
			int btop = bArr.length;
			int aidx;
			int bidx;
			for (aidx = 0; aidx < atop; ++aidx) {
				Expression af = aArr[aidx];
				for (bidx = 0; bidx < btop; ++bidx) {
					Expression bf = bArr[bidx];
					if (af.equals(bf)) {
						if (common == null)
							common = new ArrayList<Expression>();
						common.add(af);
						break;
					}
				}
				if (bidx == btop) {
					if (onlyA == null)
						onlyA = new ArrayList<Expression>();
					onlyA.add(af);
				}
			}
			if (common == null)
				// Nothing in common
				return false;

			if (onlyA == null && parts == null)
				return true;

			List<Expression> onlyB = null;
			for (bidx = 0; bidx < btop; ++bidx) {
				Expression bf = bArr[bidx];
				for (aidx = 0; aidx < atop; ++aidx)
					if (bf.equals(aArr[aidx]))
						break;
				if (aidx == atop) {
					if (onlyB == null)
						onlyB = new ArrayList<Expression>();
					onlyB.add(bf);
				}
			}

			if (onlyB == null && parts == null) {
				// All of B is already covered by base
				base = b;
				return true;
			}

			if (parts == null)
				parts = new ArrayList<Expression>();

			if (onlyA != null) {
				base = normalize(common, op);
				Expression af = normalize(onlyA, op);
				if (!parts.contains(af))
					parts.add(af);
			}
			Expression bf = normalize(onlyB, op);
			if (!parts.contains(bf))
				parts.add(bf);
			return true;
		}
	}

	public static class VariableFinder implements IExpressionVisitor {
		private boolean found = false;
		private final Variable variable;

		public VariableFinder(Variable variable) {
			this.variable = variable;
		}

		public boolean visit(IExpression expression) {
			if (((Expression) expression).isReferenceTo(variable))
				found = true;
			return !found;
		}

		public void reset() {
			found = false;
		}

		public boolean isFound() {
			return found;
		}
	}

	private static class MembersFinder implements IExpressionVisitor {
		private List<String> members;
		private final Class<?> elementClass;
		private final IExpression operand;

		MembersFinder(Class<?> elementClass, IExpression operand) {
			this.elementClass = elementClass;
			this.operand = operand;
		}

		public boolean visit(IExpression expression) {
			if (expression instanceof Matches) {
				if (IInstallableUnit.class.isAssignableFrom(elementClass)) {
					// This one is a bit special since an
					// IInstallableUnit ~= IRequirement often
					// means that we can reuse the requirement
					// expression.
					Matches matches = (Matches) expression;
					if (matches.lhs == operand) {
						if (members == null)
							members = new ArrayList<String>();
						if (!members.contains(InstallableUnit.MEMBER_PROVIDED_CAPABILITIES))
							members.add(InstallableUnit.MEMBER_PROVIDED_CAPABILITIES);
					}
				}

				// No point in scanning for more index candidates in a matches expression
				return false;
			}

			if (expression instanceof Member) {
				Member member = (Member) expression;
				if (member.getOperand() == operand) {
					String name = member.getName();
					if (members == null)
						members = new ArrayList<String>();
					if (!members.contains(name))
						members.add(member.getName());
					return false;
				}
			}
			return true;
		}

		List<String> getMembers() {
			return members == null ? Collections.<String> emptyList() : members;
		}
	}

	static Expression addFilter(Expression base, Expression subFilter, int expressionType) {
		if (base.equals(subFilter))
			return base;

		ArrayList<Expression> filters = new ArrayList<Expression>(2);
		filters.add(base);
		filters.add(subFilter);
		return normalize(filters, expressionType);
	}

	static Expression normalize(List<Expression> operands, int op) {
		int top = operands.size();
		if (top == 1)
			return operands.get(0);

		// a | (b | c) becomes a | b | c
		// a & (b & c) becomes a & b & c
		//
		for (int idx = 0; idx < top; ++idx) {
			Expression f = operands.get(idx);
			if (f.getExpressionType() != op)
				continue;

			Expression[] sfs = getFilterImpls(f);
			operands.remove(idx);
			--top;
			for (int ndx = 0; ndx < sfs.length; ++ndx) {
				Expression nf = sfs[ndx];
				if (!operands.contains(nf))
					operands.add(nf);
			}
		}
		top = operands.size();
		if (top == 1)
			return operands.get(0);

		Collections.sort(operands);
		List<Compacter> splits = new ArrayList<Compacter>();
		int reverseOp = op == TYPE_AND ? TYPE_OR : TYPE_AND;

		for (int idx = 0; idx < top; ++idx)
			merge(splits, operands.get(idx), reverseOp);

		operands.clear();
		top = splits.size();
		for (int idx = 0; idx < top; ++idx) {
			Expression filter = splits.get(idx).getResultingFilter();
			if (!operands.contains(filter))
				operands.add(filter);
		}
		top = operands.size();
		if (top == 1)
			return operands.get(0);

		Collections.sort(operands);
		Expression[] expArray = operands.toArray(new Expression[top]);
		return op == TYPE_AND ? new And(expArray) : new Or(expArray);
	}

	static void merge(List<Compacter> splits, Expression base, int op) {
		int top = splits.size();
		for (int idx = 0; idx < top; ++idx) {
			Compacter split = splits.get(idx);
			if (split.merge(base))
				return;
		}
		splits.add(new Compacter(base, op));
	}

	static Expression[] getFilterImpls(Expression expression) {
		if (expression instanceof NAry)
			return ((NAry) expression).operands;
		throw new IllegalArgumentException();
	}

	static Set<?> asSet(Object val, boolean forcePrivateCopy) {
		if (val == null)
			throw new IllegalArgumentException("Cannot convert null into an set"); //$NON-NLS-1$

		if (val instanceof IRepeatableIterator<?>) {
			Object provider = ((IRepeatableIterator<?>) val).getIteratorProvider();
			if (!forcePrivateCopy) {
				if (provider instanceof Set<?>)
					return (Set<?>) provider;
				if (provider instanceof IQueryResult<?>)
					return ((IQueryResult<?>) provider).toUnmodifiableSet();
			}

			if (provider instanceof Collection<?>)
				val = provider;
		} else {
			if (!forcePrivateCopy) {
				if (val instanceof Set<?>)
					return (Set<?>) val;
				if (val instanceof IQueryResult<?>)
					return ((IQueryResult<?>) val).toUnmodifiableSet();
			}
		}

		HashSet<Object> result;
		if (val instanceof Collection<?>)
			result = new HashSet<Object>((Collection<?>) val);
		else {
			result = new HashSet<Object>();
			Iterator<?> iterator = RepeatableIterator.create(val);
			while (iterator.hasNext())
				result.add(iterator.next());
		}
		return result;
	}

	private static class TranslationSupportFinder implements IExpressionVisitor {
		private boolean found;

		TranslationSupportFinder() { //
		}

		public boolean visit(IExpression expression) {
			if (expression.getExpressionType() == TYPE_MEMBER && InstallableUnit.MEMBER_TRANSLATED_PROPERTIES.equals(((Member) expression).getName()))
				found = true;
			return !found;
		}

		boolean isFound() {
			return found;
		}
	}

	/**
	 * Checks if the expression will make repeated requests for the 'everything' iterator.
	 * @return <code>true</code> if repeated requests will be made, <code>false</code> if not.
	 */
	public boolean needsTranslationSupport() {
		TranslationSupportFinder tsFinder = new TranslationSupportFinder();
		accept(tsFinder);
		return tsFinder.isFound();
	}

	int countAccessToEverything() {
		return 0;
	}
}
