package org.eclipse.equinox.p2.tests.planner.bug326624;

import java.io.*;
import java.util.*;
import junit.framework.TestCase;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.junit.Assert;

@SuppressWarnings("restriction")
public class OptionalGreedyTest extends TestCase {
	private static final IInstallableUnit[] IU_ARRAY = new IInstallableUnit[0];

	private IProgressMonitor monitor = new NullProgressMonitor();

	public void testResolve() throws IOException {
		Map<String, String> properties = new HashMap<String, String>();
		//		properties.put("osgi.ws", "gtk");
		//		properties.put("org.osgi.framework.executionenvironment", "OSGi/Minimum-1.0,OSGi/Minimum-1.1");
		//		properties.put("org.eclipse.update.install.features", "true");
		//		properties.put("osgi.os", "linux");
		//		properties.put("osgi.arch", "x86_64");
		//		properties.put("org.osgi.framework.system.packages", "");
		Map<String, String> newSelectionContext = SimplePlanner.createSelectionContext(properties);

		IQueryable<IInstallableUnit> slice = new CollectionResult<IInstallableUnit>(loadIUs("/Users/Pascal/dev/p2_36x/org.eclipse.equinox.p2.tests/src/org/eclipse/equinox/p2/tests/planner/bug326624/slice.xml"));
		Set<IInstallableUnit> rootIUs = loadIUs("/Users/Pascal/dev/p2_36x/org.eclipse.equinox.p2.tests/src/org/eclipse/equinox/p2/tests/planner/bug326624/root.xml");
		Set<IInstallableUnit> extraIUs = loadIUs("/Users/Pascal/dev/p2_36x/org.eclipse.equinox.p2.tests/src/org/eclipse/equinox/p2/tests/planner/bug326624/extra.xml");

		Projector projector = new Projector(slice, newSelectionContext, new HashSet<IInstallableUnit>(), false);
		projector.encode(createMetaIU(rootIUs), extraIUs.toArray(IU_ARRAY) /* alreadyExistingRoots */, new QueryableArray(IU_ARRAY) /* installed IUs */, rootIUs /* newRoots */, monitor);

		IStatus s = projector.invokeSolver(monitor);
		Assert.assertTrue(s.isOK());

		Collection<IInstallableUnit> newState = projector.extractSolution();

		Assert.assertEquals(97, newState.size());
	}

	protected Set<IInstallableUnit> loadIUs(String filename) throws FileNotFoundException, IOException {
		Set<IInstallableUnit> slice;
		MetadataIO io = new MetadataIO();
		FileInputStream is = new FileInputStream(filename);
		try {
			slice = io.readXML(is);
		} finally {
			is.close();
		}
		return slice;
	}

	private IInstallableUnit createMetaIU(Set<IInstallableUnit> rootIUs) {
		InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
		String time = Long.toString(System.currentTimeMillis());
		iud.setId(time);
		iud.setVersion(Version.createOSGi(0, 0, 0, time));

		ArrayList<IRequirement> requirements = new ArrayList<IRequirement>();
		for (IInstallableUnit iu : rootIUs) {
			VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
			requirements.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range, iu.getFilter(), 1 /* min */, iu.isSingleton() ? 1 : Integer.MAX_VALUE /* max */, true /* greedy */));
		}

		iud.setRequirements((IRequirement[]) requirements.toArray(new IRequirement[requirements.size()]));
		return MetadataFactory.createInstallableUnit(iud);
	}

}
