/*******************************************************************************
 * Copyright (c) 2006-2009, Cloudsmith Inc.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 ******************************************************************************/

package org.eclipse.equinox.p2.tests.metadata.repository;

import java.net.URI;
import junit.framework.TestCase;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceReference;

public class AuthTest extends TestCase {
	//	private static String UPDATE_SITE = "http://p2.piggott.ca/updateSite/";
	private static String UPDATE_SITE = "http://localhost:8080/private/composite/one";
	private IMetadataRepositoryManager mgr;
	private URI repoLoc;

	protected void setUp() throws Exception {
		super.setUp();
		repoLoc = new URI(UPDATE_SITE);

		ServiceReference sr2 = TestActivator.context.getServiceReference(IMetadataRepositoryManager.class.getName());
		mgr = (IMetadataRepositoryManager) TestActivator.context.getService(sr2);
		if (mgr == null) {
			throw new RuntimeException("Repository manager could not be loaded");
		}
		mgr.removeRepository(repoLoc);
		if (mgr.contains(repoLoc))
			throw new RuntimeException("Error - An earlier test did not leave a clean state - could not remove repo");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		mgr.removeRepository(repoLoc);
	}

	public void testLoad() throws ProvisionException {
		boolean caught = false;
		try {
			mgr.loadRepository(repoLoc, null);
		} catch (OperationCanceledException e) {
			/* ignore - the operation is supposed to be canceled */
			caught = true;
		} catch (ProvisionException e) {
			caught = false;
		}
		assertTrue("Cancel should have been caught (1)", caught);

	}

}
