/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.reconciler.dropins;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IFileArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.osgi.framework.*;

/**
 * Synchronizes a profile with a set of repositories.
 */
public class ProfileSynchronizer {
	private static final String SUPER_IU = "org.eclipse.equinox.p2.dropins"; //$NON-NLS-1$

	public class ListCollector extends Collector {
		public List getList() {
			return super.getList();
		}
	}

	private static final String CACHE_EXTENSIONS = "org.eclipse.equinox.p2.cache.extensions"; //$NON-NLS-1$
	private static final String PIPE = "|"; //$NON-NLS-1$
	final IProfile profile;

	final Map repositoryMap;

	/*
	 * Constructor for the class.
	 */
	public ProfileSynchronizer(IProfile profile, Collection repositories) {
		this.profile = profile;
		this.repositoryMap = new HashMap();
		for (Iterator it = repositories.iterator(); it.hasNext();) {
			IMetadataRepository repository = (IMetadataRepository) it.next();
			repositoryMap.put(repository.getLocation().toExternalForm(), repository);
		}
	}

	/*
	 * Synchronize the profile with the list of metadata repositories.
	 */
	public IStatus synchronize(IProgressMonitor monitor) {

		IStatus status = synchronizeCacheExtensions();
		if (!status.isOK())
			return status;

		ProvisioningContext context = getContext();
		ProfileChangeRequest request = createProfileChangeRequest(context);
		if (request == null)
			return Status.OK_STATUS;

		SubMonitor sub = SubMonitor.convert(monitor, 100);
		try {
			//create the provisioning plan
			ProvisioningPlan plan = createProvisioningPlan(request, context, sub.newChild(50));
			status = plan.getStatus();
			if (status.getSeverity() == IStatus.ERROR || plan.getOperands().length == 0)
				return status;

			//invoke the engine to perform installs/uninstalls
			IStatus engineResult = executePlan(plan, context, sub.newChild(50));
			if (!engineResult.isOK())
				return engineResult;

			applyConfiguration();
			return status;
		} finally {
			sub.done();
		}
	}

	private ProvisioningContext getContext() {
		ArrayList repoURLs = new ArrayList();
		for (Iterator iterator = repositoryMap.keySet().iterator(); iterator.hasNext();) {
			try {
				repoURLs.add(new URL((String) iterator.next()));
			} catch (MalformedURLException e) {
				//ignore
			}
		}
		return new ProvisioningContext((URL[]) repoURLs.toArray(new URL[repoURLs.size()]));
	}

	private IStatus synchronizeCacheExtensions() {
		List currentExtensions = new ArrayList();
		StringBuffer buffer = new StringBuffer();
		for (Iterator it = repositoryMap.keySet().iterator(); it.hasNext();) {
			String repositoryId = (String) it.next();
			try {
				IArtifactRepository repository = Activator.loadArtifactRepository(new URL(repositoryId));

				if (repository instanceof IFileArtifactRepository) {
					currentExtensions.add(repositoryId);
					buffer.append(repositoryId);
					if (it.hasNext())
						buffer.append(PIPE);
				}
			} catch (ProvisionException e) {
				// ignore
			} catch (MalformedURLException e) {
				// unexpected
				e.printStackTrace();
			}
		}
		String currentExtensionsProperty = (buffer.length() == 0) ? null : buffer.toString();

		List previousExtensions = new ArrayList();
		String previousExtensionsProperty = profile.getProperty(CACHE_EXTENSIONS);
		if (previousExtensionsProperty != null) {
			StringTokenizer tokenizer = new StringTokenizer(previousExtensionsProperty, PIPE);
			while (tokenizer.hasMoreTokens()) {
				previousExtensions.add(tokenizer.nextToken());
			}
		}

		if (previousExtensions.size() == currentExtensions.size() && previousExtensions.containsAll(currentExtensions))
			return Status.OK_STATUS;

		Operand operand = new PropertyOperand(CACHE_EXTENSIONS, previousExtensionsProperty, currentExtensionsProperty);

		return executeOperands(new ProvisioningContext(new URL[0]), new Operand[] {operand}, null);
	}

