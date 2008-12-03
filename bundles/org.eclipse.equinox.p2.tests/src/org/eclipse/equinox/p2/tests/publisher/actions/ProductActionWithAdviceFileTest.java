/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.TouchpointData;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.tests.TestData;

@SuppressWarnings( {"unchecked"})
/**
 * Tests the product action when run on a product file that has a corresponding
 * advice file (p2.inf).
 */
public class ProductActionWithAdviceFileTest extends ActionTest {

	File executablesFeatureLocation = null;
	String productLocation = "";
	String source = "";

	public void setUp() throws Exception {
		setupPublisherResult();
	}

	/**
	 * Tests publishing a product that contains an advice file (p2.inf)
	 */
	public void testProductWithAdviceFile() throws IOException {
		testAction = new ProductAction(source, TestData.getFile("ProductActionTest/productWithAdvice", "productWithAdvice.product").toString(), flavorArg, executablesFeatureLocation);
		testAction.perform(new PublisherInfo(), publisherResult, null);

		Collection productIUs = publisherResult.getIUs("productWithAdvice.product", IPublisherResult.NON_ROOT);
		assertEquals("1.0", 1, productIUs.size());
		IInstallableUnit product = (IInstallableUnit) productIUs.iterator().next();
		TouchpointData[] data = product.getTouchpointData();
		assertEquals("1.1", 1, data.length);
		String configure = data[0].getInstruction("configure").getBody();
		assertEquals("1.2", "addRepository(type:0,location:http${#58}//download.eclipse.org/releases/fred);addRepository(type:1,location:http${#58}//download.eclipse.org/releases/fred);", configure);
	}

}
