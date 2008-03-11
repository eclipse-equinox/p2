/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher;

import java.util.Collection;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.IPublisherResult;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.PublisherResult;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;

public class Publisher {

	private IPublisherInfo info;
	private IPublisherResult results;

	public Publisher(IPublisherInfo info) {
		this.info = info;
		results = new PublisherResult();
	}

	public Publisher(IPublisherInfo info, IPublisherResult results) {
		this.info = info;
		this.results = results;
	}

	public IStatus publish(IPublishingAction[] actions) {
		// run all the actions
		MultiStatus finalStatus = new MultiStatus("this", 0, "publishing result", null);
		for (int i = 0; i < actions.length; i++) {
			IStatus status = actions[i].perform(info, results);
			finalStatus.merge(status);
		}
		if (!finalStatus.isOK())
			return finalStatus;

		// if there were no errors, publish all the ius.
		IMetadataRepository metadataRepository = info.getMetadataRepository();
		if (metadataRepository != null) {
			Collection ius = results.getIUs(null, null);
			metadataRepository.addInstallableUnits((IInstallableUnit[]) ius.toArray(new IInstallableUnit[ius.size()]));
		}
		return Status.OK_STATUS;
	}
}
