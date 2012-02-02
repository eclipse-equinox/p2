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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorEvent;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.p2.metadata.TouchpointData;
import org.eclipse.equinox.internal.p2.metadata.TouchpointInstruction;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.RemoveRepositoryAction;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.junit.*;

public class ProvisioningEventTest extends AbstractProvisioningTest {
	private IEngine engine;
	private File testProvisioning;

	@Before
	public void setUp() throws Exception {
		engine = getEngine();
		testProvisioning = new File(System.getProperty("java.io.tmpdir"), "testProvisioning");
		delete(testProvisioning);
		testProvisioning.mkdir();
	}

	@After
	public void tearDown() throws Exception {
		engine = null;
		delete(testProvisioning);
	}

	@Test
	public void testCollectEvent() throws ProvisionException, OperationCanceledException, InterruptedException {
		class ProvTestListener implements ProvisioningListener {
			int requestsNumber = 0;
			boolean called = false;
			boolean mirrorEevent = false;
			CountDownLatch latch = new CountDownLatch(1);

			public void notify(EventObject o) {
				if (o instanceof CollectEvent) {
					if (((CollectEvent) o).getType() == CollectEvent.TYPE_OVERALL_START && ((CollectEvent) o).getRepository() == null) {
						called = true;
						IArtifactRequest[] requests = ((CollectEvent) o).getDownloadRequests();
						requestsNumber = requests.length;
					}
				} else if (o instanceof MirrorEvent) {
					mirrorEevent = true;
					System.out.println(((MirrorEvent) o).getDownloadStatus());
				} else if (o instanceof CommitOperationEvent || o instanceof RollbackOperationEvent)
					latch.countDown();
			}
		}
		final ProvTestListener listener = new ProvTestListener();
		getEventBus().addListener(listener);
		try {
			URI repoLoc = getTestData("Load test data.", "/testData/testRepos/updateSite").toURI();
			IProfile profile = createProfile("test");
			// clean possible cached artifacts
			IArtifactRepository bundlePool = Util.getBundlePoolRepository(getAgent(), profile);
			bundlePool.removeAll(new NullProgressMonitor());
			ProvisioningContext context = new ProvisioningContext(getAgent());
			context.setArtifactRepositories(new URI[] {repoLoc});
			context.setMetadataRepositories(new URI[] {repoLoc});
			IProvisioningPlan plan = engine.createPlan(profile, context);
			IQueryResult<IInstallableUnit> allIUs = getMetadataRepositoryManager().loadRepository(repoLoc, null).query(QueryUtil.ALL_UNITS, null);
			for (IInstallableUnit iu : allIUs.toSet()) {
				plan.addInstallableUnit(iu);
			}
			IStatus status = engine.perform(plan, new NullProgressMonitor());
			assertTrue("Provisioning was failed.", status.isOK());
			//			// make sure the listener handles all event already that are dispatched asynchronously
			listener.latch.await(10, TimeUnit.SECONDS);
			assertTrue("Collect event wasn't dispatched.", listener.called);
			assertEquals("Collect event didn't report expected artifacts to be downloaded.", 19, listener.requestsNumber);
			assertTrue("Mirror event wasn't dispatched.", listener.mirrorEevent);
		} finally {
			getEventBus().removeListener(listener);
		}
	}

	@Test
	public void testPhaseEvent() throws ProvisionException, OperationCanceledException, InterruptedException {
		final String[] phaseSets = new String[] {PhaseSetFactory.PHASE_COLLECT, PhaseSetFactory.PHASE_CHECK_TRUST, PhaseSetFactory.PHASE_INSTALL, PhaseSetFactory.PHASE_CONFIGURE};

		class ProvTestListener implements ProvisioningListener {
			String publishUnWantedPhaseEvent = null;
			int publishUnWantedPhaseType = 0;
			List<String> phaseStartEventToBePublised = new ArrayList<String>(Arrays.asList(phaseSets));
			List<String> phaseEndEventToBePublised = new ArrayList<String>(Arrays.asList(phaseSets));
			CountDownLatch latch = new CountDownLatch(1);

			public void notify(EventObject o) {
				if (o instanceof PhaseEvent) {
					PhaseEvent event = (PhaseEvent) o;
					if (event.getType() == PhaseEvent.TYPE_START) {
						if (!phaseStartEventToBePublised.remove(event.getPhaseId()))
							publishUnWantedPhaseEvent = event.getPhaseId();
					} else if (event.getType() == PhaseEvent.TYPE_END) {
						if (!phaseEndEventToBePublised.remove(event.getPhaseId()))
							publishUnWantedPhaseEvent = event.getPhaseId();
					} else
						publishUnWantedPhaseType = event.getType();
				} else if (o instanceof CommitOperationEvent || o instanceof RollbackOperationEvent)
					latch.countDown();
			}
		}
		final ProvTestListener listener = new ProvTestListener();
		getEventBus().addListener(listener);
		try {
			URI repoLoc = getTestData("Load test data.", "/testData/testRepos/updateSite").toURI();
			IProfile profile = createProfile("test");
			ProvisioningContext context = new ProvisioningContext(getAgent());
			context.setArtifactRepositories(new URI[] {repoLoc});
			context.setMetadataRepositories(new URI[] {repoLoc});
			IProvisioningPlan plan = engine.createPlan(profile, context);
			IQueryResult<IInstallableUnit> allIUs = getMetadataRepositoryManager().loadRepository(repoLoc, null).query(QueryUtil.ALL_UNITS, null);
			for (IInstallableUnit iu : allIUs.toSet()) {
				plan.addInstallableUnit(iu);
			}
			IStatus status = engine.perform(plan, PhaseSetFactory.createPhaseSetIncluding(phaseSets), new NullProgressMonitor());
			assertTrue("Provisioning was failed.", status.isOK());
			// make sure the listener handles all event already that are dispatched asynchronously
			listener.latch.await(10, TimeUnit.SECONDS);
			assertNull("Published phase event with unwanted phase id.", listener.publishUnWantedPhaseEvent);
			assertEquals("Published unwanted type of phase event.", 0, listener.publishUnWantedPhaseType);
			assertEquals("Expected Phase start event is not published.", new ArrayList<String>(0), listener.phaseStartEventToBePublised);
			assertEquals("Expected Phase end event is not published.", new ArrayList<String>(0), listener.phaseEndEventToBePublised);
		} finally {
			getEventBus().removeListener(listener);
		}
	}

