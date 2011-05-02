/*******************************************************************************
 * Copyright (c) 2009, 2010, Cloudsmith Inc.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 ******************************************************************************/

package org.eclipse.equinox.p2.tests.metadata.repository;

import java.lang.reflect.Field;
import java.net.URI;
import java.security.cert.Certificate;
import java.text.MessageFormat;
import java.text.ParseException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.UIServices;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

/**
 * Test response to various HTTP status codes.
 */
public class HttpStatusTest extends ServerBasedTestCase {
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

	public void testStatusCodes() throws ProvisionException, Exception {
		AllServerTests.setServiceUI(new AladdinNotSavedService());
		// http codes with expected messages
		runSequence(400, 418);
		runSequence(422, 426);
		runSequence(449, 450);
		runSequence(500, 508);
		runSequence(510, 510);
	}

	public void testUnknownStatusCodes() throws ProvisionException, Exception {
		AllServerTests.setServiceUI(new AladdinNotSavedService());
		// undefined HTTP response codes.
		runSequence(419, 421);
		runSequence(427, 448);
		runSequence(511, 601);
	}

	public void testMultipleChoiceCode() throws ProvisionException, Exception {
		AllServerTests.setServiceUI(new AladdinNotSavedService());
		// undefined HTTP response codes.
		runSequence(300, 300);
	}

	private void runSequence(int from, int to) throws Exception {
		for (int i = from; i <= to; i++) {
			setUpRepo(super.getBaseURL() + "/status/" + Integer.valueOf(i).toString());

			try {
				mgr.loadRepository(repoLoc, null);
			} catch (OperationCanceledException e) {
				fail("The repository load was canceled - the UI auth service is probably not running");
			} catch (ProvisionException e) {

				IStatus status = e.getStatus();
				String msg = e.getMessage();

				// Print for human inspection
				System.out.print(String.format("HTTP %d => %s e-message: [%s]\n", //
						i, provisionCodeToText(status.getCode()), msg));

				// assert:
				// - that HTTP code => Repository Code is correct
				// - that correct message surfaces
				//					String m = org.eclipse.equinox.internal.p2.repository.Messages.TransportErrorTranslator_400;
				// Some codes have different message
				switch (i) {
					case 401 :
						// Authentication exception -
						// Assert the ProvisionException code
						assertEquals("Expected Provision Exception code for: " + Integer.valueOf(i), //
								ProvisionException.REPOSITORY_FAILED_AUTHENTICATION, status.getCode());
						break;
					case 300 : // fall through
					case 403 : // fall through
					case 404 : // Does not use the HTTP message.
						// No need to test the message text - any error would be discovered immediately
						// in the UI anyway.
						assertEquals("Expected Provision Exception code for: " + Integer.valueOf(i), //
								ProvisionException.REPOSITORY_NOT_FOUND, status.getCode());
						break;
					case 407 : // fall through
					default :
						// All other messages should surface
						try {
							MessageFormat msgFormat = new MessageFormat(getMessageForCode(i));
							msgFormat.parse(msg);
						} catch (ParseException p) {
							fail("The expected message was not returned for the code:" + Integer.valueOf(i));
						} catch (NoSuchFieldException nsf) {
							fail("The expected message was not returned for the code:" + Integer.valueOf(i));
						}
						assertEquals("Expected Provision Exception code for: " + Integer.valueOf(i), //
								ProvisionException.REPOSITORY_FAILED_READ, status.getCode());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			assertFalse("Repository should not have been added", mgr.contains(repoLoc));
		}
	}

	private static String getMessageForCode(int code) throws Exception {
		// use reflection on Messages class to get the string in use
		Class c = org.eclipse.equinox.internal.p2.repository.Messages.class;
		try {
			Field field = c.getDeclaredField("TransportErrorTranslator_" + Integer.valueOf(code).toString());
			return (String) field.get(null);
		} catch (NoSuchFieldException e) {
			Field field = c.getDeclaredField("TransportErrorTranslator_UnknownErrorCode");
			return (String) field.get(null);
		}
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
}
