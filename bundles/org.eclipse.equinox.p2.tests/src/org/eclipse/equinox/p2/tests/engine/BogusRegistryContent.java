package org.eclipse.equinox.p2.tests.engine;

import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.TestActivator;

public class BogusRegistryContent extends TestCase {
	public void testBogusRegistry() {
		//		new SimpleProfileRegistry()
		File registryFolder = null;
		try {
			registryFolder = new File(FileLocator.resolve(TestActivator.getContext().getBundle().getEntry("testData/engineTest/bogusRegistryContent/")).getPath());
		} catch (IOException e) {
			fail("Test not properly setup");
		}
		SimpleProfileRegistry registry = new SimpleProfileRegistry(registryFolder, null, false);
		IProfile[] profiles = registry.getProfiles();
		assertNotNull(profiles);
		assertEquals(1, profiles.length);
	}
}
