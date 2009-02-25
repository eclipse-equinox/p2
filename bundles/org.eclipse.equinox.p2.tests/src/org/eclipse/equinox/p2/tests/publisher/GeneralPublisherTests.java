package org.eclipse.equinox.p2.tests.publisher;

import java.lang.reflect.Method;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.publisher.Messages;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAndBundlesPublisherApplication;

public class GeneralPublisherTests extends TestCase {

	public void testBug255820_Product_normalize() throws Exception {
		Method normalizeMethod = ProductFile.class.getDeclaredMethod("normalize", String.class);
		normalizeMethod.setAccessible(true);
		assertNotNull(normalizeMethod);

		assertEquals(normalizeMethod.invoke(null, "a b  c\td\ne"), "a b c d e");
		assertEquals(normalizeMethod.invoke(null, "a\fbd\r\n e"), "a bd e");
	}

	public void testInvalidConfiguration1() {
		FeaturesAndBundlesPublisherApplication application = new FeaturesAndBundlesPublisherApplication();
		Integer retValue = 0;
		try {
			retValue = (Integer) application.run(new String[0]);
		} catch (Exception e) {
			fail("0.99");
		}
		assertEquals("1.0", 1, retValue.intValue());
		assertEquals("1.1", Messages.exception_noMetadataRepo, application.getStatus().getMessage());
	}

	public void testInvalidConfiguration2() {
		FeaturesAndBundlesPublisherApplication application = new FeaturesAndBundlesPublisherApplication();
		Integer retValue = 0;
		try {
			retValue = (Integer) application.run(new String[] {"-metadataRepository foo", "-publishArtifacts"});
		} catch (Exception e) {
			fail("0.99");
		}
		assertEquals("1.0", 1, retValue.intValue());
		assertEquals("1.1", Messages.exception_noArtifactRepo, application.getStatus().getMessage());

	}
}
