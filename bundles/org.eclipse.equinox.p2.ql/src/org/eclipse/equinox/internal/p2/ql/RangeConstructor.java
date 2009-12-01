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

import org.eclipse.equinox.internal.provisional.p2.metadata.VersionRange;

public class RangeConstructor extends Constructor {

	static final String KEYWORD = "range"; //$NON-NLS-1$

	public RangeConstructor(Expression[] operands) {
		super(operands);
	}

	Object createInstance(String arg) {
		return new VersionRange(arg);
	}

	String getOperator() {
		return KEYWORD;
	}
}
