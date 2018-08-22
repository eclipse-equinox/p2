/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import java.util.Collection;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherResult;

/**
 * Advises publisher actions as to which IUs should be listed as children of an
 * eventual root IU.  Each child is described as a VersionedName.
 */
public class RootIUAdvice extends AbstractAdvice implements IRootIUAdvice {

	private Collection<? extends Object> children;

	public RootIUAdvice(Collection<? extends Object> children) {
		this.children = children;
	}

	@Override
	public Collection<? extends Object> getChildren(IPublisherResult result) {
		return children;
	}

}
