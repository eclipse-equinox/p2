/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.generator;

import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.generator.features.ProductFile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.osgi.service.resolver.VersionRange;

public class ProductQuery extends Query {
	private static final String EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher"; //$NON-NLS-1$

	private final ProductFile product;
	private final String flavor;
	private final String launcherCUPrefix;
	private final Map children = new HashMap();

	public ProductQuery(ProductFile product, String flavor) {
		this.product = product;
		this.flavor = flavor;
		this.launcherCUPrefix = flavor + product.getId() + ".launcher"; //$NON-NLS-1$
		initialize();
	}

	private void initialize() {
		List contents = product.useFeatures() ? product.getFeatures() : product.getPlugins();
		for (Iterator iterator = contents.iterator(); iterator.hasNext();) {
			String item = (String) iterator.next();
			children.put(item, VersionRange.emptyRange);
			children.put(flavor + item, VersionRange.emptyRange); //CUs
		}

		//also add the launcher.jar
		if (!children.containsKey(EQUINOX_LAUNCHER)) {
			children.put(EQUINOX_LAUNCHER, VersionRange.emptyRange);
			children.put(flavor + EQUINOX_LAUNCHER, VersionRange.emptyRange);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.query.Query#isMatch(java.lang.Object)
	 */
	public boolean isMatch(Object object) {
		if (!(object instanceof IInstallableUnit))
			return false;

		IInstallableUnit candidate = (IInstallableUnit) object;
		VersionRange range = (VersionRange) children.get(candidate.getId());
		if (range != null) {
			return range.isIncluded(candidate.getVersion());
		}

		//also include the launcher CU fragments as a workaround to bug 218890
		if (candidate.getId().startsWith(launcherCUPrefix))
			return true;
		//and include the launcher fragment CUs
		if (candidate.getId().startsWith(flavor + EQUINOX_LAUNCHER))
			return true;

		return false;
	}
}
