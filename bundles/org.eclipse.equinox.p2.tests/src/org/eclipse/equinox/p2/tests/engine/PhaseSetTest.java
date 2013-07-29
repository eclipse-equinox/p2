/*******************************************************************************
 *  Copyright (c) 2007, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.p2.repository.DownloadProgressEvent;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Util;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.junit.Test;

/**
 * Simple test of the engine API.
 */
public class PhaseSetTest extends AbstractProvisioningTest {
	PauseJob pause = null;

	public PhaseSetTest(String name) {
		super(name);
	}

	public PhaseSetTest() {
		super("");
	}

	public void testNullPhases() {
		try {
			new PhaseSet(null) {
				// empty PhaseSet
			};
		} catch (IllegalArgumentException exepcted) {
			return;
		}
		fail();
	}

	public void testNoTrustCheck() {
		IPhaseSet set1 = PhaseSetFactory.createDefaultPhaseSet();
		IPhaseSet set2 = PhaseSetFactory.createDefaultPhaseSetExcluding(new String[] {PhaseSetFactory.PHASE_CHECK_TRUST});
		assertTrue("1.0", !set1.equals(set2));
	}

	public void testEmptyPhases() {
		IProfile profile = createProfile("PhaseSetTest");
		PhaseSet phaseSet = new PhaseSet(new Phase[] {}) {
			// empty PhaseSet
		};
		InstallableUnitOperand op = new InstallableUnitOperand(createResolvedIU(createIU("iu")), null);
		InstallableUnitOperand[] operands = new InstallableUnitOperand[] {op};

		ProvisioningContext context = new ProvisioningContext(getAgent());
		IStatus result = phaseSet.perform(new EngineSession(null, profile, context), operands, new NullProgressMonitor());
		assertTrue(result.isOK());
	}

	@Test
	public void testPauseNotRunningPhaseSet() {
		PhaseSet set = (PhaseSet) PhaseSetFactory.createDefaultPhaseSet();
		assertFalse("Can pause not running phaseset.", set.pause());
	}

	@Test
	public void testResumeNotPausedPhaseSet() {
		PhaseSet set = (PhaseSet) PhaseSetFactory.createDefaultPhaseSet();
		assertFalse("Can resume not phaused phaseset.", set.resume());
	}

	abstract class PauseJob extends Job {
		public PauseJob(String name) {
			super(name);
		}

		private boolean isPaused = false;
		Job resume = null;

		public boolean isPaused() {
			return isPaused;
		}

		public void setPause(boolean paused) {
			isPaused = paused;
		}
	}

	@Test
	public void testPauseAndResume() throws ProvisionException, OperationCanceledException, InterruptedException {
		URI repoLoc = getTestData("Load test data.", "/testData/pausefeature").toURI();
		final PhaseSet phaseSet = (PhaseSet) PhaseSetFactory.createDefaultPhaseSet();
		pause = new PauseJob("pause") {
			protected IStatus run(IProgressMonitor monitor) {
				if (!phaseSet.pause())
					return new Status(IStatus.ERROR, TestActivator.PI_PROV_TESTS, "pause() failed.");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				setPause(true);
				resume = new Job("resume") {
					@Override
					protected IStatus run(IProgressMonitor monitor1) {
						pause.setPause(false);
						if (!phaseSet.resume())
							return new Status(IStatus.ERROR, TestActivator.PI_PROV_TESTS, "resume() failed.");
						return Status.OK_STATUS;
					}
				};
				resume.schedule(10000);
				return Status.OK_STATUS;
			}
		};
		basicTest(repoLoc, phaseSet, pause, QueryUtil.createIUQuery("org.eclipse.equinox.launcher"), IStatus.OK, null);
		assertTrue("Pause job is failed.", pause.getResult().isOK());
		pause.resume.join();
		assertTrue("Resume job is failed.", pause.resume.getResult().isOK());
	}

	private void basicTest(URI repoURI, PhaseSet phaseSet, final PauseJob pauseJob, IQuery<IInstallableUnit> query, int expectedCode, IProgressMonitor monitor) throws ProvisionException, InterruptedException {
		class ProvTestListener implements ProvisioningListener {
			boolean hasProvisioningEventAfterPaused = false;
			CountDownLatch latch = new CountDownLatch(1);
			boolean canStart = false;

			public void notify(EventObject o) {
				if (o instanceof BeginOperationEvent) {
					canStart = true;
				}
				if (o instanceof RepositoryEvent || o instanceof ProfileEvent)
					return;
				if (canStart && o instanceof DownloadProgressEvent) {
					// make sure to pause downloading after it has started
					pauseJob.schedule();
					canStart = false;
					return;
				}
				if (o instanceof CommitOperationEvent || o instanceof RollbackOperationEvent) {
					latch.countDown();
					pauseJob.cancel();
					return;
				}
				if (pauseJob.isPaused() && !(o instanceof PhaseEvent)) {
					hasProvisioningEventAfterPaused = true;
				}
			}
		}

		ProvTestListener listener = new ProvTestListener();
		getEventBus().addListener(listener);
		try {
			getMetadataRepositoryManager().loadRepository(repoURI, null);
			getArtifactRepositoryManager().loadRepository(repoURI, null);
			doProvisioning(repoURI, phaseSet, query, expectedCode, monitor);
			// make sure the listener handles all event already that are dispatched asynchronously
			listener.latch.await(10, TimeUnit.SECONDS);
			assertFalse("Engine still do provisioning after pausing.", listener.hasProvisioningEventAfterPaused);
			pauseJob.join();
		} finally {
			getEventBus().removeListener(listener);
		}
	}

