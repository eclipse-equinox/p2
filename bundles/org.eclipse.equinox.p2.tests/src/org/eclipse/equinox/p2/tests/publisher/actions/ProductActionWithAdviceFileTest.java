/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import org.eclipse.equinox.p2.query.MatchQuery;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.QueryableFilterAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.TestData;

@SuppressWarnings({"unchecked"})
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

	class IUQuery extends MatchQuery {
		IInstallableUnit iu;

		public IUQuery(String id, Version version) {
			InstallableUnitDescription iuDescription = new InstallableUnitDescription();
			iuDescription.setId(id);
			iuDescription.setVersion(version);
			iu = MetadataFactory.createInstallableUnit(iuDescription);
		}

		public boolean isMatch(Object candidate) {
			if (iu.equals(candidate))
				return true;
			return false;
		}
	}

	public void testProductFileWithRepoAdvice() throws Exception {
		URI location;
		try {
			location = TestData.getFile("ProductActionTest", "contextRepos").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "platform.product").toString());
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		IMetadataRepository repository = metadataRepositoryManager.loadRepository(location, new NullProgressMonitor());
		testAction = new ProductAction(source, productFile, flavorArg, executablesFeatureLocation);
		PublisherInfo info = new PublisherInfo();
		info.setContextMetadataRepository(repository);
		info.addAdvice(new QueryableFilterAdvice(info.getContextMetadataRepository()));

		testAction.perform(info, publisherResult, null);
		IQueryResult results = publisherResult.query(new IUQuery("org.eclipse.platform.ide", Version.create("3.5.0.I20081118")), null);
		assertEquals("1.0", 1, queryResultSize(results));
		IInstallableUnit unit = (IInstallableUnit) results.iterator().next();
		Collection<IRequirement> requiredCapabilities = unit.getRequirements();

		IRequiredCapability capability = null;
		for (Iterator iterator = requiredCapabilities.iterator(); iterator.hasNext();) {
			IRequiredCapability req = (IRequiredCapability) iterator.next();
			if (req.getName().equals("org.eclipse.equinox.p2.user.ui.feature.group")) {
				capability = req;
				break;
			}
		}
		assertTrue("1.1", capability != null);
		assertEquals("1.2", InstallableUnit.parseFilter("(org.eclipse.update.install.features=true)"), capability.getFilter());
	}

	/**
	 * Tests publishing a product that contains an advice file (p2.inf)
	 */
	public void testProductWithAdviceFile() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest/productWithAdvice", "productWithAdvice.product").toString());
		testAction = new ProductAction(source, productFile, flavorArg, executablesFeatureLocation);
		testAction.perform(new PublisherInfo(), publisherResult, null);

		Collection productIUs = publisherResult.getIUs("productWithAdvice.product", IPublisherResult.NON_ROOT);
		assertEquals("1.0", 1, productIUs.size());
		IInstallableUnit product = (IInstallableUnit) productIUs.iterator().next();
		Collection<ITouchpointData> data = product.getTouchpointData();
		assertEquals("1.1", 1, data.size());
		String configure = data.iterator().next().getInstruction("configure").getBody();
		assertEquals("1.2", "addRepository(type:0,location:http${#58}//download.eclipse.org/releases/fred);addRepository(type:1,location:http${#58}//download.eclipse.org/releases/fred);", configure);
	}

}
