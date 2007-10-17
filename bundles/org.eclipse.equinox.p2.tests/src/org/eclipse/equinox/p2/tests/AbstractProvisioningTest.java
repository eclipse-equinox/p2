/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.*;
import java.util.*;
import junit.framework.TestCase;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.director.IPlanner;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * Base class for provisioning tests with convenience methods used by multiple tests.
 */
public class AbstractProvisioningTest extends TestCase {
	protected static VersionRange ANY_VERSION = new VersionRange(Version.emptyVersion, true, new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), true);

	/**
	 * Tracks the metadata repositories created by this test instance. The repositories
	 * will be removed automatically at the end of the test.
	 */
	private List metadataRepos = new ArrayList();

	/**
	 * Fails the test due to the given throwable.
	 */
	public static void fail(String message, Throwable e) {
		// If the exception is a CoreException with a multistatus
		// then print out the multistatus so we can see all the info.
		if (e instanceof CoreException) {
			IStatus status = ((CoreException) e).getStatus();
			//if the status does not have an exception, print the stack for this one
			if (status.getException() == null)
				e.printStackTrace();
			write(status, 0);
		} else
			e.printStackTrace();
		fail(message + ": " + e);
	}

	private static void indent(OutputStream output, int indent) {
		for (int i = 0; i < indent; i++)
			try {
				output.write("\t".getBytes());
			} catch (IOException e) {
				// ignore
			}
	}

	private static void write(IStatus status, int indent) {
		PrintStream output = System.out;
		indent(output, indent);
		output.println("Severity: " + status.getSeverity());

		indent(output, indent);
		output.println("Plugin ID: " + status.getPlugin());

		indent(output, indent);
		output.println("Code: " + status.getCode());

		indent(output, indent);
		output.println("Message: " + status.getMessage());

		if (status.getException() != null) {
			indent(output, indent);
			output.print("Exception: ");
			status.getException().printStackTrace(output);
		}

		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			for (int i = 0; i < children.length; i++)
				write(children[i], indent + 1);
		}
	}

	public AbstractProvisioningTest() {
		super("");
	}

	public AbstractProvisioningTest(String name) {
		super(name);
	}

	public void assertEmptyProfile(Profile p) {
		assertNotNull("The profile should not be null", p);
		boolean containsIU = false;
		for (Iterator iterator = p.getInstallableUnits(); iterator.hasNext();) {
			containsIU = true;
		}
		if (containsIU)
			fail("The profile should be empty,profileId=" + p);
	}

	protected void assertNotIUs(IInstallableUnit[] ius, Iterator installableUnits) {
		Set notexpected = new HashSet();
		notexpected.addAll(Arrays.asList(ius));

		while (installableUnits.hasNext()) {
			IInstallableUnit next = (IInstallableUnit) installableUnits.next();
			if (notexpected.contains(next)) {
				fail("not expected [" + next + "]");
			}
		}
	}

	protected void assertNotOK(IStatus result) {
		assertTrue("The status should not have been OK", !result.isOK());
	}

	protected void assertOK(IStatus result) {
		if (result.isOK())
			return;

		if (result instanceof MultiStatus) {
			MultiStatus ms = (MultiStatus) result;
			IStatus children[] = ms.getChildren();
			for (int i = 0; i < children.length; i++) {
				System.err.println(children[i]);
			}
		}

		Throwable t = result.getException();
		if (t != null)
			t.printStackTrace();

		fail(result.toString());
	}

	/**
	 * Asserts that the given profile contains *only* the given IUs.
	 */
	protected void assertProfileContains(String message, Profile profile, IInstallableUnit[] expectedUnits) {
		HashSet expected = new HashSet(Arrays.asList(expectedUnits));
		for (Iterator it = profile.getInstallableUnits(); it.hasNext();) {
			IInstallableUnit actual = (IInstallableUnit) it.next();
			if (!expected.remove(actual))
				fail(message + " profile " + profile.getProfileId() + " contained an unexpected unit: " + actual);
		}
		if (!expected.isEmpty())
			fail(message + " profile " + profile.getProfileId() + " did not contain expected units: " + expected);
	}

	/**
	 * Asserts that the given profile contains all the given IUs.
	 */
	protected void assertProfileContainsAll(String message, Profile profile, IInstallableUnit[] expectedUnits) {
		HashSet expected = new HashSet(Arrays.asList(expectedUnits));
		for (Iterator it = profile.getInstallableUnits(); it.hasNext();) {
			IInstallableUnit actual = (IInstallableUnit) it.next();
			expected.remove(actual);
		}
		if (!expected.isEmpty())
			fail(message + " profile " + profile.getProfileId() + " did not contain expected units: " + expected);
	}

	public IDirector createDirector() {
		return (IDirector) ServiceHelper.getService(TestActivator.getContext(), IDirector.class.getName());
	}

	/**
	 * Creates and returns a correctly formatted LDAP filter with the given key and value.
	 */
	protected String createFilter(String filterKey, String filterValue) {
		return "(" + filterKey + '=' + filterValue + ')';
	}

	public IPlanner createPlanner() {
		return (IPlanner) ServiceHelper.getService(TestActivator.getContext(), IPlanner.class.getName());
	}

	/**
	 * Creates and returns a required capability with the provided attributes.
	 */
	protected RequiredCapability[] createRequiredCapabilities(String namespace, String name, String filter) {
		return createRequiredCapabilities(namespace, name, ANY_VERSION, filter);
	}

	/**
	 * Creates and returns a required capability with the provided attributes.
	 */
	protected RequiredCapability[] createRequiredCapabilities(String namespace, String name, VersionRange range, String filter) {
		return new RequiredCapability[] {new RequiredCapability(namespace, name, range, filter, false, false)};
	}

	/**
	 * Adds a test metadata repository to the system that provides the given units. 
	 * The repository will automatically be removed in the tearDown method.
	 */
	protected void createTestMetdataRepository(IInstallableUnit[] units) {
		IMetadataRepository repo = new TestMetadataRepository(units);
		IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
		assertNotNull(repoMan);
		repoMan.addRepository(repo);
		metadataRepos.add(repo);
	}

	public void printProfile(Profile toPrint) {
		boolean containsIU = false;
		for (Iterator iterator = toPrint.getInstallableUnits(); iterator.hasNext();) {
			System.out.println(iterator.next());
			containsIU = true;
		}
		if (!containsIU)
			System.out.println("No iu");
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		//remove all metadata repositories created by this test
		if (!metadataRepos.isEmpty()) {
			IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
			for (Iterator it = metadataRepos.iterator(); it.hasNext();) {
				IMetadataRepository repo = (IMetadataRepository) it.next();
				repoMan.removeRepository(repo);
			}
			metadataRepos.clear();
		}
	}
}
