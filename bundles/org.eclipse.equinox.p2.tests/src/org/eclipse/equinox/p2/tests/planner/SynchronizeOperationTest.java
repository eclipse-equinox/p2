/*******************************************************************************
 *  Copyright (c) 2011, 2015 Sonatype, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *     IBM Corporation - Ongoing development
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.operations.Activator;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.SynchronizeOperation;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.ServiceReference;

public class SynchronizeOperationTest extends AbstractProvisioningTest {

	//Directly test the operation
	public void testSyncOperation() throws ProvisionException {
		ServiceReference<IProvisioningAgentProvider> providerRef = TestActivator.context.getServiceReference(IProvisioningAgentProvider.class);
		IProvisioningAgentProvider provider = TestActivator.context.getService(providerRef);

		URI p2location = getTestData("p2 location", "testData/synchronizeOperation/p2").toURI();
		URI repoLocation = getTestData("p2 location", "testData/synchronizeOperation/repo").toURI();
		IProvisioningAgent firstAgent = provider.createAgent(p2location);
		IMetadataRepositoryManager mgr = (IMetadataRepositoryManager) firstAgent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		IMetadataRepository repo = mgr.loadRepository(repoLocation, new NullProgressMonitor());
		ProvisioningSession session = new ProvisioningSession(firstAgent);
		SynchronizeOperation sync = new SynchronizeOperation(session, repo.query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).toUnmodifiableSet());
		Set<IInstallableUnit> installedIUs = ((IProfileRegistry) firstAgent.getService(IProfileRegistry.SERVICE_NAME)).getProfile("DefaultProfile").query(new UserVisibleRootQuery(), new NullProgressMonitor()).toUnmodifiableSet();
		System.out.println(installedIUs);
		sync.setProfileId("DefaultProfile");
		sync.resolveModal(new NullProgressMonitor());
		IProvisioningPlan plan = sync.getProvisioningPlan();
		assertOK(plan.getStatus());
	}

	//Test a copy of the helper code
	public void testCopyOfHelper() throws ProvisionException {
		ServiceReference<IProvisioningAgentProvider> providerRef = TestActivator.context.getServiceReference(IProvisioningAgentProvider.class);
		IProvisioningAgentProvider provider = TestActivator.context.getService(providerRef);

		URI p2location = getTestData("p2 location", "testData/synchronizeOperation/p2").toURI();
		URI repoLocation = getTestData("p2 location", "testData/synchronizeOperation/repo").toURI();
		IProvisioningAgent firstAgent = provider.createAgent(p2location);
		IVersionedId v = new VersionedId("payload.feature.feature.group", (String) null);
		Collection<IVersionedId> toInstall = new ArrayList<IVersionedId>();
		toInstall.add(v);
		List<URI> repos = new ArrayList<URI>();
		repos.add(repoLocation);
		SynchronizeOperation operation = createSynchronizeOperation(toInstall, repos, new NullProgressMonitor(), firstAgent);
		operation.setProfileId("DefaultProfile");
		operation.resolveModal(new NullProgressMonitor());
		assertOK(operation.getProvisioningPlan().getStatus());
	}

	//This is a copy of the OperationHelper code
	private SynchronizeOperation createSynchronizeOperation(Collection<IVersionedId> toInstall, Collection<URI> repos, IProgressMonitor monitor, IProvisioningAgent agent) throws ProvisionException {
		//		IProvisioningAgent agent = getAgent();
		ProvisioningContext ctx = createProvisioningContext(repos, agent);

		Collection<IInstallableUnit> iusToInstall;
		if (toInstall == null)
			iusToInstall = ctx.getMetadata(monitor).query(QueryUtil.createIUGroupQuery(), monitor).toUnmodifiableSet();
		else
			iusToInstall = gatherIUs(ctx.getMetadata(monitor), toInstall, false, monitor);

		SynchronizeOperation resultingOperation = new SynchronizeOperation(new ProvisioningSession(agent), iusToInstall);
		resultingOperation.setProvisioningContext(ctx);
		resultingOperation.setProfileId(IProfileRegistry.SELF);

		return resultingOperation;
	}

	//This is a copy of the OperationHelper code
	private ProvisioningContext createProvisioningContext(Collection<URI> repos, IProvisioningAgent agent) {
		ProvisioningContext ctx = new ProvisioningContext(agent);
		if (repos != null) {
			ctx.setMetadataRepositories(repos.toArray(new URI[repos.size()]));
			ctx.setArtifactRepositories(repos.toArray(new URI[repos.size()]));
		}
		return ctx;
	}

	//This is a copy of the OperationHelper code
	private Collection<IInstallableUnit> gatherIUs(IQueryable<IInstallableUnit> searchContext, Collection<IVersionedId> ius, boolean checkIUs, IProgressMonitor monitor) throws ProvisionException {
		Collection<IInstallableUnit> gatheredIUs = new ArrayList<IInstallableUnit>(ius.size());

		for (IVersionedId versionedId : ius) {
			if (!checkIUs && versionedId instanceof IInstallableUnit) {
				gatheredIUs.add((IInstallableUnit) versionedId);
				continue;
			}

			IQuery<IInstallableUnit> installableUnits = QueryUtil.createIUQuery(versionedId.getId(), versionedId.getVersion());
			IQueryResult<IInstallableUnit> matches = searchContext.query(installableUnits, monitor);
			if (matches.isEmpty())
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.OperationFactory_noIUFound, versionedId)));

			//Add the first IU
			Iterator<IInstallableUnit> iuIt = matches.iterator();
			gatheredIUs.add(iuIt.next());
		}
		return gatheredIUs;
	}
}