	private void doProvisioning(URI repoLoc, final PhaseSet phaseSet, IQuery<IInstallableUnit> query, int expectedResult, IProgressMonitor monitor) throws ProvisionException {
		File testFolder = new File(System.getProperty("java.io.tmpdir"), "testProvisioning");
		delete(testFolder);
		testFolder.mkdir();
		final String profileId = "test";
		try {
			ProvisioningContext context = new ProvisioningContext(getAgent());
			context.setArtifactRepositories(new URI[] {repoLoc});
			context.setMetadataRepositories(new URI[] {repoLoc});
			IEngine engine = getEngine();
			// restrict the installation to 'linux & gtk & x86' to match the test repo
			Map props = new HashMap<String, String>();
			props.put(IProfile.PROP_ENVIRONMENTS, "osgi.ws=gtk,osgi.arch=x86,osgi.os=linux");
			props.put(IProfile.PROP_INSTALL_FOLDER, testFolder.getAbsolutePath());
			IProfile profile = createProfile(profileId, props);
			// clean cached artifacts
			IArtifactRepository artifactRepo = org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util.getBundlePoolRepository(getAgent(), profile);
			artifactRepo.removeAll(new NullProgressMonitor());
			ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(getAgent(), profile.getProfileId());
			IQueryResult<IInstallableUnit> toBeInstalledIUs = getMetadataRepositoryManager().loadRepository(repoLoc, null).query(query, null);
			assertFalse("Test case has problem to find IU to be installed.", toBeInstalledIUs.isEmpty());
			request.addAll(toBeInstalledIUs.toSet());
			IProvisioningPlan plan = getPlanner(getAgent()).getProvisioningPlan(request, context, null);
			assertTrue("Provisioning plan can't be resolved.", plan.getStatus().isOK());
			IStatus status = engine.perform(plan, phaseSet, monitor);
			assertEquals("The reture code of provisioning is not expected.", expectedResult, status.getSeverity());
		} finally {
			delete(testFolder);
			getProfileRegistry().removeProfile(profileId);
			Util.getDownloadCacheRepo(getAgent()).removeAll(new NullProgressMonitor());
		}
	}

	@Test
	public void testPauseAndResumeMoreThanOnce() throws ProvisionException, InterruptedException {
		//		URI repoLoc = URI.create("http://download.eclipse.org/releases/indigo");
		URI repoLoc = getTestData("Load test data.", "/testData/pausefeature").toURI();
		final PhaseSet phaseSet = (PhaseSet) PhaseSetFactory.createDefaultPhaseSet();
		final int threhold = 3;
		class ResumeJob extends Job {

			private PauseJob pauseJob;
			private int count = 0;

			public ResumeJob(String name, PauseJob pauseJob) {
				super(name);
				this.pauseJob = pauseJob;
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				pauseJob.setPause(false);
				if (!phaseSet.resume())
					return new Status(IStatus.INFO, TestActivator.PI_PROV_TESTS, "resume() failed.");
				if (count++ < threhold)
					pauseJob.schedule(10000);
				return Status.OK_STATUS;
			}
		}
		pause = new PauseJob("pause") {
			protected IStatus run(IProgressMonitor monitor) {
				if (!phaseSet.pause())
					return new Status(IStatus.INFO, TestActivator.PI_PROV_TESTS, "pause() failed.");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				setPause(true);
				if (resume == null)
					resume = new ResumeJob("resume", this);
				resume.schedule(10000);
				return Status.OK_STATUS;
			}
		};

		basicTest(repoLoc, phaseSet, pause, QueryUtil.createLatestQuery(QueryUtil.createIUQuery("org.eclipse.equinox.executable.feature.group")), IStatus.OK, null);
	}

	@Test
	public void testCancelPausedProvisioing() throws ProvisionException, InterruptedException {
		URI repoLoc = getTestData("Load test data.", "/testData/pausefeature").toURI();
		final PhaseSet phaseSet = (PhaseSet) PhaseSetFactory.createDefaultPhaseSet();
		class ProvListener implements ProvisioningListener {
			boolean hasDownloadEvent = false;

			public void notify(EventObject o) {
				if (o instanceof DownloadProgressEvent)
					hasDownloadEvent = true;
			}

		}
		final ProvListener listener = new ProvListener();
		getEventBus().addListener(listener);
		try {

			pause = new PauseJob("pause") {
				protected IStatus run(IProgressMonitor monitor) {
					while (!listener.hasDownloadEvent) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
					if (!phaseSet.pause())
						return new Status(IStatus.ERROR, TestActivator.PI_PROV_TESTS, "pause() failed.");
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					// wait seconds
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// 
					}
					setPause(true);
					return Status.OK_STATUS;
				}
			};

			basicTest(repoLoc, phaseSet, pause, QueryUtil.createLatestQuery(QueryUtil.createIUQuery("org.eclipse.equinox.executable.feature.group")), IStatus.CANCEL, new NullProgressMonitor() {
				@Override
				public boolean isCanceled() {
					return pause.isPaused();
				}
			});
		} finally {
			getEventBus().removeListener(listener);
		}
	}
}
