/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

import org.eclipse.equinox.p2.metadata.Version;

/**
 * A function that creates a {@link Version} from a string
 */
public final class VersionFunction extends Function {

	public VersionFunction(Expression[] operands) {
		super(assertLength(operands, 1, 1, KEYWORD_VERSION));
	}

	@Override
	boolean assertSingleArgumentClass(Object v) {
		return v instanceof String;
	}

	@Override
	Object createInstance(Object arg) {
		return Version.create((String) arg);
	}

	@Override
	public String getOperator() {
		return KEYWORD_VERSION;
	}
}
