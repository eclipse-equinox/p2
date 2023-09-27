/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ql;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.expression.MatchIteratorFilter;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IExpressionParser;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.MatchQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PerformanceTest extends AbstractProvisioningTest {
	public void testParserPerformance() throws Exception {
		IExpressionParser parser = ExpressionUtil.getParser();
		long start = System.currentTimeMillis();
		for (int i = 0; i < 50000; i++)
			parser.parse("providedCapabilities.exists(x | x.name == foo)");
		System.out.println("Parse of 50000 expressions took: " + (System.currentTimeMillis() - start) + " milliseconds");
	}

	public void testMatchQueryVersusExpressionPerformance() throws Exception {

		IMetadataRepository repo = getMDR("/testData/galileoM7");

		IQuery<IInstallableUnit> expressionQuery = QueryUtil.createMatchQuery("id ~= /com.ibm.*/");
		IQuery<IInstallableUnit> matchQuery = new MatchQuery<>() {
			@Override
			public boolean isMatch(IInstallableUnit candidate) {
				return candidate.getId().startsWith("com.ibm.");
			}
		};
		IQueryResult<IInstallableUnit> result;
		long matchQueryMS = 0;
		long exprQueryMS = 0;

		for (int i = 0; i < 5; ++i) {
			long start = System.currentTimeMillis();
			for (int idx = 0; idx < 200; ++idx) {
				result = repo.query(expressionQuery, new NullProgressMonitor());
				assertEquals(queryResultSize(result), 2);
			}
			exprQueryMS += (System.currentTimeMillis() - start);

			start = System.currentTimeMillis();
			for (int idx = 0; idx < 200; ++idx) {
				result = repo.query(matchQuery, new NullProgressMonitor());
				assertEquals(queryResultSize(result), 2);
			}
			matchQueryMS += (System.currentTimeMillis() - start);
		}
		System.out.println("ExpressionQuery took: " + exprQueryMS + " milliseconds");
		System.out.println("MatchQuery took: " + matchQueryMS + " milliseconds");
		System.out.println();
	}

	public void testMatchQueryVersusIndexedExpressionPerformance() throws Exception {

		IMetadataRepository repo = getMDR("/testData/galileoM7");

		IQuery<IInstallableUnit> expressionQuery = QueryUtil.createMatchQuery("id == 'org.eclipse.core.resources'");
		IQuery<IInstallableUnit> matchQuery = new MatchQuery<>() {
			@Override
			public boolean isMatch(IInstallableUnit candidate) {
				return candidate.getId().equals("org.eclipse.core.resources");
			}
		};
		IQueryResult<IInstallableUnit> result;
		long matchQueryMS = 0;
		long exprQueryMS = 0;

		for (int i = 0; i < 5; ++i) {
			long start = System.currentTimeMillis();
			for (int idx = 0; idx < 200; ++idx) {
				result = repo.query(expressionQuery, new NullProgressMonitor());
				assertEquals(queryResultSize(result), 1);
			}
			exprQueryMS += (System.currentTimeMillis() - start);

			start = System.currentTimeMillis();
			for (int idx = 0; idx < 200; ++idx) {
				result = repo.query(matchQuery, new NullProgressMonitor());
				assertEquals(queryResultSize(result), 1);
			}
			matchQueryMS += (System.currentTimeMillis() - start);
		}
		System.out.println("IndexedExpressionQuery took: " + exprQueryMS + " milliseconds");
		System.out.println("MatchQuery took: " + matchQueryMS + " milliseconds");
		System.out.println();
	}

	public void testMatchQueryVersusIndexedExpressionPerformance2() throws Exception {

		IMetadataRepository repo = getMDR("/testData/galileoM7");

		IQuery<IInstallableUnit> expressionQuery = QueryUtil.createMatchQuery("providedCapabilities.exists(x | x.namespace == 'org.eclipse.equinox.p2.iu' && x.name == 'org.eclipse.core.resources')");
		IQuery<IInstallableUnit> matchQuery = new MatchQuery<>() {
			@Override
			public boolean isMatch(IInstallableUnit candidate) {
				for (IProvidedCapability capability : candidate.getProvidedCapabilities())
					if ("org.eclipse.equinox.p2.iu".equals(capability.getNamespace()) && "org.eclipse.core.resources".equals(capability.getName()))
						return true;
				return false;
			}
		};
		IQueryResult<IInstallableUnit> result;
		long matchQueryMS = 0;
		long exprQueryMS = 0;

		for (int i = 0; i < 5; ++i) {
			long start = System.currentTimeMillis();
			for (int idx = 0; idx < 200; ++idx) {
				result = repo.query(expressionQuery, new NullProgressMonitor());
				assertEquals(queryResultSize(result), 1);
			}
			exprQueryMS += (System.currentTimeMillis() - start);

			start = System.currentTimeMillis();
			for (int idx = 0; idx < 200; ++idx) {
				result = repo.query(matchQuery, new NullProgressMonitor());
				assertEquals(queryResultSize(result), 1);
			}
			matchQueryMS += (System.currentTimeMillis() - start);
		}
		System.out.println("IndexedExpressionQuery took: " + exprQueryMS + " milliseconds");
		System.out.println("MatchQuery took: " + matchQueryMS + " milliseconds");
		System.out.println();
	}

	public void testMatchQueryVersusMatchIteratorPerformance() throws Exception {

		IMetadataRepository repo = getMDR("/testData/galileoM7");

		IQuery<IInstallableUnit> matchQuery = new MatchQuery<>() {
			@Override
			public boolean isMatch(IInstallableUnit candidate) {
				return candidate.getId().startsWith("org.eclipse.");
			}
		};

		long matchFilterMS = 0;
		long iterationFilterMS = 0;
		long matchQueryMS = 0;

		for (int i = 0; i < 5; ++i) {
			long start = System.currentTimeMillis();
			for (int idx = 0; idx < 80; ++idx) {
				IQueryResult<IInstallableUnit> everything = repo.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
				Iterator<IInstallableUnit> matchIter = new MatchIteratorFilter<>(everything.iterator()) {
					@Override
					protected boolean isMatch(IInstallableUnit candidate) {
						return candidate.getId().startsWith("org.eclipse.");
					}
				};
				int sz = 0;
				while (matchIter.hasNext()) {
					matchIter.next();
					sz++;
				}
				assertEquals(sz, 3240);
			}
			matchFilterMS += (System.currentTimeMillis() - start);

			start = System.currentTimeMillis();
			for (int idx = 0; idx < 80; ++idx) {
				int sz = 0;
				for (IInstallableUnit candidate : repo.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())) {
					if (candidate.getId().startsWith("org.eclipse."))
						sz++;
				}
				assertEquals(sz, 3240);
			}
			iterationFilterMS += (System.currentTimeMillis() - start);

			start = System.currentTimeMillis();
			for (int idx = 0; idx < 80; ++idx) {
				int sz = 0;
				for (Iterator<IInstallableUnit> iter = repo.query(matchQuery, new NullProgressMonitor()).iterator(); iter.hasNext();) {
					iter.next();
					sz++;
				}
				assertEquals(sz, 3240);
			}
			matchQueryMS += (System.currentTimeMillis() - start);
		}
		System.out.println("MatchFilter took: " + matchFilterMS + " milliseconds");
		System.out.println("IteratorFilter took: " + iterationFilterMS + " milliseconds");
		System.out.println("MatchQuery took: " + matchQueryMS + " milliseconds");
		System.out.println();
	}

	public void testCapabilityQueryPerformance() throws Exception {

		IMetadataRepository repo = getMDR("/testData/galileoM7");

		IRequirement capability = MetadataFactory.createRequirement("org.eclipse.equinox.p2.eclipse.type", "feature", new VersionRange("[1.0.0,2.0.0)"), null, false, false);
		IQuery<IInstallableUnit> predicateQuery = QueryUtil.createMatchQuery("this ~= $0", capability);
		IQuery<IInstallableUnit> capabilityQuery = QueryUtil.createMatchQuery(capability.getMatches());
		IQueryResult<IInstallableUnit> result;
		long tradQueryMS = 0;
		long exprQueryMS = 0;

		for (int i = 0; i < 5; ++i) {
			long start = System.currentTimeMillis();
			for (int idx = 0; idx < 80; ++idx) {
				result = repo.query(capabilityQuery, new NullProgressMonitor());
				assertEquals(queryResultSize(result), 487);
			}
			tradQueryMS += (System.currentTimeMillis() - start);

			start = System.currentTimeMillis();
			for (int idx = 0; idx < 80; ++idx) {
				result = repo.query(predicateQuery, new NullProgressMonitor());
				assertEquals(queryResultSize(result), 487);
			}
			exprQueryMS += (System.currentTimeMillis() - start);
		}
		System.out.println("CapabilityQuery took: " + tradQueryMS + " milliseconds");
		System.out.println("PredicateQuery took: " + exprQueryMS + " milliseconds");
		System.out.println();
	}

	public void testCapabilityQueryPerformanceEE() throws Exception {

		IMetadataRepository repo = getMDR("/testData/galileoM7");

		Version jreVersion = Version.parseVersion("1.8");
		Map<String, Object> capAttrs = new HashMap<>();
		capAttrs.put(JREAction.NAMESPACE_OSGI_EE, "JavaSE");
		capAttrs.put(JREAction.VERSION_OSGI_EE, jreVersion);
		IProvidedCapability jreCap = MetadataFactory.createProvidedCapability(JREAction.NAMESPACE_OSGI_EE, capAttrs);
		IInstallableUnit jre8IU = createIU("a.jre.javase", jreVersion, new IProvidedCapability[] {jreCap});
		repo.addInstallableUnits(Collections.singletonList(jre8IU));

		jreVersion = Version.parseVersion("1.9");
		capAttrs = new HashMap<>();
		capAttrs.put(JREAction.NAMESPACE_OSGI_EE, "JavaSE");
		capAttrs.put(JREAction.VERSION_OSGI_EE, jreVersion);
		jreCap = MetadataFactory.createProvidedCapability(JREAction.NAMESPACE_OSGI_EE, capAttrs);
		IInstallableUnit jre9IU = createIU("b.jre.anotherjavase", jreVersion, new IProvidedCapability[] {jreCap});
		repo.addInstallableUnits(Collections.singletonList(jre9IU));

		IRequirement capability = MetadataFactory.createRequirement(JREAction.NAMESPACE_OSGI_EE, "(&(osgi.ee=JavaSE)(version=1.8))", null, 0, 0, false);
		IQuery<IInstallableUnit> capabilityQuery = QueryUtil.createMatchQuery(capability.getMatches());
		IQueryResult<IInstallableUnit> result;
		long tradQueryMS = 0;

		for (int i = 0; i < 5; ++i) {
			long start = System.currentTimeMillis();
			for (int idx = 0; idx < 80; ++idx) {
				result = repo.query(capabilityQuery, new NullProgressMonitor());
				assertEquals(1, queryResultSize(result));
				assertEquals(jre8IU, result.toUnmodifiableSet().iterator().next());
			}
			tradQueryMS += (System.currentTimeMillis() - start);
		}
		System.out.println("CapabilityQuery took: " + tradQueryMS + " milliseconds");
		System.out.println();
	}

	public void testCapabilityQueryPerformanceOsgiService() throws Exception {

		IMetadataRepository repo = getMDR("/testData/2018-12");

		IRequirement capability = MetadataFactory.createRequirement("osgi.service", "(objectClass=org.osgi.service.event.EventAdmin)", null, 0, 0, false);
		IQuery<IInstallableUnit> capabilityQuery = QueryUtil.createMatchQuery(capability.getMatches());
		IQueryResult<IInstallableUnit> result;

		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000; ++i) {
			result = repo.query(capabilityQuery, new NullProgressMonitor());
			assertEquals(1, queryResultSize(result));
			assertEquals("org.eclipse.equinox.event", result.iterator().next().getId());
		}
		System.out.println("1000 * CapabilityQuery for osgi.service took: " + (System.currentTimeMillis() - start) + " milliseconds");
		System.out.println();
	}

	public void testIUPropertyQueryPerformance() throws Exception {

		IMetadataRepository repo = getMDR("/testData/galileoM7");

		IQuery<IInstallableUnit> propertyQuery = QueryUtil.createIUPropertyQuery("df_LT.providerName", "Eclipse.org");
		IQuery<IInstallableUnit> predicateQuery = QueryUtil.createMatchQuery("properties[$0] == $1", "df_LT.providerName", "Eclipse.org");
		IQueryResult<IInstallableUnit> result;
		long tradQueryMS = 0;
		long exprQueryMS = 0;

		for (int i = 0; i < 5; ++i) {
			long start = System.currentTimeMillis();
			for (int idx = 0; idx < 80; ++idx) {
				result = repo.query(propertyQuery, new NullProgressMonitor());
				assertEquals(queryResultSize(result), 965);
			}
			tradQueryMS += (System.currentTimeMillis() - start);

			start = System.currentTimeMillis();
			for (int idx = 0; idx < 80; ++idx) {
				result = repo.query(predicateQuery, new NullProgressMonitor());
				assertEquals(queryResultSize(result), 965);
			}
			exprQueryMS += (System.currentTimeMillis() - start);
		}
		System.out.println("IUPropertyQuery took: " + tradQueryMS + " milliseconds");
		System.out.println("PredicateQuery took: " + exprQueryMS + " milliseconds");
		System.out.println();
	}

	public void testSlicerPerformance() throws Exception {
		HashMap<String, String> env = new HashMap<>();
		env.put("osgi.os", "linux");
		env.put("osgi.ws", "gtk");
		env.put("osgi.arch", "x86");
		IInstallableUnit envIU = InstallableUnit.contextIU(env);

		IMetadataRepository repo = getMDR("/testData/galileoM7");
		IQueryResult<IInstallableUnit> r = repo.query(QueryUtil.createIUQuery("org.eclipse.sdk.feature.group", Version.create("3.5.0.v20090423-7Q7bA7DPR-wM38__Q4iRsmx9z0KOjbpx3AbyvXd-Uq7J2")), new NullProgressMonitor());
		Iterator<IInstallableUnit> itor = r.iterator();
		assertTrue(itor.hasNext());
		IInstallableUnit[] roots = new IInstallableUnit[] {itor.next()};

		IQuery<IInstallableUnit> query = QueryUtil.createQuery( //
				"$0.traverse(set(), _, { cache, parent | parent.requirements.unique(cache).select(rc | rc.filter == null || $1 ~= rc.filter).collect(rc | everything.select(iu | iu ~= rc)).flatten()})", roots, envIU);

		long sliceTime = 0;
		long traverseTime = 0;
		IQueryable<IInstallableUnit> slice = null;
		for (int idx = 0; idx < 100; ++idx) {
			long startTime = System.currentTimeMillis();
			r = repo.query(query, new NullProgressMonitor());
			traverseTime += (System.currentTimeMillis() - startTime);
			assertEquals(queryResultSize(r), 411);

			startTime = System.currentTimeMillis();
			Slicer slicer = new Slicer(new QueryableArray(gatherAvailableInstallableUnits(repo)), env, false);
			slice = slicer.slice(r.toUnmodifiableSet(), new NullProgressMonitor());
			sliceTime += (System.currentTimeMillis() - startTime);
		}
		// Check the size of the last slice to verify that it's the same as the traverse size
		r = slice.query(new MatchQuery<IInstallableUnit>() {
			@Override
			public boolean isMatch(IInstallableUnit value) {
				return true;
			}
		}, new NullProgressMonitor());
		assertEquals(queryResultSize(r), 411);

		System.out.print("100 * Slicing took: ");
		System.out.println(sliceTime);
		System.out.print("100 * Indexed Traverse expression took: ");
		System.out.println(traverseTime);
		System.out.println();
	}

	public void testPermissiveSlicerPerformance() throws Exception {
		HashMap<String, String> env = new HashMap<>();
		//env.put("osgi.os", "linux");
		//env.put("osgi.ws", "gtk");
		//env.put("osgi.arch", "x86");
		IInstallableUnit envIU = InstallableUnit.contextIU(env);

		CompositeMetadataRepository compositeMetadataRepository = CompositeMetadataRepository.createMemoryComposite(getMetadataRepositoryManager().getAgent());
		compositeMetadataRepository.addChild(new URI("https://download.eclipse.org/releases/galileo"));
		IMetadataRepository repo = compositeMetadataRepository;
		IQueryResult<IInstallableUnit> r = repo.query(QueryUtil.createIUQuery("org.eclipse.sdk.ide"), new NullProgressMonitor());
		IInstallableUnit[] roots = r.toArray(IInstallableUnit.class);

		IQuery<IInstallableUnit> query = QueryUtil.createQuery( //
				"$0.traverse(set(), _, { cache, parent | parent.requirements.unique(cache).collect(rc | everything.select(iu | iu ~= rc)).flatten()})", roots, envIU);

		long sliceTime = 0;
		long traverseTime = 0;
		IQueryable<IInstallableUnit> slice = null;
		for (int idx = 0; idx < 10; ++idx) {
			long startTime = System.currentTimeMillis();
			r = repo.query(query, new NullProgressMonitor());
			traverseTime += (System.currentTimeMillis() - startTime);
			assertEquals(4704, queryResultSize(r));

			startTime = System.currentTimeMillis();
			Slicer slicer = new PermissiveSlicer(repo, env, true, true, true, false, false);
			slice = slicer.slice(r.toUnmodifiableSet(), new NullProgressMonitor());
			sliceTime += (System.currentTimeMillis() - startTime);
		}
		// Check the size of the last slice to verify that it's the same as the traverse size
		r = slice.query(new MatchQuery<IInstallableUnit>() {
			@Override
			public boolean isMatch(IInstallableUnit value) {
				return true;
			}
		}, new NullProgressMonitor());
		assertEquals(queryResultSize(r), 4704);

		System.out.print("10 * Slicing took: ");
		System.out.println(sliceTime);
		System.out.print("10 * Indexed Traverse expression took: ");
		System.out.println(traverseTime);
		System.out.println();
	}

	private IMetadataRepository getMDR(String uri) throws Exception {
		URI metadataRepo = getTestData("1.1", uri).toURI();

		IMetadataRepositoryManager metadataManager = getAgent().getService(IMetadataRepositoryManager.class);
		assertNotNull(metadataManager);

		return metadataManager.loadRepository(metadataRepo, new NullProgressMonitor());
	}

	private Set<IInstallableUnit> gatherAvailableInstallableUnits(IQueryable<IInstallableUnit> queryable) {
		return queryable.query(QueryUtil.createIUAnyQuery(), null).toUnmodifiableSet();
	}
}
