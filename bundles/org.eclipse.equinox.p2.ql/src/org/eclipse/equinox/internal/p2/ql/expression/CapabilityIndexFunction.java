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

import org.eclipse.equinox.internal.p2.ql.CapabilityIndex;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ql.IEvaluationContext;

/**
 * A function that creates a {@link CapabilityIndex} based on a collection of
 * {@link IInstallableUnit} instances.
 */
final class CapabilityIndexFunction extends Function {

	public CapabilityIndexFunction(Expression[] operands) {
		super(assertLength(operands, 1, 1, KEYWORD_CAPABILITY_INDEX));
		assertNotBoolean(operands[0], "parameter"); //$NON-NLS-1$
	}

	public Object evaluate(IEvaluationContext context) {
		return new CapabilityIndex(operands[0].evaluateAsIterator(context));
	}

	String getOperator() {
		return KEYWORD_CAPABILITY_INDEX;
	}
}
