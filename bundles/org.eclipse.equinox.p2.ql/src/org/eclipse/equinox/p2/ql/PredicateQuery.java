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
package org.eclipse.equinox.p2.ql;

import java.util.Map;
import org.eclipse.equinox.internal.p2.ql.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IVersionedId;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.MatchQuery;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

/**
 * An IQuery implementation that is based on the p2 query language.
 */
public class PredicateQuery extends MatchQuery {
	private static final ExpressionParser parser = new ExpressionParser();

	private final ExpressionContext context;
	private final VariableScope scope;
	private final ItemExpression expression;

	public PredicateQuery(Class instanceClass, ItemExpression expression, Object[] parameters) {
		this.expression = expression;
		this.context = new ExpressionContext(instanceClass, parameters, null);
		this.scope = expression.defineScope();
	}

	public PredicateQuery(Class instanceClass, String expression) {
		this(instanceClass, parser.parsePredicate(expression), null);
	}

	public PredicateQuery(Class instanceClass, String expression, Object param1) {
		this(instanceClass, parser.parsePredicate(expression), new Object[] {param1});
	}

	public PredicateQuery(Class instanceClass, String expression, Object param1, Object param2) {
		this(instanceClass, parser.parsePredicate(expression), new Object[] {param1, param2});
	}

	public PredicateQuery(Class instanceClass, String expression, Object param1, Object param2, Object param3) {
		this(instanceClass, parser.parsePredicate(expression), new Object[] {param1, param2, param3});
	}

	public PredicateQuery(ItemExpression expression, Map args) {
		this(IVersionedId.class, expression, new Object[] {args});
	}

	public PredicateQuery(ItemExpression expr, Object[] objects) {
		this(IVersionedId.class, expr, objects);
	}

	public PredicateQuery(String expression) {
		this(IVersionedId.class, parser.parsePredicate(expression), null);
	}

	public PredicateQuery(String expression, Object param1) {
		this(IVersionedId.class, parser.parsePredicate(expression), new Object[] {param1});
	}

	public PredicateQuery(String expression, Object param1, Object param2) {
		this(IVersionedId.class, parser.parsePredicate(expression), new Object[] {param1, param2});
	}

	public PredicateQuery(String expression, Object param1, Object param2, Object param3) {
		this(IVersionedId.class, parser.parsePredicate(expression), new Object[] {param1, param2, param3});
	}

	public Object getProperty(String key) {
		if (IArtifactRepository.QUERY_EXCLUDE_KEYS.equals(key))
			return Boolean.valueOf(!context.getInstanceClass().isAssignableFrom(IArtifactKey.class));

		if (IArtifactRepository.QUERY_EXCLUDE_DESCRIPTORS.equals(key))
			return Boolean.valueOf(!context.getInstanceClass().isAssignableFrom(IArtifactDescriptor.class));

		return super.getProperty(key);
	}

	public boolean isMatch(Object candidate) {
		return expression.isMatch(context, scope, candidate);
	}
}