	@Test
	public void testConfigureUnConfigureEvent() throws InterruptedException {
		final String iuId = "test";
		class ProvTestListener implements ProvisioningListener {
			int preConfigureEvent = 0;
			int postConfigureEvent = 0;
			int preUnConfigureEvent = 0;
			int postUnConfigureEvent = 0;
			CountDownLatch latch = new CountDownLatch(2);

			public void notify(EventObject o) {
				if (o instanceof InstallableUnitEvent) {
					InstallableUnitEvent event = (InstallableUnitEvent) o;
					if (event.getPhase().equals(PhaseSetFactory.PHASE_CONFIGURE) && event.isConfigure() && event.getInstallableUnit().getId().equals(iuId)) {
						if (event.isPre())
							preConfigureEvent++;
						else if (event.isPost())
							postConfigureEvent++;
					} else if (event.getPhase().equals(PhaseSetFactory.PHASE_UNCONFIGURE) && event.isUnConfigure() && event.getInstallableUnit().getId().equals(iuId)) {
						if (event.isPre())
							preUnConfigureEvent++;
						else if (event.isPost())
							postUnConfigureEvent++;
					}
				} else if (o instanceof CommitOperationEvent || o instanceof RollbackOperationEvent)
					latch.countDown();
			}
		}
		final ProvTestListener listener = new ProvTestListener();
		getEventBus().addListener(listener);

		try {
			IProfile profile = createProfile("testConfigureEvent");
			IProvisioningPlan plan = engine.createPlan(profile, null);

			final String testLocation = "http://download.eclipse.org/releases/juno";
			// remove the existing location in case it has
			Map args = new HashMap();
			args.put(ActionConstants.PARM_AGENT, getAgent());
			args.put("location", testLocation);
			args.put("type", Integer.toString(IRepository.TYPE_ARTIFACT));
			args.put("enabled", "true");
			new RemoveRepositoryAction().execute(args);

			Map<String, ITouchpointInstruction> data = new HashMap<String, ITouchpointInstruction>();
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("location", testLocation);
			parameters.put("type", Integer.toString(IRepository.TYPE_ARTIFACT));
			parameters.put("name", "Juno");
			parameters.put("enabled", "true");
			data.put(PhaseSetFactory.PHASE_CONFIGURE, new TouchpointInstruction(TouchpointInstruction.encodeAction("addRepository", parameters), null));
			IInstallableUnit testIU = createResolvedIU(createEclipseIU(iuId, Version.create("1.0.0"), new IRequirement[0], new TouchpointData(data)));
			plan.addInstallableUnit(testIU);
			IStatus result = engine.perform(plan, PhaseSetFactory.createDefaultPhaseSet(), new NullProgressMonitor());
			assertTrue("0.2", result.isOK());
			Set<IInstallableUnit> installedIUs = profile.available(QueryUtil.ALL_UNITS, null).toUnmodifiableSet();
			assertEquals("0.3", 1, installedIUs.size());
			plan = engine.createPlan(profile, null);
			plan.removeInstallableUnit(testIU);
			result = engine.perform(plan, PhaseSetFactory.createDefaultPhaseSet(), new NullProgressMonitor());
			assertTrue("0.4", result.isOK());
			// make sure the listener handles all event already that are dispatched asynchronously
			listener.latch.await(10, TimeUnit.SECONDS);
			assertEquals("0.5", 1, listener.preConfigureEvent);
			assertEquals("0.6", 1, listener.postConfigureEvent);
			assertEquals("0.7", 1, listener.preUnConfigureEvent);
			assertEquals("0.8", 1, listener.postUnConfigureEvent);
		} finally {
			getEventBus().removeListener(listener);
		}
	}

