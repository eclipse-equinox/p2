/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ql;

import java.net.URI;
import java.util.Hashtable;
import java.util.Iterator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.p2.director.app.Activator;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.ql.ExpressionQuery;
import org.eclipse.equinox.p2.ql.PredicateQuery;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PerformanceTest extends AbstractProvisioningTest {
	public void testCapabilityQueryPerformance() throws Exception {

		IMetadataRepository repo = getMDR("/testData/galileoM7");

		IRequiredCapability capability = MetadataFactory.createRequiredCapability("org.eclipse.equinox.p2.eclipse.type", "feature", new VersionRange("[1.0.0,2.0.0)"), null, false, false);
		CapabilityQuery capabilityQuery = new CapabilityQuery(capability);
		PredicateQuery predicateQuery = new PredicateQuery("item ~= $0", capability);
		Collector result;
		long tradQueryMS = 0;
		long exprQueryMS = 0;

		for (int i = 0; i < 10; ++i) {
			long start = System.currentTimeMillis();
			for (int idx = 0; idx < 100; ++idx) {
				result = repo.query(capabilityQuery, new Collector(), new NullProgressMonitor());
				assertEquals(result.size(), 487);
			}
			tradQueryMS += (System.currentTimeMillis() - start);

			start = System.currentTimeMillis();
			for (int idx = 0; idx < 100; ++idx) {
				result = repo.query(predicateQuery, new Collector(), new NullProgressMonitor());
				assertEquals(result.size(), 487);
			}
			exprQueryMS += (System.currentTimeMillis() - start);
		}
		System.out.println("CapabilityQuery took: " + tradQueryMS + " milliseconds");
		System.out.println("PredicateQuery took: " + exprQueryMS + " milliseconds");

		// Assert that expression query is not worse off then 4 / 3 ratio (should typically
		// be better then that.
		assertTrue(tradQueryMS * 4 > exprQueryMS * 3);
	}

	public void testCapabilityQueryPerformance2() throws Exception {

		IMetadataRepository repo = getMDR("/testData/galileoM7");

		IRequiredCapability[] capabilities = new IRequiredCapability[] {//
		MetadataFactory.createRequiredCapability("org.eclipse.equinox.p2.eclipse.type", "feature", new VersionRange("[1.0.0,2.0.0)"), null, false, false), //
				MetadataFactory.createRequiredCapability("org.eclipse.equinox.p2.localization", "df_LT", new VersionRange("[1.0.0,2.0.0)"), null, false, false)//
		};
		CapabilityQuery capabilityQuery = new CapabilityQuery(capabilities);
		PredicateQuery predicateQuery = new PredicateQuery("$0.all(rq | item ~= rq)", capabilities);
		Collector result;
		long tradQueryMS = 0;
		long exprQueryMS = 0;

		for (int i = 0; i < 10; ++i) {
			long start = System.currentTimeMillis();
			for (int idx = 0; idx < 100; ++idx) {
				result = repo.query(capabilityQuery, new Collector(), new NullProgressMonitor());
				assertEquals(result.size(), 446);
			}
			tradQueryMS += (System.currentTimeMillis() - start);

			start = System.currentTimeMillis();
			for (int idx = 0; idx < 100; ++idx) {
				result = repo.query(predicateQuery, new Collector(), new NullProgressMonitor());
				assertEquals(result.size(), 446);
			}
			exprQueryMS += (System.currentTimeMillis() - start);
		}
		System.out.println("CapabilityQuery took: " + tradQueryMS + " milliseconds");
		System.out.println("PredicateQuery took: " + exprQueryMS + " milliseconds");

		// Assert that expression query is not worse off then 4 / 3 ratio (should typically
		// be better then that.
		assertTrue(tradQueryMS * 4 > exprQueryMS * 3);
	}

	public void testIUPropertyQueryPerformance() throws Exception {

		IMetadataRepository repo = getMDR("/testData/galileoM7");

		IUPropertyQuery propertyQuery = new IUPropertyQuery("df_LT.providerName", "Eclipse.org");
		PredicateQuery predicateQuery = new PredicateQuery("properties[$0] == $1", "df_LT.providerName", "Eclipse.org");
		Collector result;
		long tradQueryMS = 0;
		long exprQueryMS = 0;

		for (int i = 0; i < 10; ++i) {
			long start = System.currentTimeMillis();
			for (int idx = 0; idx < 100; ++idx) {
				result = repo.query(propertyQuery, new Collector(), new NullProgressMonitor());
				assertEquals(result.size(), 965);
			}
			tradQueryMS += (System.currentTimeMillis() - start);

			start = System.currentTimeMillis();
			for (int idx = 0; idx < 100; ++idx) {
				result = repo.query(predicateQuery, new Collector(), new NullProgressMonitor());
				assertEquals(result.size(), 965);
			}
			exprQueryMS += (System.currentTimeMillis() - start);
		}
		System.out.println("IUPropertyQuery took: " + tradQueryMS + " milliseconds");
		System.out.println("PredicateQuery took: " + exprQueryMS + " milliseconds");

		// Assert that expression query is not worse off then 4 / 3 ratio (should typically
		// be better then that.
		assertTrue(tradQueryMS * 4 > exprQueryMS * 3);
	}

	public void testSlicerPerformance() throws Exception {
		Hashtable env = new Hashtable();
		env.put("osgi.os", "linux");
		env.put("osgi.ws", "gtk");
		env.put("osgi.arch", "x86");

		IMetadataRepository repo = getMDR("/testData/galileoM7");
		Collector c = repo.query(new InstallableUnitQuery("org.eclipse.sdk.feature.group", Version.create("3.5.0.v20090423-7Q7bA7DPR-wM38__Q4iRsmx9z0KOjbpx3AbyvXd-Uq7J2")), new Collector(), new NullProgressMonitor());
		Iterator itor = c.iterator();
		assertTrue(itor.hasNext());
		IInstallableUnit[] roots = new IInstallableUnit[] {(IInstallableUnit) itor.next()};
		Slicer slicer = new Slicer(repo, env, false);

		long startTime = System.currentTimeMillis();
		IQueryable slice = slicer.slice(roots, new NullProgressMonitor());

		c = slice.query(new MatchQuery() {
			public boolean isMatch(Object value) {
				return true;
			}
		}, new Collector(), new NullProgressMonitor());
		long slicerTime = System.currentTimeMillis() - startTime;

		assertEquals(c.size(), 411);
		System.out.print("Slicer took: ");
		System.out.println(slicerTime);

		IQuery query = new ExpressionQuery(//
				"$0.traverse(set(), _, {requirementsCache, parent | select(" + //
						"parent.requiredCapabilities.unique(requirementsCache).select(rc | rc.filter == null || $1 ~= filter(rc.filter)), _," + //
						"{rcs, child | rcs.exists(rc | child ~= rc)})})", roots, env);

		startTime = System.currentTimeMillis();
		c = repo.query(query, new Collector(), new NullProgressMonitor());
		long traverseTime = System.currentTimeMillis() - startTime;

		assertEquals(c.size(), 411);
		System.out.print("Traverse expression took: ");
		System.out.println(traverseTime);

		// Assert that expression query is at least 3/1 ratio ratio (should typically
		// be better then that.
		assertTrue(traverseTime * 3 < slicerTime);
	}

	private IMetadataRepository getMDR(String uri) throws Exception {
		URI metadataRepo = getTestData("1.1", uri).toURI();

		IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
		assertNotNull(metadataManager);

		return metadataManager.loadRepository(metadataRepo, new NullProgressMonitor());
	}
}
