/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.generator;

import java.io.*;
import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.generator.features.ProductFile;
import org.eclipse.equinox.internal.p2.metadata.query.MatchQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.Generator.GeneratorResult;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.MetadataGeneratorHelper;
import org.eclipse.equinox.p2.metadata.*;

public class ProductQuery extends MatchQuery {
	private static final String EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher"; //$NON-NLS-1$

	private final ProductFile product;
	private final String flavor;
	private final Map children = new HashMap();
	private final String versionAdvice;

	public ProductQuery(ProductFile product, String flavor, Map configIUs, String versionAdvice) {
		this.product = product;
		this.flavor = flavor;
		this.versionAdvice = versionAdvice;
		initialize(configIUs);
	}

	private Properties loadVersions(String location) {
		Properties properties = new Properties();
		if (location == null)
			return properties;
		File file = new File(location);
		if (file.exists()) {
			InputStream stream = null;
			try {
				stream = new BufferedInputStream(new FileInputStream(file));
				properties.load(stream);
			} catch (IOException e) {
				// nothing
			} finally {
				if (stream != null)
					try {
						stream.close();
					} catch (IOException e) {
						//nothing
					}
			}
		}
		return properties;
	}

	private void initialize(Map configIUs) {
		boolean features = product.useFeatures();
		Properties versions = loadVersions(versionAdvice);

		List contents = features ? product.getFeatures() : product.getPlugins();
		for (Iterator iterator = contents.iterator(); iterator.hasNext();) {
			String item = (String) iterator.next();

			VersionRange range = VersionRange.emptyRange;
			if (versions.containsKey(item)) {
				Version value = Version.create(versions.getProperty(item));
				range = new VersionRange(value, true, value, true);
			}

			if (features) // for features we want the group
				item = MetadataGeneratorHelper.getTransformedId(item, false, true);

			children.put(item, range);
			if (configIUs.containsKey(item)) {
				for (Iterator ius = ((Set) configIUs.get(item)).iterator(); ius.hasNext();) {
					IInstallableUnit object = (IInstallableUnit) ius.next();
					children.put(object.getId(), new VersionRange(object.getVersion(), true, object.getVersion(), true));
				}
			}
		}

		//also include the launcher CU fragments as a workaround to bug 218890
		String launcherPrefix = product.getId() + ".launcher"; //$NON-NLS-1$
		if (configIUs.containsKey(launcherPrefix)) {
			for (Iterator ius = ((Set) configIUs.get(launcherPrefix)).iterator(); ius.hasNext();) {
				IInstallableUnit object = (IInstallableUnit) ius.next();
				children.put(object.getId(), new VersionRange(object.getVersion(), true, object.getVersion(), true));
			}
		}

		//also add the launcher.jar
		if (!children.containsKey(EQUINOX_LAUNCHER)) {
			children.put(EQUINOX_LAUNCHER, VersionRange.emptyRange);
			children.put(flavor + EQUINOX_LAUNCHER, VersionRange.emptyRange);
		}

		// and launcher fragment CUs
		if (configIUs.containsKey(EQUINOX_LAUNCHER)) {
			for (Iterator ius = ((Set) configIUs.get(EQUINOX_LAUNCHER)).iterator(); ius.hasNext();) {
				IInstallableUnit object = (IInstallableUnit) ius.next();
				children.put(object.getId(), new VersionRange(object.getVersion(), true, object.getVersion(), true));
			}
		}

		// feature based product, individual bundle config CUs are under CONFIGURATION_CUS for convenience
		if (features && configIUs.containsKey(GeneratorResult.CONFIGURATION_CUS)) {
			for (Iterator ius = ((Set) configIUs.get(GeneratorResult.CONFIGURATION_CUS)).iterator(); ius.hasNext();) {
				IInstallableUnit object = (IInstallableUnit) ius.next();
				children.put(object.getId(), new VersionRange(object.getVersion(), true, object.getVersion(), true));
			}
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

		return false;
	}
}
