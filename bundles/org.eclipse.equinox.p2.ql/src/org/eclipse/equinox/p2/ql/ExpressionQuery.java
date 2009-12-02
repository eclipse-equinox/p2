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

import java.util.Iterator;
import java.util.Map;
import org.eclipse.equinox.internal.p2.ql.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IVersionedId;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.ContextQuery;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

/**
 * An IQuery implementation that is based on the p2 query language.
 */
public class ExpressionQuery extends ContextQuery {
	private static final ExpressionParser parser = new ExpressionParser();
	private static final Object[] noParameters = new Object[0];

	private final Class instanceClass;
	private final ContextExpression expression;
	private final Object[] parameters;

	public ExpressionQuery(Class instanceClass, ContextExpression expression, Object[] parameters) {
		this.expression = expression;
		this.parameters = parameters == null ? noParameters : parameters;
		this.instanceClass = instanceClass;
	}

	public ExpressionQuery(Class instanceClass, String expression) {
		this(instanceClass, parser.parseQuery(expression), null);
	}

	public ExpressionQuery(Class instanceClass, String expression, Object param1) {
		this(instanceClass, parser.parseQuery(expression), new Object[] {param1});
	}

	public ExpressionQuery(Class instanceClass, String expression, Object param1, Object param2) {
		this(instanceClass, parser.parseQuery(expression), new Object[] {param1, param2});
	}

	public ExpressionQuery(Class instanceClass, String expression, Object param1, Object param2, Object param3) {
		this(instanceClass, parser.parseQuery(expression), new Object[] {param1, param2, param3});
	}

	public ExpressionQuery(ContextExpression expression, Map args) {
		this(IVersionedId.class, expression, new Object[] {args});
	}

	public ExpressionQuery(ContextExpression expr, Object[] objects) {
		this(IVersionedId.class, expr, objects);
	}

	public ExpressionQuery(String expression) {
		this(IVersionedId.class, parser.parseQuery(expression), noParameters);
	}

	public ExpressionQuery(String expression, Object param1) {
		this(IVersionedId.class, parser.parseQuery(expression), new Object[] {param1});
	}

	public ExpressionQuery(String expression, Object param1, Object param2) {
		this(IVersionedId.class, parser.parseQuery(expression), new Object[] {param1, param2});
	}

	public ExpressionQuery(String expression, Object param1, Object param2, Object param3) {
		this(IVersionedId.class, parser.parseQuery(expression), new Object[] {param1, param2, param3});
	}

	public Class getInstanceClass() {
		return instanceClass;
	}

	public Object getProperty(String key) {
		if (IArtifactRepository.QUERY_EXCLUDE_KEYS.equals(key))
			return Boolean.valueOf(!instanceClass.isAssignableFrom(IArtifactKey.class));

		if (IArtifactRepository.QUERY_EXCLUDE_DESCRIPTORS.equals(key))
			return Boolean.valueOf(!instanceClass.isAssignableFrom(IArtifactDescriptor.class));

		return super.getProperty(key);
	}

	public Collector perform(Iterator iterator, Collector collector) {
		iterator = expression.evaluateAsIterator(new ExpressionContext(instanceClass, parameters, iterator, expression.needsRepeatedIterations()), expression.defineScope());
		while (iterator.hasNext()) {
			Object nxt = iterator.next();
			if (!collector.accept(nxt))
				break;
		}
		return collector;
	}
}
