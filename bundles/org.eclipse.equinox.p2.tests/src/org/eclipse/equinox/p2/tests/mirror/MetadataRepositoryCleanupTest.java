/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.mirror;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.artifact.mirror.MirrorApplication;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.Bundle;

/**
 * Test to ensure MirrorApplication handles loading an removing repositories correctly
 */
public class MetadataRepositoryCleanupTest extends AbstractProvisioningTest {
	protected File destRepoLocation;
	protected File sourceRepoLocation; //helloworldfeature
	protected File bystanderRepoLocation; //anotherfeature

	//TODO change to an abstracted type when API is available
	private MetadataRepositoryManager getMetadataRepositoryManager() {
		return (MetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
	}

	protected void setUp() throws Exception {
		super.setUp();
		//load all the repositories
		sourceRepoLocation = getTestData("1.0", "/testData/mirror/mirrorSourceRepo1");
		bystanderRepoLocation = getTestData("2.0", "/testData/mirror/mirrorSourceRepo2");

		//create destination location
		String tempDir = System.getProperty("java.io.tmpdir");
		destRepoLocation = new File(tempDir, "BasicMirrorApplicationTest");
		AbstractProvisioningTest.delete(destRepoLocation);
	}

	protected void tearDown() throws Exception {
		//remove all the repositories
		getMetadataRepositoryManager().removeRepository(destRepoLocation.toURL());
		getMetadataRepositoryManager().removeRepository(sourceRepoLocation.toURL());
		getMetadataRepositoryManager().removeRepository(bystanderRepoLocation.toURL());

		//delete the destination location (no left over files for the next test)
		delete(destRepoLocation);
		super.tearDown();
	}

	/**
	 * runs default mirror. source is the source repo, destination is the destination repo
	 */
	private void runMirrorApplication(final File source, final File destination, final boolean append) throws Exception {
		MirrorApplication application = new MirrorApplication();
		application.start(new IApplicationContext() {

			public void applicationRunning() {
			}

			public Map getArguments() {
				Map arguments = new HashMap();

				try {
					arguments.put(IApplicationContext.APPLICATION_ARGS, new String[] {"-source", source.toURL().toExternalForm(), "-destination", destination.toURL().toExternalForm(), append ? "-append" : ""});

				} catch (MalformedURLException e) {
					fail("invalid URL for source or target repo");
				}

				return arguments;
			}

			public String getBrandingApplication() {
				return null;
			}

			public Bundle getBrandingBundle() {
				return null;
			}

			public String getBrandingDescription() {
				return null;
			}

			public String getBrandingId() {
				return null;
			}

			public String getBrandingName() {
				return null;
			}

			public String getBrandingProperty(String key) {
				return null;
			}
		});
	}

	/**
	 * Ensures that if the mirror application is run with neither source nor destination loaded
	 * then neither will remain loaded after the application finishes
	 */
	public void testMetadataMirrorRemovesRepos() {
		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("1.0", e);
		}

		try {
			//TODO modify the contains statement once the API is available
			assertFalse(getMetadataRepositoryManager().contains(sourceRepoLocation.toURL()));
			//TODO modify the contains statement once the API is available
			assertFalse(getMetadataRepositoryManager().contains(destRepoLocation.toURL()));
		} catch (MalformedURLException e) {
			fail("1.1", e);
		}
	}

	/**
	 * Ensures that if the mirror application is run with only the source loaded
	 * then the source, and only the source, remains loaded after the application finishes
	 */
	public void testMetadataMirrorRemovesReposWithSourceLoaded() {
		try {
			//Load the source
			getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null);
		} catch (ProvisionException e) {
			fail("2.0", e);
		} catch (MalformedURLException e) {
			fail("2.1", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("2.2", e);
		}

		try {
			//TODO modify the contains statement once the API is available
			assertTrue(getMetadataRepositoryManager().contains(sourceRepoLocation.toURL()));
			//TODO modify the contains statement once the API is available
			assertFalse(getMetadataRepositoryManager().contains(destRepoLocation.toURL()));
		} catch (MalformedURLException e) {
			fail("2.3", e);
		}
	}

