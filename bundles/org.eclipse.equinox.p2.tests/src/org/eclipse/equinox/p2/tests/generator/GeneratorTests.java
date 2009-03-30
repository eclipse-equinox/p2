/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.generator;

import java.io.File;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.p2.metadata.generator.EclipseGeneratorApplication;
import org.eclipse.equinox.internal.p2.metadata.generator.Messages;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;

public class GeneratorTests extends AbstractProvisioningTest {

	public void test233240_artifactsDeleted() throws Exception {
		//this also covers 220494
		File rootFolder = getTempFolder();

		//copy some bundles over 
		File plugins = new File(rootFolder, "plugins");
		plugins.mkdir();

		for (int i = 0; i < 3; i++) {
			BundleContext context = TestActivator.getContext();
			File bundle = FileLocator.getBundleFile(context.getBundle(i));

			copy("1.0 Populating input bundles.", bundle, new File(plugins, bundle.getName()));
		}

		String[] arguments = new String[] {"-metadataRepository", rootFolder.toURL().toExternalForm().toString(), "-artifactRepository", rootFolder.toURL().toExternalForm().toString(), "-source", rootFolder.getAbsolutePath(), "-publishArtifacts", "-noDefaultIUs"};
		EclipseGeneratorApplication application = new EclipseGeneratorApplication();
		application.run(arguments);

		assertTrue("2.0 - initial artifact repo existance", new File(rootFolder, "artifacts.xml").exists());
		assertTrue("2.1 - initial artifact repo contents", plugins.listFiles().length > 0);

		//Taunt you one more time
		application = new EclipseGeneratorApplication();
		try {
			application.run(arguments);
			fail("3.0 - Expected Illegal Argument Exception not thrown.");
		} catch (IllegalArgumentException e) {
			assertTrue("3.0 - Expected Illegal Argument", e.getMessage().equals(NLS.bind(Messages.exception_artifactRepoNoAppendDestroysInput, rootFolder.toURI())));
		}

		assertTrue("3.1 - artifact repo existance", new File(rootFolder, "artifacts.xml").exists());

		//with -updateSite
		arguments = new String[] {"-metadataRepository", rootFolder.toURL().toExternalForm().toString(), "-artifactRepository", rootFolder.toURL().toExternalForm().toString(), "-updateSite", rootFolder.getAbsolutePath(), "-publishArtifacts", "-noDefaultIUs"};
		application.run(arguments);

		assertTrue("4.0 - artifact repo existance", new File(rootFolder, "artifacts.xml").exists());
		assertTrue("4.1 - artifact repo contents", plugins.listFiles().length > 0);

		delete(rootFolder);
	}
}
