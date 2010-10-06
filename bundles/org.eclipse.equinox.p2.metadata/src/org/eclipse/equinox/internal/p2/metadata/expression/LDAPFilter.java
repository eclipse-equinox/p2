/*******************************************************************************
 * Copyright (c) 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *     IBM - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.util.Dictionary;
import java.util.Map;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

public class LDAPFilter extends Unary implements IFilterExpression {

	LDAPFilter(Expression expression) {
		super(expression);
	}

	public boolean accept(IExpressionVisitor visitor) {
		return operand.accept(visitor);
	}

	public boolean equals(Object o) {
		return (o instanceof Filter && !(o instanceof LDAPFilter)) ? equals(ExpressionUtil.parseLDAP(o.toString())) : super.equals(o);
	}

	@Override
	public String getOperator() {
		return operand.getOperator();
	}

	@Override
	public int getPriority() {
		return operand.getPriority();
	}

	public int getExpressionType() {
		return 0;
	}

	public boolean match(Map<String, ? extends Object> map) {
		return isMatch(MemberProvider.create(map, true));
	}

	@SuppressWarnings("rawtypes")
	public boolean match(Dictionary dictionary) {
		return isMatch(dictionary == null ? MemberProvider.emptyProvider() : MemberProvider.create(dictionary, true));
	}

	public boolean isMatch(Object candidate) {
		Variable self = ExpressionFactory.THIS;
		IEvaluationContext ctx = EvaluationContext.create(self);
		self.setValue(ctx, candidate);
		return Boolean.TRUE == operand.evaluate(ctx);
	}

	public boolean match(ServiceReference<?> reference) {
		return isMatch(reference == null ? MemberProvider.emptyProvider() : MemberProvider.create(reference, true));
	}

	public boolean matchCase(Map<String, ? extends Object> map) {
		return isMatch(map == null ? MemberProvider.emptyProvider() : MemberProvider.create(map, false));
	}

	public boolean matches(Map<String, ?> map) {
		return matchCase(map);
	}

	@SuppressWarnings("rawtypes")
	public boolean matchCase(Dictionary dictionary) {
		return isMatch(dictionary == null ? MemberProvider.emptyProvider() : MemberProvider.create(dictionary, false));
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		operand.toLDAPString(bld);
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		return null;
	}
}