	/**
	 * Ensures that if the mirror application is run with only the destination loaded
	 * then the destination, and only the destination, remains loaded after the application finishes
	 */
	public void testMetadataMirrorRemovesReposWithDestinationLoaded() {
		try {
			//Load (by creating) the destination
			String repositoryName = destRepoLocation.toURL() + " - metadata"; //$NON-NLS-1$
			getMetadataRepositoryManager().createRepository(destRepoLocation.toURL(), repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("3.0", e);
		} catch (MalformedURLException e) {
			fail("3.1", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("3.2", e);
		}

		try {
			//TODO modify the contains statement once the API is available
			assertTrue(getMetadataRepositoryManager().contains(destRepoLocation.toURL()));
			//TODO modify the contains statement once the API is available
			assertFalse(getMetadataRepositoryManager().contains(sourceRepoLocation.toURL()));
		} catch (MalformedURLException e) {
			fail("3.3", e);
		}
	}

	/**
	 * Ensures that is the mirror application is run with both the source and destination loaded
	 * then both will remain loaded after the application has finished
	 */
	public void testMetadataMirrorRemovesReposWithBothLoaded() {
		try {
			//Load (by creating) the destination
			String repositoryName = destRepoLocation.toURL() + " - metadata"; //$NON-NLS-1$
			getMetadataRepositoryManager().createRepository(destRepoLocation.toURL(), repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			//Load the source
			getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null);
		} catch (ProvisionException e) {
			fail("4.0", e);
		} catch (MalformedURLException e) {
			fail("4.1", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("4.2", e);
		}

		try {
			//TODO modify the contains statement once the API is available
			assertTrue(getMetadataRepositoryManager().contains(destRepoLocation.toURL()));
			//TODO modify the contains statement once the API is available
			assertTrue(getMetadataRepositoryManager().contains(sourceRepoLocation.toURL()));
		} catch (MalformedURLException e) {
			fail("4.3", e);
		}
	}

	/**
	 * Ensures that if the mirror application is run with neither source nor destination loaded
	 * then neither will remain loaded after the application finishes
	 * Also ensures that the mirror application does not alter the state of any repository other
	 * than the source or destination
	 */
	public void testMetadataMirrorRemovesReposWithBystanderLoaded() {
		//Load the bystander repository. This should not be effected by the mirror application
		try {
			getMetadataRepositoryManager().loadRepository(bystanderRepoLocation.toURL(), null);
		} catch (ProvisionException e) {
			// TODO Auto-generated catch block
			fail("5.0", e);
		} catch (MalformedURLException e) {
			fail("5.1", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("5.2", e);
		}

		try {
			//TODO modify the contains statement once the API is available
			assertFalse(getMetadataRepositoryManager().contains(sourceRepoLocation.toURL()));
			//TODO modify the contains statement once the API is available
			assertFalse(getMetadataRepositoryManager().contains(destRepoLocation.toURL()));
			//Ensure bystander was not effected by the mirror application
			//TODO modify the contains statement once the API is available
			assertTrue(getMetadataRepositoryManager().contains(bystanderRepoLocation.toURL()));
		} catch (MalformedURLException e) {
			fail("5.3", e);
		}
	}

	/**
	 * Ensures that if the mirror application is run with only the source loaded
	 * then the source, and only the source, remains loaded after the application finishes
	 * Also ensures that the mirror application does not alter the state of any repository other
	 * than the source or destination
	 */
	public void testMetadataMirrorRemovesReposWithSourceAndBystanderLoaded() {
		try {
			//Load the bystander repository. This should not be effected by the mirror application
			getMetadataRepositoryManager().loadRepository(bystanderRepoLocation.toURL(), null);
			//Load the source
			getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null);
		} catch (ProvisionException e) {
			fail("6.0", e);
		} catch (MalformedURLException e) {
			fail("6.1", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("6.2", e);
		}

		try {
			//TODO modify the contains statement once the API is available
			assertTrue(getMetadataRepositoryManager().contains(sourceRepoLocation.toURL()));
			//TODO modify the contains statement once the API is available
			assertFalse(getMetadataRepositoryManager().contains(destRepoLocation.toURL()));
			//Ensure bystander was not effected by the mirror application
			//TODO modify the contains statement once the API is available
			assertTrue(getMetadataRepositoryManager().contains(bystanderRepoLocation.toURL()));
		} catch (MalformedURLException e) {
			fail("6.3", e);
		}
	}

	/**
	 * Ensures that if the mirror application is run with the destination loaded
	 * then the destination, and only the destination, remains loaded after the application finishes
	 * Also ensures that the mirror application does not alter the state of any repository other
	 * than the source or destination
	 */
	public void testMetadataMirrorRemovesReposWithDestinationAndBystanderLoaded() {
		try {
			//Load the bystander repository. This should not be effected by the mirror application
			getMetadataRepositoryManager().loadRepository(bystanderRepoLocation.toURL(), null);
			//Load (by creating) the destination
			String repositoryName = destRepoLocation.toURL() + " - metadata"; //$NON-NLS-1$
			getMetadataRepositoryManager().createRepository(destRepoLocation.toURL(), repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("7.0", e);
		} catch (MalformedURLException e) {
			fail("7.1", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("7.2", e);
		}

		try {
			//TODO modify the contains statement once the API is available
			assertTrue(getMetadataRepositoryManager().contains(destRepoLocation.toURL()));
			//TODO modify the contains statement once the API is available
			assertFalse(getMetadataRepositoryManager().contains(sourceRepoLocation.toURL()));
			//Ensure bystander was not effected by the mirror application
			//TODO modify the contains statement once the API is available
			assertTrue(getMetadataRepositoryManager().contains(bystanderRepoLocation.toURL()));
		} catch (MalformedURLException e) {
			fail("7.3", e);
		}
	}

	/**
	 * Ensures that is the mirror application is run with both the source and destination loaded
	 * then both will remain loaded after the application has finished
	 * Also ensures that the mirror application does not alter the state of any repository other
	 * than the source or destination
	 */
	public void testMetadataMirrorRemovesReposWithBothAndBystanderLoaded() {
		try {
			//Load the bystander repository. This should not be effected by the mirror application
			getMetadataRepositoryManager().loadRepository(bystanderRepoLocation.toURL(), null);
			//Load (by creating) the destination
			String repositoryName = destRepoLocation.toURL() + " - metadata"; //$NON-NLS-1$
			getMetadataRepositoryManager().createRepository(destRepoLocation.toURL(), repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			//Load the source
			getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null);
		} catch (ProvisionException e) {
			fail("8.0", e);
		} catch (MalformedURLException e) {
			fail("8.1", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("8.2", e);
		}

		try {
			//TODO modify the contains statement once the API is available
			assertTrue(getMetadataRepositoryManager().contains(destRepoLocation.toURL()));
			//TODO modify the contains statement once the API is available
			assertTrue(getMetadataRepositoryManager().contains(sourceRepoLocation.toURL()));
			//Ensure bystander was not effected by the mirror application
			//TODO modify the contains statement once the API is available
			assertTrue(getMetadataRepositoryManager().contains(bystanderRepoLocation.toURL()));
		} catch (MalformedURLException e) {
			fail("8.3", e);
		}
	}
}