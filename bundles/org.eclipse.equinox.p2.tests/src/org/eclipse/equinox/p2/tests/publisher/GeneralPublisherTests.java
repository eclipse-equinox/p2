package org.eclipse.equinox.p2.tests.publisher;

import java.lang.reflect.Method;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;

public class GeneralPublisherTests extends TestCase {

	public void testBug255820_Product_normalize() throws Exception {
		Method normalizeMethod = ProductFile.class.getDeclaredMethod("normalize", String.class);
		normalizeMethod.setAccessible(true);
		assertNotNull(normalizeMethod);

		assertEquals(normalizeMethod.invoke(null, "a b  c\td\ne"), "a b c d e");
		assertEquals(normalizeMethod.invoke(null, "a\fbd\r\n e"), "a bd e");
	}
}
