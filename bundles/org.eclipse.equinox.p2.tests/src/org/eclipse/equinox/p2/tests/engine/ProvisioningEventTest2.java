/*******************************************************************************
 *  Copyright (c) 2012 Wind River and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Wind River - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.net.URI;
import java.util.EventObject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.ProvisioningAgent;
import org.eclipse.equinox.internal.p2.engine.CommitOperationEvent;
import org.eclipse.equinox.internal.p2.engine.RollbackOperationEvent;
import org.eclipse.equinox.internal.p2.repository.DownloadProgressEvent;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.testserver.helper.AbstractTestServerClientCase;
import org.junit.Test;

public class ProvisioningEventTest2 extends AbstractTestServerClientCase {

	@Test
	public void testDownloadEventFromMultipleAgents() throws ProvisionException, OperationCanceledException, InterruptedException {
		ProvisioningAgent newAgent = new ProvisioningAgent();
		newAgent.setBundleContext(TestActivator.getContext());
		IProvisioningEventBus eventBus = (IProvisioningEventBus) newAgent.getService(IProvisioningEventBus.SERVICE_NAME);
		class DownloadProvisiongEventListener implements ProvisioningListener {
			boolean notifiedDownloadProgressEvent = false;
			CountDownLatch latch = new CountDownLatch(1);

			public void notify(EventObject o) {
				if (o instanceof DownloadProgressEvent) {
					notifiedDownloadProgressEvent = true;
				} else if (o instanceof CommitOperationEvent || o instanceof RollbackOperationEvent)
					latch.countDown();
			}
		}
		DownloadProvisiongEventListener provListener = new DownloadProvisiongEventListener();
		DownloadProvisiongEventListener provListener1 = new DownloadProvisiongEventListener();
		eventBus.addListener(provListener);

		IProvisioningEventBus eventBus2 = (IProvisioningEventBus) getAgent().getService(IProvisioningEventBus.SERVICE_NAME);
		try {
			URI repoLoc = URI.create(getBaseURL() + "/public/emptyJarRepo");
			//remove any existing profile with the same name
			final String name = "testProfile";
			IProfileRegistry profileRegistry = (IProfileRegistry) getAgent().getService(IProfileRegistry.SERVICE_NAME);
			profileRegistry.removeProfile(name);
			IProfile profile = profileRegistry.addProfile(name, null);
			// clean possible cached artifacts
			IArtifactRepository bundlePool = Util.getBundlePoolRepository(getAgent(), profile);
			bundlePool.removeAll(new NullProgressMonitor());
			ProvisioningContext context = new ProvisioningContext(getAgent());
			context.setArtifactRepositories(new URI[] {repoLoc});
			context.setMetadataRepositories(new URI[] {repoLoc});
			IEngine engine = (IEngine) getAgent().getService(IEngine.SERVICE_NAME);
			IProvisioningPlan plan = engine.createPlan(profile, context);
			IMetadataRepositoryManager metaManager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
			IQueryResult<IInstallableUnit> allIUs = metaManager.loadRepository(repoLoc, null).query(QueryUtil.ALL_UNITS, null);
			for (IInstallableUnit iu : allIUs.toSet()) {
				plan.addInstallableUnit(iu);
			}

			eventBus2.addListener(provListener1);
			IStatus status = engine.perform(plan, new NullProgressMonitor());
			assertTrue("Provisioning was failed.", status.isOK());
			provListener1.latch.await(10, TimeUnit.SECONDS);
			assertTrue("Listener1 is NOT notified by DownloadProgressEvent.", provListener1.notifiedDownloadProgressEvent);
			assertFalse("Listener should NOT be notified by DownloadProgressEvent.", provListener.notifiedDownloadProgressEvent);
		} finally {
			eventBus2.removeListener(provListener1);
		}
	}
}