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
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IFileArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ProfileSynchronizer {

	public class ListCollector extends Collector {
		public List getList() {
			return super.getList();
		}
	}

	private static final String SYNCH_REPOSITORY_ID = "synch.repository.id"; //$NON-NLS-1$
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

		ProfileChangeRequest request = createProfileChangeRequest();
		if (request == null)
			return Status.OK_STATUS;

		SubMonitor sub = SubMonitor.convert(monitor, 100);
		ProvisioningContext provisioningContext = new ProvisioningContext(new URL[0]);

		ProvisioningPlan plan = createProvisioningPlan(request, provisioningContext, sub.newChild(50));
		if (!plan.getStatus().isOK())
			return plan.getStatus();

		if (plan.getOperands().length == 0) {
			sub.done();
			return Status.OK_STATUS;
		}

		status = executePlan(plan, provisioningContext, sub.newChild(50));
		if (!status.isOK())
			return status;

		applyConfiguration();
		return Status.OK_STATUS;
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

	private ProfileChangeRequest createProfileChangeRequest() {
		boolean modified = false;
		Collection profileIUs = getProfileIUs();
		Collection toRemove = getStaleIUs();
		profileIUs.removeAll(toRemove);

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		for (Iterator it = repositoryMap.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			String repositoryId = (String) entry.getKey();
			IMetadataRepository repository = (IMetadataRepository) entry.getValue();
			Iterator repositoryIterator = repository.query(InstallableUnitQuery.ANY, new Collector(), null).iterator();
			while (repositoryIterator.hasNext()) {
				IInstallableUnit iu = (IInstallableUnit) repositoryIterator.next();
				if (profileIUs.contains(iu))
					continue;

				if (toRemove.contains(iu)) {
					toRemove.remove(iu);
				} else {
					request.addInstallableUnits(new IInstallableUnit[] {iu});
					if (Boolean.valueOf(iu.getProperty(IInstallableUnit.PROP_TYPE_GROUP)).booleanValue())
						request.setInstallableUnitProfileProperty(iu, IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
				}
				request.setInstallableUnitProfileProperty(iu, SYNCH_REPOSITORY_ID, repositoryId);
				profileIUs.add(iu);
				modified = true;
			}
		}
		if (!toRemove.isEmpty()) {
			request.removeInstallableUnits((IInstallableUnit[]) toRemove.toArray(new IInstallableUnit[0]));
			modified = true;
		}

		if (!modified)
			return null;

		return request;
	}

	private Collection getStaleIUs() {
		Query removeQuery = new Query() {
			public boolean isMatch(Object object) {
				IInstallableUnit iu = (IInstallableUnit) object;
				String repositoryId = profile.getInstallableUnitProperty(iu, SYNCH_REPOSITORY_ID);
				if (repositoryId == null)
					return false;

				IMetadataRepository repo = (IMetadataRepository) repositoryMap.get(repositoryId);
				Query iuQuery = new InstallableUnitQuery(iu.getId(), iu.getVersion());
				return (repo == null || repo.query(iuQuery, new Collector(), null).isEmpty());
			}
		};

		ListCollector listCollector = new ListCollector();
		profile.query(removeQuery, listCollector, null);

		List result = listCollector.getList();
		return (result != null) ? result : Collections.EMPTY_LIST;
	}

	private List getProfileIUs() {
		ListCollector listCollector = new ListCollector();
		profile.query(InstallableUnitQuery.ANY, listCollector, null);

		List result = listCollector.getList();
		return (result != null) ? result : Collections.EMPTY_LIST;
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
			// TODO unexpected -- log
			e.printStackTrace();
		} finally {
			context.ungetService(reference);
		}
	}
}
