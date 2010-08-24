/******************************************************************************* 
* Copyright (c) 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import java.io.File;
import java.net.URI;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

/**
 *
 */
public class ContextRepositoryTest extends AbstractProvisioningTest {

	/**
	 * runs default director app.
	 */
	public class TestPublisherApplication extends AbstractPublisherApplication {

		/* (non-Javadoc)
		 * @see org.eclipse.equinox.p2.publisher.AbstractPublisherApplication#createActions()
		 */
		protected IPublisherAction[] createActions() {
			return new IPublisherAction[0];
		}

		public IPublisherInfo getInfo() {
			return info;
		}
	}

	public void testContextMetadataRepository() throws Exception {
		URI tempRepo = getTempFolder().toURI();
		File repository = new File(TestActivator.getTestDataFolder(), "metadataRepo/good");

		String[] firstRun = new String[] {"-mr", tempRepo.toString(), "-contextMetadata", repository.toURI().toString()};
		TestPublisherApplication application = new TestPublisherApplication();
		application.run(firstRun);
		assertNotNull(application.getInfo().getContextMetadataRepository());
	}

	public void testContextArtifactRepository() throws Exception {
		URI tempRepo = getTempFolder().toURI();
		File repository = new File(TestActivator.getTestDataFolder(), "artifactRepo/simple");

		String[] firstRun = new String[] {"-mr", tempRepo.toString(), "-contextArtifacts", repository.toURI().toString()};
		TestPublisherApplication application = new TestPublisherApplication();
		application.run(firstRun);
		assertNotNull(application.getInfo().getContextArtifactRepository());
	}
}
