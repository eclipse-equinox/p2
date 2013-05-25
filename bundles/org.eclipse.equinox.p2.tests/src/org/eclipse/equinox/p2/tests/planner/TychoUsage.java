package org.eclipse.equinox.p2.tests.planner;

import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.Projector;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class TychoUsage extends AbstractProvisioningTest {

	private IInstallableUnit topLevelIU;
	private IProfile profile;

	private void setupTopLevelIU() {
		IRequirement[] reqPlatform1 = new IRequirement[1];
		reqPlatform1[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "org.eclipse.emf.sdk.feature.group", new VersionRange("[2.7.2.v20120130-0943, 2.7.2.v20120130-0943]"), null, false, false, true);
		Properties p = new Properties();
		topLevelIU = createIU("topLevelIU", Version.create("1.0.0"), null, reqPlatform1, new IProvidedCapability[0], p, null, null, true);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setupTopLevelIU();
	}

	public void testEquivalentP2Call() throws ProvisionException, URISyntaxException {
		loadMetadataRepository(URIUtil.fromString("http://download.eclipse.org/releases/indigo"));
		profile = createProfile("TestProfile." + getName());
		IProfileChangeRequest pcr = getPlanner(getAgent()).createChangeRequest(profile);
		pcr.add(topLevelIU);
		System.out.println(System.currentTimeMillis());
		assertResolve(pcr, getPlanner(getAgent()));
		System.out.println(System.currentTimeMillis());
	}

	public void testTychoUsage() throws ProvisionException, URISyntaxException {
		IMetadataRepository repo = loadMetadataRepository(URIUtil.fromString("http://download.eclipse.org/releases/indigo"));
		IInstallableUnit newRoot1 = repo.query(QueryUtil.createIUQuery("org.eclipse.emf.sdk.feature.group", Version.create("2.7.2.v20120130-0943")), new NullProgressMonitor()).iterator().next();
		Collection<IInstallableUnit> newRoots = new ArrayList<IInstallableUnit>();
		newRoots.add(newRoot1);

		Map<String, String> context = new HashMap<String, String>();
		context.put("osgi.ws", "win32");
		context.put("osgi.os", "win32");
		context.put("osgi.arch", "x86_64");
		context.put("org.eclipse.update.install.features", "true");

		Slicer slicer = new Slicer(repo, context, false);
		IQueryable<IInstallableUnit> slice = slicer.slice(new IInstallableUnit[] {topLevelIU}, new NullProgressMonitor());

		Projector p = new Projector(slice, context, new HashSet<IInstallableUnit>(), false);
		p.encode(topLevelIU, new IInstallableUnit[0], new Collector<IInstallableUnit>(), newRoots, new NullProgressMonitor());
		IStatus result = p.invokeSolver(new NullProgressMonitor());
		assertTrue(result.isOK() || result.getSeverity() == IStatus.WARNING);
		assertFalse(p.extractSolution().isEmpty());
	}
}
