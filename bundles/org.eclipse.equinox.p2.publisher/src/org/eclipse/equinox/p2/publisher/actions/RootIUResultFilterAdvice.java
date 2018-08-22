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
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import java.util.Arrays;
import java.util.Collection;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.query.IQuery;

public class RootIUResultFilterAdvice extends AbstractAdvice implements IRootIUAdvice {
	private IQuery<IInstallableUnit> query;

	public RootIUResultFilterAdvice(IQuery<IInstallableUnit> query) {
		this.query = query;
	}

	@Override
	public Collection<IInstallableUnit> getChildren(IPublisherResult result) {
		Collection<IInstallableUnit> value = result.getIUs(null, IPublisherResult.ROOT);
		if (query == null)
			return value;
		return Arrays.asList(query.perform(value.iterator()).toArray(IInstallableUnit.class));
	}
}
