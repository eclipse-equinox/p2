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
import java.util.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.p2.director.app.Activator;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.ql.ExpressionQuery;
import org.eclipse.equinox.p2.ql.PredicateQuery;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.ql.TestQueryReimplementation.CapabilityQuery;

public class PerformanceTest extends AbstractProvisioningTest {
	public void testCapabilityQueryPerformance() throws Exception {

		IMetadataRepository repo = getMDR("/testData/galileoM7");

		IRequiredCapability capability = MetadataFactory.createRequiredCapability("org.eclipse.equinox.p2.eclipse.type", "feature", new VersionRange("[1.0.0,2.0.0)"), null, false, false);
		PredicateQuery predicateQuery = new PredicateQuery("item ~= $0", capability);
		Collector result;
		long tradQueryMS = 0;
		long exprQueryMS = 0;

		for (int i = 0; i < 5; ++i) {
			long start = System.currentTimeMillis();
			for (int idx = 0; idx < 80; ++idx) {
				result = repo.query(capability, new NullProgressMonitor());
				assertEquals(result.size(), 487);
			}
			tradQueryMS += (System.currentTimeMillis() - start);

			start = System.currentTimeMillis();
			for (int idx = 0; idx < 80; ++idx) {
				result = repo.query(predicateQuery, new NullProgressMonitor());
				assertEquals(result.size(), 487);
			}
			exprQueryMS += (System.currentTimeMillis() - start);
		}
		System.out.println("CapabilityQuery took: " + tradQueryMS + " milliseconds");
		System.out.println("PredicateQuery took: " + exprQueryMS + " milliseconds");
		System.out.println();
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

		for (int i = 0; i < 5; ++i) {
			long start = System.currentTimeMillis();
			for (int idx = 0; idx < 80; ++idx) {
				result = repo.query(capabilityQuery, new NullProgressMonitor());
				assertEquals(result.size(), 446);
			}
			tradQueryMS += (System.currentTimeMillis() - start);

			start = System.currentTimeMillis();
			for (int idx = 0; idx < 80; ++idx) {
				result = repo.query(predicateQuery, new NullProgressMonitor());
				assertEquals(result.size(), 446);
			}
			exprQueryMS += (System.currentTimeMillis() - start);
		}
		System.out.println("CapabilityQuery took: " + tradQueryMS + " milliseconds");
		System.out.println("PredicateQuery took: " + exprQueryMS + " milliseconds");
		System.out.println();
	}

	public void testIUPropertyQueryPerformance() throws Exception {

		IMetadataRepository repo = getMDR("/testData/galileoM7");

		IUPropertyQuery propertyQuery = new IUPropertyQuery("df_LT.providerName", "Eclipse.org");
		PredicateQuery predicateQuery = new PredicateQuery("properties[$0] == $1", "df_LT.providerName", "Eclipse.org");
		Collector result;
		long tradQueryMS = 0;
		long exprQueryMS = 0;

		for (int i = 0; i < 5; ++i) {
			long start = System.currentTimeMillis();
			for (int idx = 0; idx < 80; ++idx) {
				result = repo.query(propertyQuery, new NullProgressMonitor());
				assertEquals(result.size(), 965);
			}
			tradQueryMS += (System.currentTimeMillis() - start);

			start = System.currentTimeMillis();
			for (int idx = 0; idx < 80; ++idx) {
				result = repo.query(predicateQuery, new NullProgressMonitor());
				assertEquals(result.size(), 965);
			}
			exprQueryMS += (System.currentTimeMillis() - start);
		}
		System.out.println("IUPropertyQuery took: " + tradQueryMS + " milliseconds");
		System.out.println("PredicateQuery took: " + exprQueryMS + " milliseconds");
		System.out.println();
	}

	public void testSlicerPerformance() throws Exception {
		Hashtable env = new Hashtable();
		env.put("osgi.os", "linux");
		env.put("osgi.ws", "gtk");
		env.put("osgi.arch", "x86");

		IMetadataRepository repo = getMDR("/testData/galileoM7");
		Collector c = repo.query(new InstallableUnitQuery("org.eclipse.sdk.feature.group", Version.create("3.5.0.v20090423-7Q7bA7DPR-wM38__Q4iRsmx9z0KOjbpx3AbyvXd-Uq7J2")), new NullProgressMonitor());
		Iterator itor = c.iterator();
		assertTrue(itor.hasNext());
		IInstallableUnit[] roots = new IInstallableUnit[] {(IInstallableUnit) itor.next()};

		IQuery query = new ExpressionQuery("" + //
				"$0.traverse(capabilityIndex(everything), _, {index, parent | " + //
				"index.satisfiesAny(parent.requiredCapabilities.select(rc | rc.filter == null || $1 ~= filter(rc.filter)))})", roots, env);

		long sliceTime = 0;
		long traverseTime = 0;
		IQueryable slice = null;
		for (int idx = 0; idx < 20; ++idx) {
			long startTime = System.currentTimeMillis();
			c = repo.query(query, new NullProgressMonitor());
			traverseTime += (System.currentTimeMillis() - startTime);
			assertEquals(c.size(), 411);

			startTime = System.currentTimeMillis();
			Slicer slicer = new Slicer(new QueryableArray(gatherAvailableInstallableUnits(repo)), env, false);
			slice = slicer.slice(roots, new NullProgressMonitor());
			c = slice.query(new MatchQuery() {
				public boolean isMatch(Object value) {
					return true;
				}
			}, new NullProgressMonitor());
			sliceTime += (System.currentTimeMillis() - startTime);
			assertEquals(c.size(), 411);
		}
		System.out.print("20 * Slicing took: ");
		System.out.println(sliceTime);
		System.out.print("20 * Indexed Traverse expression took: ");
		System.out.println(traverseTime);
		System.out.println();
	}

	private IMetadataRepository getMDR(String uri) throws Exception {
		URI metadataRepo = getTestData("1.1", uri).toURI();

		IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
		assertNotNull(metadataManager);

		return metadataManager.loadRepository(metadataRepo, new NullProgressMonitor());
	}

	private IInstallableUnit[] gatherAvailableInstallableUnits(IQueryable queryable) {
		ArrayList list = new ArrayList();
		Collector matches = queryable.query(InstallableUnitQuery.ANY, null);
		for (Iterator it = matches.iterator(); it.hasNext();)
			list.add(it.next());
		return (IInstallableUnit[]) list.toArray(new IInstallableUnit[list.size()]);
	}
}
