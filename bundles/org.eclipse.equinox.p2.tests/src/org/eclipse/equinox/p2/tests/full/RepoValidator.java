package org.eclipse.equinox.p2.tests.full;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceReference;

public class RepoValidator extends AbstractProvisioningTest {
	public void testValidate() throws ProvisionException, MalformedURLException {
		ServiceReference sr = TestActivator.context.getServiceReference(IPlanner.class.getName());
		if (sr == null) {
			throw new RuntimeException("Planner service not available");
		}
		IPlanner planner = (IPlanner) TestActivator.context.getService(sr);
		if (planner == null) {
			throw new RuntimeException("Planner could not be loaded");
		}

		ServiceReference sr2 = TestActivator.context.getServiceReference(IMetadataRepositoryManager.class.getName());
		IMetadataRepositoryManager mgr = (IMetadataRepositoryManager) TestActivator.context.getService(sr2);
		if (mgr == null) {
			throw new RuntimeException("Repository manager could not be loaded");
		}
		IMetadataRepository validatedRepo = mgr.loadRepository(new URL("file:d:/ganymedeM4"), null);

		Map properties = new HashMap();
		properties.put(IInstallableUnit.NAMESPACE_FLAVOR, "tooling");
		IProfile p = createProfile("repoValidator", null, properties);

		Query q;
		//		q = new Query() {
		//			public boolean isMatch(Object object) {
		//				if (!(object instanceof IInstallableUnit))
		//					return false;
		//				IInstallableUnit candidate = (IInstallableUnit) object;
		//				if (candidate.getId().startsWith("org.eclipse.jst") || candidate.getId().startsWith("org.eclipse.wst"))
		//					return true;
		//				return false;
		//			}
		//		};

		//		q = new InstallableUnitQuery("org.eclipse.wst.ws.parser");

		q = InstallableUnitQuery.ANY;
		Collector iusToTest = validatedRepo.query(q, new Collector(), null);

		ProvisioningContext pc = new ProvisioningContext(new URL[] {new URL("file:d:/ganymedeM4")});
		for (Iterator iterator = iusToTest.iterator(); iterator.hasNext();) {
			try {
				IInstallableUnit isInstallable = (IInstallableUnit) iterator.next();
				ProfileChangeRequest req = new ProfileChangeRequest(p);
				req.setProfileProperty("eclipse.p2.install.features", "true");
				req.addInstallableUnits(new IInstallableUnit[] {isInstallable});
				//				System.out.println("Working on: " + isInstallable);
				IStatus s = planner.getProvisioningPlan(req, pc, null).getStatus();
				if (!s.isOK()) {
					System.err.println("Can't resolve: " + isInstallable);
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				//ignore
			}
		}
	}
}
