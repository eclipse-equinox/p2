/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.metadata.expression.IExpressionFactory;
import org.eclipse.equinox.p2.metadata.expression.IExpressionParser;
import org.osgi.framework.*;

public class MetadataActivator implements BundleActivator {
	public static final String PI_METADATA = "org.eclipse.equinox.p2.metadata"; //$NON-NLS-1$

	public static final String SERVICE_PRIORITY = "service.priority"; //$NON-NLS-1$

	public static MetadataActivator instance;

	private BundleContext context;
	private IExpressionFactory expressionFactory;
	private ServiceReference expressionFactoryReference;
	private IExpressionParser expressionParser;
	private ServiceReference expressionParserReference;

	public static BundleContext getContext() {
		MetadataActivator activator = instance;
		return activator == null ? null : activator.context;
	}

	public static IExpressionFactory getExpressionFactory() {
		MetadataActivator activator = instance;
		return activator == null ? null : activator._getExpressionFactory();
	}

	public static IExpressionParser getExpressionParser() {
		MetadataActivator activator = instance;
		return activator == null ? null : activator._getExpressionParser();
	}

	public void start(BundleContext aContext) throws Exception {
		context = aContext;
		instance = this;
	}

	public void stop(BundleContext aContext) throws Exception {
		instance = null;

		if (expressionFactoryReference != null) {
			aContext.ungetService(expressionFactoryReference);
			expressionFactoryReference = null;
			expressionFactory = null;
		}
		if (expressionParserReference != null) {
			aContext.ungetService(expressionParserReference);
			expressionParserReference = null;
			expressionParser = null;
		}
	}

	private ServiceReference getBestReference(Class<?> serviceInterface) {
		ServiceReference[] refs;
		String serviceName = serviceInterface.getName();
		try {
			refs = context.getAllServiceReferences(serviceName, null);
		} catch (InvalidSyntaxException e) {
			LogHelper.log(new Status(IStatus.ERROR, context.getBundle().getSymbolicName(), "Unable to obtain service references for service " + serviceName, e)); //$NON-NLS-1$
			return null;
		}

		if (refs == null)
			return null;

		ServiceReference best = null;
		int idx = refs.length;
		while (--idx >= 0) {
			ServiceReference ref = refs[idx];
			if (best == null) {
				best = ref;
				continue;
			}
			Integer refPrio = (Integer) ref.getProperty(SERVICE_PRIORITY);
			Integer bestPrio = (Integer) best.getProperty(SERVICE_PRIORITY);
			if (refPrio == null)
				continue;
			if (bestPrio == null || bestPrio.intValue() < refPrio.intValue())
				best = ref;
		}
		return best;
	}

	private synchronized IExpressionFactory _getExpressionFactory() {
		if (expressionFactory == null) {
			expressionFactoryReference = getBestReference(IExpressionFactory.class);
			if (expressionFactoryReference == null)
				throw new IllegalStateException(Messages.no_expression_factory);
			expressionFactory = (IExpressionFactory) context.getService(expressionFactoryReference);
		}
		return expressionFactory;
	}

	private synchronized IExpressionParser _getExpressionParser() {
		if (expressionParser == null) {
			expressionParserReference = getBestReference(IExpressionParser.class);
			if (expressionParserReference == null)
				throw new IllegalStateException(Messages.no_expression_parser);
			expressionParser = (IExpressionParser) context.getService(expressionParserReference);
		}
		return expressionParser;
	}
}
