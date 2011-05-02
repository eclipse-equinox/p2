/*******************************************************************************
 * Copyright (c) 2009, 2010, Cloudsmith Inc.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 ******************************************************************************/

package org.eclipse.equinox.p2.tests.metadata.repository;

import java.net.URI;
import java.security.cert.Certificate;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.UIServices;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

/**
 * Test handling of timeout.
 */
public class TimeoutTest extends ServerBasedTestCase {
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
		AllServerTests.setServiceUI(null); // cleanup hook
		super.tearDown();
		if (repoLoc != null)
			mgr.removeRepository(repoLoc);
	}

	/**
	 * Test that timeout occurs, that the expected exception is thrown, and with correct detail
	 * and message.
	 * Note that test takes at least 120 seconds to complete due to length of timeout.
	 * @throws ProvisionException
	 * @throws Exception
	 */
	public void testTimeout() throws ProvisionException, Exception {
		System.out.print("Note that test takes at least 120 seconds before timing out\n");
		AllServerTests.setServiceUI(new AladdinNotSavedService());
		setUpRepo(super.getBaseURL() + "/timeout/");

		try {
			mgr.loadRepository(repoLoc, null);
		} catch (OperationCanceledException e) {
			fail("The repository load was canceled - the UI auth service is probably not running");
		} catch (ProvisionException e) {

			IStatus status = e.getStatus();
			Throwable ex = status.getException();
			String msg = e.getMessage();
			if (ex instanceof CoreException)
				msg = ((CoreException) ex).getStatus().getMessage();

			// Print for human inspection
			System.out.print(String.format("%s e-message: [%s], detail:[%s]\n", //
					provisionCodeToText(status.getCode()), msg, ex != null ? ex.getMessage() : "<no detailed message>"));
			assertEquals("Socket timeout exception should be found as detail", ex.getClass(), java.net.SocketTimeoutException.class);

		} catch (Exception e) {
			e.printStackTrace();
		}
		assertFalse("Repository should not have been added", mgr.contains(repoLoc));
	}

	/**
	 * Test that it is possible to cancel a repository load that hangs on a HEAD request.
	 * Note that test takes at least 10 seconds (the cancel delay time). The real timeout is
	 * 120 seconds.
	 * @throws ProvisionException
	 * @throws Exception
	 */
	public void testTimeoutCancelation() throws ProvisionException, Exception {
		System.out.print("Note that test takes at least 10 seconds before timing out (and >120 if it fails)\n");

		AllServerTests.setServiceUI(new AladdinNotSavedService());
		setUpRepo(super.getBaseURL() + "/timeout/");

		IProgressMonitor monitor = new NullProgressMonitor();
		MonitorCancelation cancelHandler = new MonitorCancelation(monitor, 10000);
		Thread proc = new Thread(cancelHandler, "cancelHandler");
		proc.start();
		boolean caught = false;
		long startTime = System.currentTimeMillis();
		try {
			mgr.loadRepository(repoLoc, monitor);
		} catch (OperationCanceledException e) {
			caught = true;
		} catch (ProvisionException e) {
			// should not happen, but if it does, do some printout
			IStatus status = e.getStatus();
			Throwable ex = status.getException();
			String msg = e.getMessage();
			if (ex instanceof CoreException)
				msg = ((CoreException) ex).getStatus().getMessage();

			// Print for human inspection
			System.out.print(String.format("%s e-message: [%s], detail:[%s]\n", //
					provisionCodeToText(status.getCode()), msg, ex != null ? ex.getMessage() : "<no detailed message>"));

			fail("Provision exception should not have occured");

		} catch (Exception e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		assertTrue("The timeout chould have been canceled", caught);
		assertTrue("The cancel should happen before the timeout", endTime - startTime < 50000);

		// ignore testing if repo was loaded - it may or may not, depending on where the cancellation took place.
		// commented code kept, in case there is a change in API - should cancellation of load keep the repository.
		// assertFalse("Repository should not have been added", mgr.contains(repoLoc));
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

	public static class MonitorCancelation implements Runnable {
		private IProgressMonitor theMonitor;
		private long theDelay;

		MonitorCancelation(IProgressMonitor monitor, long delay) {
			theMonitor = monitor;
			theDelay = delay;
		}

		public void run() {
			try {
				Thread.sleep(theDelay);
			} catch (InterruptedException e) {
				/* ignore */
			}
			System.out.print("TimeoutTest: Cancelling monitor\n");
			theMonitor.setCanceled(true);

		}
	}
}
