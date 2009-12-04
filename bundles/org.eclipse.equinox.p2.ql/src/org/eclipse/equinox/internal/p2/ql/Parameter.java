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
package org.eclipse.equinox.internal.p2.ql;

/**
 * The abstract base class for the indexed and keyed parameters
 */
abstract class Parameter extends Expression {
	static final String OPERATOR = "$"; //$NON-NLS-1$

	int getPriority() {
		return ExpressionParser.PRIORITY_PARAMETER;
	}
}