	private IInstallableUnit createRootIU(List children) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(SUPER_IU);
		iu.setVersion(new Version("1.0.0.v" + System.currentTimeMillis()));
		List required = new ArrayList();
		for (Iterator iter = children.iterator(); iter.hasNext();) {
			IInstallableUnit next = (IInstallableUnit) iter.next();
			required.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, next.getId(), null, null, false /* optional */, false, true));
		}
		if (required.size() > 0)
			iu.setRequiredCapabilities((RequiredCapability[]) required.toArray(new RequiredCapability[required.size()]));
		return MetadataFactory.createInstallableUnit(iu);
	}

	private ProfileChangeRequest createProfileChangeRequest(ProvisioningContext context) {
		List toAdd = new ArrayList();
		List defaults = new ArrayList();

		Collector allIUs = getAllIUsFromRepos();

		//Nothing has changed
		IInstallableUnit previous = getIU(SUPER_IU);
		//Empty repo

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		if (allIUs.size() == 0) {
			if (previous == null)
				return null;

			//Request the removal of the super IU
			request.removeInstallableUnits(new IInstallableUnit[] {previous});
			return request;
		}

		for (Iterator iterator = allIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			defaults.add(createDefaultIU(iu));
			toAdd.add(createIncludedIU(iu));
			if (Boolean.valueOf(iu.getProperty(IInstallableUnit.PROP_TYPE_GROUP)).booleanValue())
				request.setInstallableUnitProfileProperty(iu, IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
		}

		List extra = new ArrayList();
		extra.addAll(defaults);
		extra.addAll(toAdd);
		context.setExtraIUs(extra);

		// only add one IU to the request. it will contain all the other IUs we want to install
		IInstallableUnit rootIU = createRootIU(toAdd);
		request.addInstallableUnits(new IInstallableUnit[] {rootIU});

		//Request the removal of the previous super IU
		if (previous != null)
			request.removeInstallableUnits(new IInstallableUnit[] {previous});
		return request;
	}

	private Collector getAllIUsFromRepos() {
		Collector allRepos = new Collector();
		for (Iterator it = repositoryMap.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			IMetadataRepository repository = (IMetadataRepository) entry.getValue();
			repository.query(InstallableUnitQuery.ANY, allRepos, null).iterator();
		}
		return allRepos;
	}

	private IInstallableUnit createIncludedIU(IInstallableUnit iu) {
		InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
		iud.setId(iu.getId());
		iud.setVersion(new Version(0, 0, 0, Long.toString(System.currentTimeMillis())));
		RequiredCapability[] reqs = new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), null, null, false, false, true)};
		iud.setRequiredCapabilities(reqs);
		return MetadataFactory.createInstallableUnit(iud);
	}

	private IInstallableUnit createDefaultIU(IInstallableUnit iu) {
		InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
		iud.setId(iu.getId());
		iud.setVersion(new Version(0, 0, 0));
		iud.setCapabilities(new ProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), new Version(0, 0, 0))});
		return MetadataFactory.createInstallableUnit(iud);
	}

	private IInstallableUnit getIU(String iuId) {
		ListCollector collector = new ListCollector();
		profile.query(new InstallableUnitQuery(iuId), collector, null);
		if (collector.size() > 0)
			return (IInstallableUnit) collector.iterator().next();
		return null;
	}

	private ProvisioningPlan createProvisioningPlan(ProfileChangeRequest request, ProvisioningContext provisioningContext, IProgressMonitor monitor) {
		BundleContext context = Activator.getContext();
		ServiceReference reference = context.getServiceReference(IPlanner.class.getName());
		IPlanner planner = (IPlanner) context.getService(reference);

		try {
			return planner.getProvisioningPlan(request, provisioningContext, monitor);
		} finally {
			context.ungetService(reference);
		}
	}

	private IStatus executePlan(ProvisioningPlan plan, ProvisioningContext provisioningContext, IProgressMonitor monitor) {
		Operand[] operands = plan.getOperands();
		return executeOperands(provisioningContext, operands, monitor);
	}

	private IStatus executeOperands(ProvisioningContext provisioningContext, Operand[] operands, IProgressMonitor monitor) {
		BundleContext context = Activator.getContext();
		ServiceReference reference = context.getServiceReference(IEngine.class.getName());
		IEngine engine = (IEngine) context.getService(reference);
		try {
			PhaseSet phaseSet = new DefaultPhaseSet();
			IStatus engineResult = engine.perform(profile, phaseSet, operands, provisioningContext, monitor);
			return engineResult;
		} finally {
			context.ungetService(reference);
		}
	}

	/*
	 * Write out the configuration file.
	 */
	private void applyConfiguration() {
		BundleContext context = Activator.getContext();
		ServiceReference reference = context.getServiceReference(Configurator.class.getName());
		Configurator configurator = (Configurator) context.getService(reference);
		try {
			configurator.applyConfiguration();
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Unexpected failure applying configuration", e)); //$NON-NLS-1$
		} finally {
			context.ungetService(reference);
		}
	}
}
