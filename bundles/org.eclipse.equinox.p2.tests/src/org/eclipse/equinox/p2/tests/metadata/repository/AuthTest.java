/*******************************************************************************
 * Copyright (c) 2006, 2010, Cloudsmith Inc.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 ******************************************************************************/

package org.eclipse.equinox.p2.tests.metadata.repository;

import java.net.URI;
import java.security.cert.Certificate;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.internal.p2.repository.RepositoryPreferences;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.UIServices;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

public class AuthTest extends ServerBasedTestCase {
	//	private static String UPDATE_SITE = "http://p2.piggott.ca/updateSite/";
	private String PRIVATE_REPO;
	private String NEVER_REPO;
	private IMetadataRepositoryManager mgr;
	private URI repoLoc;
	protected String authTestFailMessage;

	public void setUp() throws Exception {
		super.setUp();
		PRIVATE_REPO = super.getBaseURL() + "/private/mdr/composite/one";
		NEVER_REPO = super.getBaseURL() + "/proxy/never";
		mgr = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (mgr == null) {
			throw new RuntimeException("Repository manager could not be loaded");
		}
	}

	private void setUpRepo(String repo) throws Exception {
		repoLoc = new URI(repo);
		mgr.removeRepository(repoLoc);
		if (mgr.contains(repoLoc))
			throw new RuntimeException("Error - An earlier test did not leave a clean state - could not remove repo");

	}

	@Override
	public void tearDown() throws Exception {
		AllServerTests.setServiceUI(null); // cleanup hook
		super.tearDown();
		if (repoLoc != null)
			mgr.removeRepository(repoLoc);
	}

	public void testPrivateLoad() throws ProvisionException, Exception {
		AllServerTests.setServiceUI(new AladdinNotSavedService());
		setUpRepo(PRIVATE_REPO);
		try {
			mgr.loadRepository(repoLoc, null);
		} catch (OperationCanceledException e) {
			fail("The repository load was canceled - the UI auth service is probably not running");
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertTrue("Repository should have been added", mgr.contains(repoLoc));
	}

	public void testNeverLoad() throws ProvisionException, Exception {
		AladdinNotSavedService service;
		AllServerTests.setServiceUI(service = new AladdinNotSavedService());
		setUpRepo(NEVER_REPO);
		try {
			mgr.loadRepository(repoLoc, null);
		} catch (OperationCanceledException e) {
			fail("The repository load was canceled - the UI auth service is probably not running");
		} catch (ProvisionException e) {
			assertEquals("Repository is expected to report failed authentication", ProvisionException.REPOSITORY_FAILED_AUTHENTICATION, e.getStatus().getCode());
		}
		// note that preference includes the first attempt where user is not prompted
		assertEquals("There should have been N attempts", RepositoryPreferences.getLoginRetryCount() - 1, service.counter);
		assertFalse("Repository should not have been added", mgr.contains(repoLoc));

	}

	public class AladdinNotSavedService extends UIServices {
		public int counter = 0;

		public AuthenticationInfo getUsernamePassword(String location) {
			counter++;
			return new AuthenticationInfo("Aladdin", "open sesame", false);
		}

		public AuthenticationInfo getUsernamePassword(String location, AuthenticationInfo previousInfo) {
			counter++;
			assertEquals("Aladdin", previousInfo.getUserName());
			assertEquals("open sesame", previousInfo.getPassword());
			assertEquals(false, previousInfo.saveResult());
			return previousInfo;
		}

		/**
		 * Not used
		 */
		public TrustInfo getTrustInfo(Certificate[][] untrustedChain, String[] unsignedDetail) {
			return new TrustInfo(null, false, true);
		}
	}
}