	public static class AlwaysFail extends ProvisioningAction {

		@Override
		public IStatus execute(Map<String, Object> parameters) {
			int a = 1;
			if (a == 1)
				throw new NullPointerException("no reason");
			return null;
		}

		@Override
		public IStatus undo(Map<String, Object> parameters) {
			return null;
		}

	}

	@Test
	public void testConfigureUndoEvent() throws InterruptedException {
		final String iuId = "test";
		final String failureIU = "alwaysFail";
		class ProvTestListener implements ProvisioningListener {
			int preConfigureEvent = 0;
			int postConfigureEvent = 0;
			int preUnConfigureEventForUndo = 0;
			int postUnConfigureEventForUndo = 0;
			CountDownLatch latch = new CountDownLatch(1);

			public void notify(EventObject o) {
				if (o instanceof InstallableUnitEvent) {
					InstallableUnitEvent event = (InstallableUnitEvent) o;
					if (event.getPhase().equals(PhaseSetFactory.PHASE_CONFIGURE) && event.getInstallableUnit().getId().equals(iuId)) {
						if (event.isConfigure() && event.isPre())
							preConfigureEvent++;
						else if (event.isConfigure() && event.isPost())
							postConfigureEvent++;
						else if (event.isUnConfigure() && event.isPre())
							preUnConfigureEventForUndo++;
						else if (event.isUnConfigure() && event.isPost())
							postUnConfigureEventForUndo++;
					} else if (event.getPhase().equals(PhaseSetFactory.PHASE_CONFIGURE) && event.getInstallableUnit().getId().equals(failureIU)) {
						if (event.isConfigure() && event.isPre()) {
							preConfigureEvent++;
						} else if (event.isConfigure() && event.isPost())
							postConfigureEvent++;
						else if (event.isUnConfigure() && event.isPre())
							preUnConfigureEventForUndo++;
						else if (event.isUnConfigure() && event.isPost())
							postUnConfigureEventForUndo++;
					}
				} else if (o instanceof CommitOperationEvent || o instanceof RollbackOperationEvent)
					latch.countDown();
			}
		}
		final ProvTestListener listener = new ProvTestListener();
		getEventBus().addListener(listener);

		try {
			final String customTouchPoint = "<extension point=\"org.eclipse.equinox.p2.engine.actions\"> <action class=\"org.eclipse.equinox.p2.tests.engine.ProvisioningEventTest.AlwaysFail\" name=\"alwaysFail\" touchpointType=\"org.eclipse.equinox.p2.osgi\" touchpointVersion=\"1.0.0\" version=\"1.0.0\"></action></extension>";
			ByteArrayInputStream input = new ByteArrayInputStream(customTouchPoint.getBytes());
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			registry.addContribution(input, new RegistryContributor(TestActivator.PI_PROV_TESTS, "p2 tests", null, null), false, "Always Fail TouchPoint Action", null, ((ExtensionRegistry) registry).getTemporaryUserToken());

			IProfile profile = createProfile("testConfigureEvent");
			IProvisioningPlan plan = engine.createPlan(profile, null);
			Map<String, ITouchpointInstruction> data = new HashMap<String, ITouchpointInstruction>();
			data.put(PhaseSetFactory.PHASE_CONFIGURE, MetadataFactory.createTouchpointInstruction("instructionparsertest.goodAction()", null));
			IInstallableUnit testIU = createResolvedIU(createIU(iuId, Version.create("1.0.0"), null, new IRequirement[0], BUNDLE_CAPABILITY, NO_PROPERTIES, ITouchpointType.NONE, new TouchpointData(data), false));
			plan.addInstallableUnit(testIU);
			data = new HashMap<String, ITouchpointInstruction>(1);
			data.put(PhaseSetFactory.PHASE_CONFIGURE, new TouchpointInstruction("alwaysFail();", null));
			plan.addInstallableUnit(createResolvedIU(createEclipseIU(failureIU, Version.create("1.0.0"), new IRequirement[0], new TouchpointData(data))));
			IStatus result = engine.perform(plan, PhaseSetFactory.createDefaultPhaseSet(), new NullProgressMonitor());
			assertFalse(result.isOK());
			// make sure the listener handles all event already that are dispatched asynchronously
			listener.latch.await(10, TimeUnit.SECONDS);
			assertEquals("0.5", 2, listener.preConfigureEvent);
			assertEquals("0.6", 1, listener.postConfigureEvent);
			assertEquals("0.7", 1, listener.preUnConfigureEventForUndo);
			assertEquals("0.8", 2, listener.postUnConfigureEventForUndo);
		} finally {
			getEventBus().removeListener(listener);
		}
	}
}