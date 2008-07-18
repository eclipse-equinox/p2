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

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * Create a top level IU that lists all the current roots as well as any explicitly identified
 * top level IUs.
 */
public class RootIUAction extends AbstractPublishingAction {

	private String version;
	private String id;
	private String name;
	private IQueryable repositories;
	private Collection versionAdvice;
	private IPublisherInfo info;

	public RootIUAction(String id, String version, String name, IPublisherInfo info) {
		this.id = id;
		this.version = version;
		this.name = name;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		this.info = info;
		generateRootIU(results);
		return Status.OK_STATUS;
	}

	protected void generateRootIU(IPublisherResult result) {
		Collection children = getChildren(result);
		InstallableUnitDescription descriptor = createTopLevelIUDescription(children, id, version, name, null, false);
		IInstallableUnit rootIU = MetadataFactory.createInstallableUnit(descriptor);
		if (rootIU == null)
			return;
		result.addIU(rootIU, IPublisherResult.NON_ROOT);

		// TODO why do we create a category here?
		//		result.addIU(generateDefaultCategory(rootIU, rootCategory), IPublisherResult.NON_ROOT);
	}

	/**
	 * This was copied over from Generator to match up with the call from generateRootIU (above).
	 * It is entirely unclear why it was needed.  Should review.
	 * Short term fix to ensure IUs that have no corresponding category are not lost.
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=211521.
	 */
	//	private IInstallableUnit generateDefaultCategory(IInstallableUnit rootIU) {
	//		rootCategory.add(rootIU);
	//
	//		InstallableUnitDescription cat = new MetadataFactory.InstallableUnitDescription();
	//		cat.setSingleton(true);
	//		String categoryId = rootIU.getId() + ".categoryIU"; //$NON-NLS-1$
	//		cat.setId(categoryId);
	//		cat.setVersion(Version.emptyVersion);
	//		cat.setProperty(IInstallableUnit.PROP_NAME, rootIU.getProperty(IInstallableUnit.PROP_NAME));
	//		cat.setProperty(IInstallableUnit.PROP_DESCRIPTION, rootIU.getProperty(IInstallableUnit.PROP_DESCRIPTION));
	//
	//		ArrayList required = new ArrayList(rootCategory.size());
	//		for (Iterator iterator = rootCategory.iterator(); iterator.hasNext();) {
	//			IInstallableUnit iu = (IInstallableUnit) iterator.next();
	//			required.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), VersionRange.emptyRange, iu.getFilter(), false, false));
	//		}
	//		cat.setRequiredCapabilities((RequiredCapability[]) required.toArray(new RequiredCapability[required.size()]));
	//		cat.setCapabilities(new ProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, categoryId, Version.emptyVersion)});
	//		cat.setArtifacts(new IArtifactKey[0]);
	//		cat.setProperty(IInstallableUnit.PROP_TYPE_CATEGORY, "true"); //$NON-NLS-1$
	//		return MetadataFactory.createInstallableUnit(cat);
	//	}
	private Collection getChildren(IPublisherResult result) {
		// get any roots that we have accummulated so far and search for
		// children from the advice.
		Collection children = result.getIUs(null, IPublisherResult.ROOT);
		Collection rootAdvice = info.getAdvice(null, true, null, null, IRootIUAdvice.class);
		for (Iterator i = rootAdvice.iterator(); i.hasNext();) {
			IRootIUAdvice advice = (IRootIUAdvice) i.next();
			Collection list = advice.getChildren();
			if (list != null)
				for (Iterator j = list.iterator(); j.hasNext();) {
					Object object = j.next();
					// if the advice is a string, look it up in the result.  if not there then 
					// query the known metadata repos
					if (object instanceof String) {
						String childId = (String) object;
						IInstallableUnit iu = result.getIU(childId, null);
						if (iu == null)
							iu = queryFor(childId);
						if (iu != null)
							children.add(iu);
					} else if (object instanceof IInstallableUnit)
						children.add(object);
				}
		}
		return children;
	}

	/**
	 * Loop over the known metadata repositories looking for the given IU.
	 * Return the first IU found.
	 * @param iuId  the id of the IU to look for
	 * @return the first matching IU or <code>null</code> if none.
	 */
	private IInstallableUnit queryFor(String iuId) {
		InstallableUnitQuery query = new InstallableUnitQuery(iuId, getVersionAdvice(iuId));
		if (repositories == null)
			return null;
		Collector result = repositories.query(query, new Collector(), new NullProgressMonitor());
		if (!result.isEmpty())
			return (IInstallableUnit) result.iterator().next();
		return null;
	}

	protected static InstallableUnitDescription createTopLevelIUDescription(Collection children, String id, String version, String name, Collection requires, boolean configureLauncherData) {
		InstallableUnitDescription root = new MetadataFactory.InstallableUnitDescription();
		root.setSingleton(true);
		root.setId(id);
		root.setVersion(new Version(version));
		root.setProperty(IInstallableUnit.PROP_NAME, name);

		ArrayList requiredCapabilities = new ArrayList(children.size());
		for (Iterator iterator = children.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
			//			boolean isOptional = checkOptionalRootDependency(iu);
			requiredCapabilities.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range, iu.getFilter(), false, false));
		}
		if (requires != null)
			requiredCapabilities.addAll(requires);
		root.setRequiredCapabilities((RequiredCapability[]) requiredCapabilities.toArray(new RequiredCapability[requiredCapabilities.size()]));
		root.setArtifacts(new IArtifactKey[0]);

		root.setProperty("lineUp", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		root.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(id, VersionRange.emptyRange, IUpdateDescriptor.NORMAL, null));
		root.setProperty(IInstallableUnit.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		root.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(id, new Version(version))});
		// TODO why is the type OSGI?
		root.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_OSGI);
		return root;
	}

	private Version getVersionAdvice(String id) {
		if (versionAdvice == null) {
			versionAdvice = info.getAdvice(null, true, null, null, IVersionAdvice.class);
			if (versionAdvice == null)
				return null;
		}
		for (Iterator i = versionAdvice.iterator(); i.hasNext();) {
			IVersionAdvice advice = (IVersionAdvice) i.next();
			// TODO have to figure a way to know the namespace here.  for now just look everywhere
			Version result = advice.getVersion(IVersionAdvice.NS_BUNDLE, id);
			if (result != null)
				return result;
			result = advice.getVersion(IVersionAdvice.NS_FEATURE, id);
			if (result != null)
				return result;
		}
		return null;
	}
}
