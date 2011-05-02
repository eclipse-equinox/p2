/*******************************************************************************
 * Copyright (c) 2009, 2010, Cloudsmith Inc.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 ******************************************************************************/

package org.eclipse.equinox.p2.tests.repository;

import java.net.URI;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.testserver.helper.AbstractTestServerClientCase;

/**
 * Test response to various HTTP status codes.
 */
public class NTLMTest extends AbstractTestServerClientCase {
	private IMetadataRepositoryManager mgr;
	private URI repoLoc;
	protected String authTestFailMessage;

	public void setUp() throws Exception {
		super.setUp();
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
		super.tearDown();
		if (repoLoc != null)
			mgr.removeRepository(repoLoc);
	}

	/**
	 * Test that a repeated status of 477 switches to JRE Http Client once.
	 * TODO - test is incomplete, there is no test that switch has taken place yet.
	 * @throws ProvisionException
	 * @throws Exception
	 */
	public void test477Status() throws ProvisionException, Exception {
		setUpRepo(super.getBaseURL() + "/status/477");

		try {
			mgr.loadRepository(repoLoc, null);
		} catch (OperationCanceledException e) {
			fail("The repository load was canceled - the UI auth service is probably not running");
		} catch (ProvisionException e) {

			IStatus status = e.getStatus();
			String msg = e.getMessage();

			// Print for human inspection - should be "REPOSITORY FAILED AUTHENTICATION
			// in this simple test (i.e. too many attempts)
			System.out.print(String.format("HTTP 477 => %s e-message: [%s]\n", //
					provisionCodeToText(status.getCode()), msg));

		} catch (Exception e) {
			e.printStackTrace();
		}
		assertFalse("Repository should not have been added", mgr.contains(repoLoc));

	}

	private static String provisionCodeToText(int code) {
		String msg = "REPOSITORY_";
		switch (code) {
			case ProvisionException.REPOSITORY_EXISTS :
				return msg + "EXISTS";
			case ProvisionException.REPOSITORY_FAILED_AUTHENTICATION :
				return msg + "FAILED_AUTHENTICATION";
			case ProvisionException.REPOSITORY_FAILED_READ :
				return msg + "FAILED_READ";
			case ProvisionException.REPOSITORY_FAILED_WRITE :
				return msg + "FAILED_WRITE";
			case ProvisionException.REPOSITORY_INVALID_LOCATION :
				return msg + "INVALID_LOCATION";
			case ProvisionException.REPOSITORY_NOT_FOUND :
				return msg + "NOT_FOUND";
			case ProvisionException.REPOSITORY_READ_ONLY :
				return msg + "READ_ONLY";
			case ProvisionException.REPOSITORY_UNKNOWN_TYPE :
				return msg + "UNKNOWN_TYPE";
			default :
				return msg + String.format("<unrecognized error code: %d >", code);
		}
	}

}
