package org.eclipse.equinox.p2.tests.planner;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.director.Projector;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class TychoUsage extends AbstractProvisioningTest {

	private IInstallableUnit topLevelIU;
	private IProfile profile;

	private void setupTopLevelIU() {
		IRequirement[] reqPlatform1 = new IRequirement[1];
		reqPlatform1[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID,
				"org.eclipse.emf.sdk.feature.group", new VersionRange("[2.27.0.v20210816-1137, 2.27.0.v20210816-1137]"),
				null, false, false, true);
		Map<String, String> p = new HashMap<>();
		topLevelIU = createIU("topLevelIU", Version.create("1.0.0"), null, reqPlatform1, new IProvidedCapability[0], p, null, null, true);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setupTopLevelIU();
	}

	public void testEquivalentP2Call() throws ProvisionException, URISyntaxException {
		loadMetadataRepository(URIUtil.fromString("https://download.eclipse.org/releases/2021-09"));
		profile = createProfile("TestProfile." + getName());
		IProfileChangeRequest pcr = getPlanner(getAgent()).createChangeRequest(profile);
		pcr.add(topLevelIU);
		System.out.println(System.currentTimeMillis());
		assertResolve(pcr, getPlanner(getAgent()));
		System.out.println(System.currentTimeMillis());
	}

	public void testTychoUsage() throws ProvisionException, URISyntaxException {
		IMetadataRepository repo = loadMetadataRepository(
				URIUtil.fromString("https://download.eclipse.org/releases/2021-09"));
		IInstallableUnit newRoot1 = repo.query(
				QueryUtil.createIUQuery("org.eclipse.emf.sdk.feature.group", Version.create("2.27.0.v20210816-1137")),
				new NullProgressMonitor()).iterator().next();
		Collection<IInstallableUnit> newRoots = new ArrayList<>();
		newRoots.add(newRoot1);

		Map<String, String> context = new HashMap<>();
		context.put("osgi.ws", "win32");
		context.put("osgi.os", "win32");
		context.put("osgi.arch", "x86_64");
		context.put("org.eclipse.update.install.features", "true");

		Slicer slicer = new Slicer(repo, context, false);
		IQueryable<IInstallableUnit> slice = slicer.slice(List.of(topLevelIU), new NullProgressMonitor());

		Projector p = new Projector(slice, context, new HashSet<>(), false);
		p.encode(topLevelIU, new IInstallableUnit[0], new Collector<>(), newRoots, new NullProgressMonitor());
		IStatus result = p.invokeSolver(new NullProgressMonitor());
		assertTrue(result.isOK() || result.getSeverity() == IStatus.WARNING);
		assertFalse(p.extractSolution().isEmpty());
	}
}
