/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import java.util.Collection;
import java.util.Iterator;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherResult;

public class RootIUResultFilterAdvice extends AbstractAdvice implements IRootIUAdvice {
	private Query query;

	public RootIUResultFilterAdvice(Query query) {
		this.query = query;
	}

	public Collection getChildren(IPublisherResult result) {
		Iterator itr = result.getIUs(null, IPublisherResult.ROOT).iterator();
		return query.perform(itr, new Collector()).toCollection();
	}
}
