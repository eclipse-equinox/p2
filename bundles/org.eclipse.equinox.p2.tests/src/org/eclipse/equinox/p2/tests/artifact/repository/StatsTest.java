/*******************************************************************************
 * Copyright (c) 2012 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.*;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.testserver.helper.AbstractTestServerClientCase;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class StatsTest extends AbstractTestServerClientCase {

	private File repositoryFile;
	private URI repositoryURI;
	private SimpleArtifactRepository sourceRepo;
	private File targetLocation;
	private SimpleArtifactRepository targetRepository;
	private URI statsURL;
	private IMetadataRepository metaRepo;
	private File testInstall;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		IArtifactRepositoryManager artifactRepositoryManager = (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
		String tempDir = System.getProperty("java.io.tmpdir");
		repositoryFile = new File(tempDir, "SimpleArtifactRepositoryTest");
		AbstractProvisioningTest.delete(repositoryFile);
		repositoryURI = repositoryFile.toURI();
		Map properties = new HashMap();
		properties.put(IRepository.PROP_COMPRESSED, "true");
		statsURL = URI.create(getBaseURL() + "/stats");
		properties.put("p2.statsURI", statsURL.toString());
		sourceRepo = (SimpleArtifactRepository) artifactRepositoryManager.createRepository(repositoryURI, "artifact name", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);

		IArtifactDescriptor[] descriptors = new IArtifactDescriptor[2];
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", Version.create("1.2.3"));
		descriptors[0] = PublisherHelper.createArtifactDescriptor(key, null);
		((ArtifactDescriptor) descriptors[0]).setProperty("download.stats", "testKeyId");
		key = PublisherHelper.createBinaryArtifactKey("testKeyId2", Version.create("1.2.3"));
		descriptors[1] = PublisherHelper.createArtifactDescriptor(key, null);
		((ArtifactDescriptor) descriptors[1]).setProperty("download.stats", "testKeyId2");
		sourceRepo.addDescriptors(descriptors, null);

		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		properties = new HashMap();
		properties.put(IRepository.PROP_COMPRESSED, "true");
		metaRepo = manager.createRepository(repositoryURI, "TestRepo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);

		InstallableUnitDescription descriptor = new MetadataFactory.InstallableUnitDescription();
		descriptor.setId("testIuId");
		descriptor.setVersion(Version.create("1.0.0"));
		descriptor.setArtifacts(new IArtifactKey[] {key});
		Collection providedCaps = new ArrayList();
		providedCaps.add(MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, descriptor.getId(), descriptor.getVersion()));
		descriptor.addProvidedCapabilities(providedCaps);
		descriptor.setMetaRequirements(new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, descriptor.getId(), new VersionRange(descriptor.getVersion(), true, Version.MAX_VERSION, false), null, false, false)});
		descriptor.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(descriptor);
		metaRepo.addInstallableUnits(Arrays.asList(iu));

		for (int i = 0; i < descriptors.length; i++) {
			File artifactFile = new File(sourceRepo.getLocation(descriptors[i]));
			artifactFile.getParentFile().mkdirs();
			assertTrue("Failed to create binary artifact file.", artifactFile.createNewFile());
		}

		targetLocation = File.createTempFile("target", ".repo");
		AbstractProvisioningTest.delete(targetLocation);
		targetLocation.mkdirs();
		targetRepository = new SimpleArtifactRepository(getAgent(), "TargetRepo", targetLocation.toURI(), null);
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		//repository location is not used by all tests
		if (repositoryURI != null) {
			((IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME)).removeRepository(repositoryURI);
			((IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME)).removeRepository(repositoryURI);
			repositoryURI = null;
		}
		if (repositoryFile != null) {
			AbstractProvisioningTest.delete(repositoryFile);
			repositoryFile = null;
		}
		if (targetLocation != null) {
			((IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME)).removeRepository(targetLocation.toURI());
			AbstractProvisioningTest.delete(targetLocation);
			targetLocation = null;
		}
		if (testInstall != null) {
			AbstractProvisioningTest.delete(testInstall);
			testInstall = null;
		}
	}

	public void testCustomizedDownloadStats() throws CoreException, IOException {
		IArtifactKey key = new ArtifactKey(PublisherHelper.BINARY_ARTIFACT_CLASSIFIER, "testKeyId", Version.parseVersion("1.2.3"));
		MirrorRequest request = new MirrorRequest(key, targetRepository, null, null, (Transport) getAgent().getService(Transport.SERVICE_NAME), "package=test");
		request.perform(sourceRepo, new NullProgressMonitor());
		assertTrue("Failed on mirroring artifact.", request.getResult().isOK());

		checkStatsResult("test download number: 1");
	}

	protected void checkStatsResult(final String checkpoint) throws FileNotFoundException, CoreException, AuthenticationFailedException, IOException {
		final Transport transport = (Transport) getAgent().getService(Transport.SERVICE_NAME);
		BufferedReader statsResult = new BufferedReader(new InputStreamReader(transport.stream(statsURL, null)));
		try {
			String line = statsResult.readLine();
			while (line != null) {
				if (line.startsWith(checkpoint))
					return;
				line = statsResult.readLine();
			}
			fail("Didn't get expected stats result.");
		} finally {
			statsResult.close();
		}
	}

	public void testDownloadStatsWhileInstall() throws AuthenticationFailedException, FileNotFoundException, CoreException, IOException {
		IProfileRegistry registry = (IProfileRegistry) getAgent().getService(IProfileRegistry.SERVICE_NAME);
		final String profileName = "downloadStats";
		Map properties = new HashMap();
		properties.put(IProfile.PROP_STATS_PARAMETERS, "os=linux&ws=gtk&package=jee");
		String tempDir = System.getProperty("java.io.tmpdir");
		testInstall = new File(tempDir, "statsTestInstall");
		testInstall.mkdirs();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testInstall.toString());
		Profile profile = (Profile) registry.addProfile(profileName, properties);
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(metaRepo.query(QueryUtil.ALL_UNITS, null).toArray(IInstallableUnit.class));
		IPlanner planner = (IPlanner) getAgent().getService(IPlanner.SERVICE_NAME);
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setArtifactRepositories(new URI[] {repositoryURI});
		context.setMetadataRepositories(new URI[] {repositoryURI});
		IProvisioningPlan plan = planner.getProvisioningPlan(req, context, null);
		IEngine engine = (IEngine) getAgent().getService(IEngine.SERVICE_NAME);
		assertTrue("Failed on install test iu.", engine.perform(plan, null).isOK());
		profile = (Profile) registry.getProfile(profileName);
		assertEquals("Didn't install iu.", 1, profile.query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet().size());
		registry.removeProfile(profileName);
		checkStatsResult("jee download number: 1");
	}
}
