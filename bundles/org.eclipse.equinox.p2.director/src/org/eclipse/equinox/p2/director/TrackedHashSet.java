/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.director;

import java.util.*;
import org.eclipse.equinox.p2.metadata.*;

public class TrackedHashSet extends LinkedHashSet {
	RecommendationDescriptor recommendations;

	public TrackedHashSet(int initSize, RecommendationDescriptor recommendations) {
		super(initSize);
		this.recommendations = recommendations;
	}

	public boolean add(Object toAdd) {
		if (toAdd instanceof InstallableUnit) {
			filter((IInstallableUnit) toAdd);
		}
		return super.add(toAdd);
	}

	private void filter(IInstallableUnit toAdd) {
		ProvidedCapability[] capabilities = toAdd.getProvidedCapabilities();
		for (int i = 0; i < capabilities.length; i++) {
			if (capabilities[i].isSatisfiedBy(new RequiredCapability(IInstallableUnit.IU_KIND_NAMESPACE, "recommendation", null, null, false, false))) {
				recommendations.merge(RecommendationDescriptor.parse(toAdd.getTouchpointData()[0].getInstructions("recommendations")));
			}
		}
	}

	public boolean addAll(Collection arg0) {
		for (Iterator iterator = arg0.iterator(); iterator.hasNext();) {
			filter((IInstallableUnit) iterator.next());
		}
		return super.addAll(arg0);
	}
}