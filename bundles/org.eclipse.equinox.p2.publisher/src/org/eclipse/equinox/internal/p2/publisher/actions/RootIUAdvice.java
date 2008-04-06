/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.actions;

import java.util.Collection;

public class RootIUAdvice extends AbstractAdvice implements IRootIUAdvice {

	private Collection children;

	public RootIUAdvice(Collection children) {
		this.children = children;
	}

	public Collection getChildren() {
		return children;
	}

}
