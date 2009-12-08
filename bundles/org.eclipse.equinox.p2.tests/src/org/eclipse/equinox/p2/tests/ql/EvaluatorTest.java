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

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.director.app.Activator;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.ql.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.ql.ExpressionQuery;
import org.eclipse.equinox.p2.ql.PredicateQuery;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.Filter;

public class EvaluatorTest extends AbstractProvisioningTest {
	private static final ExpressionParser parser = new ExpressionParser();
	private static final VariableScope dummyScope = VariableScope.ROOT;

	public void testArguments() throws Exception {
		Expression expr = parser.parsePredicate("'a' == $0 && 'b' == $1 && 'c' == $2");
		assertEquals(Boolean.TRUE, expr.evaluate(new ExpressionContext(null, new Object[] {"a", "b", "c"}, null), dummyScope));
	}

	public void testAnonymousMember() throws Exception {
		Expression expr = parser.parsePredicate("$0.class == $1");
		assertEquals(Boolean.TRUE, expr.evaluate(new ExpressionContext(null, new Object[] {"a", String.class}, null), dummyScope));
	}

	public void testInstanceOf() throws Exception {
		// Explicit instanceof when rhs is a class
		Expression expr = parser.parsePredicate("$0 ~= $1");
		assertEquals(Boolean.TRUE, expr.evaluate(new ExpressionContext(null, new Object[] {new Integer(4), Number.class}, null), dummyScope));
	}

	public void testArray() throws Exception {
		ExpressionContext ctx = new ExpressionContext(null, null, null);
		Expression expr = parser.parsePredicate("['a', 'b', 'c'].exists(x | x == 'b') && ['a', 'b', 'c'].all(x | 'd' > x)");
		assertEquals(Boolean.TRUE, expr.evaluate(ctx, dummyScope));
		expr = parser.parsePredicate("['d', 'e', 'f'].exists(x | ['a', 'b', 'c'].exists(y | x > y))");
		assertEquals(Boolean.TRUE, expr.evaluate(ctx, dummyScope));
		expr = parser.parsePredicate("[['d', 'e', 'f'], ['h', 'i', 'j']].exists(x | x.all(y | ['a', 'b', 'c'].all(z | y > z)))");
		assertEquals(Boolean.TRUE, expr.evaluate(ctx, dummyScope));
		expr = parser.parsePredicate("[['d', 'e', 'f'], ['h', '3', 'j']].exists(x | x.all(y | ['a', 'b', 'c'].exists(z | y > z)))");
		assertEquals(Boolean.TRUE, expr.evaluate(ctx, dummyScope));
		expr = parser.parsePredicate("[['d', 'e', 'f'], ['h', 'i', 'j']].all(x | x.all(y | ['a', 'b', 'c'].all(z | y > z)))");
		assertEquals(Boolean.TRUE, expr.evaluate(ctx, dummyScope));
		expr = parser.parsePredicate("[['d', 'e', 'f'], ['h', '3', 'j']].all(x | x.all(y | ['a', 'b', 'c'].all(z | y > z)))");
		assertEquals(Boolean.FALSE, expr.evaluate(ctx, dummyScope)); // 3 < 'b'
	}

	public void testLatest() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/multipleversions1");
		Collector result = repo.query(new ExpressionQuery("latest(x | x.id == $0)", "test.bundle"), new NullProgressMonitor());
		assertTrue(result.size() == 1);
	}

	public void testRange() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/multipleversions1");
		Collector result = repo.query(new PredicateQuery("version ~= $0", new VersionRange("2.0.0")), new NullProgressMonitor());
		assertEquals(result.size(), 2);
	}

	public void testProperty() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/multipleversions1");

		Collector result = repo.query(new PredicateQuery("properties.exists(p | p.value == $0)", "true"), new NullProgressMonitor());
		assertEquals(result.size(), 3);

		result = repo.query(new PredicateQuery("properties['org.eclipse.equinox.p2.type.group'] == $0", "true"), new NullProgressMonitor());
		assertEquals(result.size(), 3);

		Filter filter = TestActivator.context.createFilter("(org.eclipse.equinox.p2.type.group=true)");
		result = repo.query(new PredicateQuery("properties ~= $0", filter), new NullProgressMonitor());
		assertEquals(result.size(), 3);
	}

	public void testToString() throws Exception {
		String exprString = "select(x | x.id == $0 && (x.version == $1 || x.version == $2)).traverse(set(), _, {requirementsCache, parent | select(" + //
				"parent.requiredCapabilities.unique(requirementsCache).select(rc | rc.filter == null || $2 ~= filter(rc.filter)), _, " + //
				"{rcs, child | rcs.exists(rc | child ~= rc)})}).limit(10)";

		ContextExpression expr = parser.parseQuery(exprString);
		System.out.println(expr.toString());
		assertEquals(exprString, expr.toString());
	}

	public void testSomeAPI() throws Exception {
		// Create some expressions. Note the use of identifiers instead of
		// indexes for the parameters

		Variable item = Variable.createEach("item");
		Expression cmp1 = new Equals(Member.createDynamicMember(item, "id"), new KeyedParameter("id"));
		Expression cmp2 = new Equals(new At(Member.createDynamicMember(item, "properties"), new KeyedParameter("propKey")), new KeyedParameter("propValue"));

		Variable everything = Variable.EVERYTHING;
		LambdaExpression lambda = new LambdaExpression(new And(new Expression[] {cmp1, cmp2}), item);
		Expression latest = new Latest(new Select(everything, lambda));
		ContextExpression e3 = new ContextExpression(everything, latest);

		// Put the parameters in a map
		Map args = new HashMap();
		args.put("id", "test.bundle");
		args.put("propKey", "org.eclipse.equinox.p2.type.group");
		args.put("propValue", "true");

		// Create the query
		IMetadataRepository repo = getMDR("/testData/metadataRepo/multipleversions1");
		Collector result = repo.query(new ExpressionQuery(e3, args), new NullProgressMonitor());
		assertEquals(result.size(), 1);
	}

	public void testMember() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		IProvidedCapability pc = MetadataFactory.createProvidedCapability("org.eclipse.equinox.p2.eclipse.type", "source", null);
		Collector result = repo.query(new PredicateQuery("fragment && host.exists(h | $0 ~= h)", pc), new NullProgressMonitor());
		assertEquals(result.size(), 1);
	}

	public void testPatch() throws Exception {
		IRequiredCapability[][] applicability = new IRequiredCapability[2][2];
		applicability[0][0] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "javax.wsdl", null, null, false, false);
		applicability[0][1] = MetadataFactory.createRequiredCapability("org.eclipse.equinox.p2.eclipse.type", "bundle", null, null, false, false);
		applicability[1][0] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "tooling.source.default", null, null, false, false);
		applicability[1][1] = MetadataFactory.createRequiredCapability("org.eclipse.equinox.p2.flavor", "tooling", null, null, false, false);

		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		Collector result = repo.query(new PredicateQuery("$0.exists(rcs | rcs.all(rc | item ~= rc))", applicability), new NullProgressMonitor());
		assertEquals(result.size(), 3);
	}

	public void testPattern() throws Exception {
		IProvidedCapability pc = MetadataFactory.createProvidedCapability("org.eclipse.equinox.p2.eclipse.type", "source", null);
		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		Collector result = repo.query(new PredicateQuery("id ~= /tooling.*.default/", pc), new NullProgressMonitor());
		assertEquals(result.size(), 3);
	}

	public void testLimit() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		Collector result = repo.query(new ExpressionQuery("select(x | x.id ~= /tooling.*/).limit(1)"), new NullProgressMonitor());
		assertEquals(result.size(), 1);

		result = repo.query(new ExpressionQuery("select(x | x.id ~= /tooling.*/).limit($0)", new Integer(2)), new NullProgressMonitor());
		assertEquals(result.size(), 2);
	}

	public void testNot() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		Collector result = repo.query(new PredicateQuery("!(id ~= /tooling.*/)"), new NullProgressMonitor());
		assertEquals(result.size(), 4);
	}

	public void testArtifactQuery() throws Exception {
		URI artifactRepo = getTestData("1.1", "/testData/artifactRepo/simple").toURI();

		IArtifactRepositoryManager artifactManager = getArtifactRepositoryManager();
		assertNotNull(artifactManager);

		IArtifactRepository repo = artifactManager.loadRepository(artifactRepo, new NullProgressMonitor());
		Collector result = repo.query(new PredicateQuery(IArtifactKey.class, "classifier ~= /*/"), new NullProgressMonitor());
		assertTrue(result.size() > 1);
		Iterator itor = result.iterator();
		while (itor.hasNext())
			assertTrue(itor.next() instanceof IArtifactKey);

		result = repo.query(new PredicateQuery(IArtifactDescriptor.class, "artifactKey.classifier ~= /*/"), new NullProgressMonitor());
		assertTrue(result.size() > 1);
		itor = result.iterator();
		while (itor.hasNext())
			assertTrue(itor.next() instanceof IArtifactDescriptor);
	}

	public void testClassConstructor() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		Collector result = repo.query(new ExpressionQuery(//
				"select(x | x ~= class('org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnitFragment'))"), new NullProgressMonitor());
		assertEquals(result.size(), 4);
		repo = getMDR("/testData/galileoM7");
	}

	public void testTraverse() throws Exception {
		IMetadataRepository repo = getMDR("/testData/galileoM7");
		Collector result = repo.query(new ExpressionQuery( //
				"select(x | x.id == $0 && x.version == $1).traverse(parent | select(" + //
						"child | parent.requiredCapabilities.exists(rc | child ~= rc)))", //
				"org.eclipse.sdk.feature.group", Version.create("3.5.0.v20090423-7Q7bA7DPR-wM38__Q4iRsmx9z0KOjbpx3AbyvXd-Uq7J2")), new NullProgressMonitor());
		assertEquals(result.size(), 463);
	}

	public void testTraverseWithFilteredRequirements() throws Exception {
		// Add some filtering of requirements
		ContextExpression expr = parser.parseQuery(//
				"select(x | x.id == $0 && x.version == $1).traverse(parent | select(" + //
						"child | parent.requiredCapabilities.exists(rc | (rc.filter == null || $2 ~= filter(rc.filter)) && child ~= rc)))");

		Map env = new Hashtable();
		env.put("osgi.os", "linux");
		env.put("osgi.ws", "gtk");
		env.put("osgi.arch", "x86");
		ExpressionQuery query = new ExpressionQuery(IInstallableUnit.class, expr, new Object[] {"org.eclipse.sdk.feature.group", Version.create("3.5.0.v20090423-7Q7bA7DPR-wM38__Q4iRsmx9z0KOjbpx3AbyvXd-Uq7J2"), env});

		IMetadataRepository repo = getMDR("/testData/galileoM7");
		Collector result = repo.query(query, new NullProgressMonitor());
		assertEquals(result.size(), 411);
	}

	public void testTraverseWithCurrying() throws Exception {
		// Use currying to prevent that the filtering of requirements is done more then once
		ContextExpression expr = parser.parseQuery(//
				"select(x | x.id == $0 && x.version == $1).traverse({parent | select(" + //
						"parent.requiredCapabilities.select(rc | rc.filter == null || $2 ~= filter(rc.filter)), _," + //
						"{rcs, child | rcs.exists(rc | child ~= rc)})})");

		Map env = new Hashtable();
		env.put("osgi.os", "linux");
		env.put("osgi.ws", "gtk");
		env.put("osgi.arch", "x86");
		ExpressionQuery query = new ExpressionQuery(IInstallableUnit.class, expr, new Object[] {"org.eclipse.sdk.feature.group", Version.create("3.5.0.v20090423-7Q7bA7DPR-wM38__Q4iRsmx9z0KOjbpx3AbyvXd-Uq7J2"), env});

		IMetadataRepository repo = getMDR("/testData/galileoM7");
		Collector result = repo.query(query, new NullProgressMonitor());
		assertEquals(result.size(), 411);
	}

	public void testTraverseWithCurryingAndCache() throws Exception {
		// Add some filtering of requirements
		ContextExpression expr = parser.parseQuery(//
				"select(x | x.id == $0 && x.version == $1).traverse(set(), _, {requirementsCache, parent | select(" + //
						"parent.requiredCapabilities.unique(requirementsCache).select(rc | rc.filter == null || $2 ~= filter(rc.filter)), _," + //
						"{rcs, child | rcs.exists(rc | child ~= rc)})})");

		Map env = new Hashtable();
		env.put("osgi.os", "linux");
		env.put("osgi.ws", "gtk");
		env.put("osgi.arch", "x86");

		ExpressionQuery query = new ExpressionQuery(IInstallableUnit.class, expr, new Object[] {"org.eclipse.sdk.feature.group", Version.create("3.5.0.v20090423-7Q7bA7DPR-wM38__Q4iRsmx9z0KOjbpx3AbyvXd-Uq7J2"), env});

		IMetadataRepository repo = getMDR("/testData/galileoM7");

		long startTime = System.currentTimeMillis();
		Collector result = repo.query(query, new NullProgressMonitor());
		System.out.print("testTraverseWithCurryingAndCache: ");
		System.out.println(System.currentTimeMillis() - startTime);
		assertEquals(result.size(), 411);
	}

	public void testCommonRequirements() throws Exception {
		// Add some filtering of requirements
		ContextExpression expr = parser.parseQuery(//
				"" + //
						"select(x | x.id == $0 && x.version == $1).traverse(set(), _, {requirementsCache, parent | select(" + //
						"parent.requiredCapabilities.unique(requirementsCache).select(rc | rc.filter == null || $4 ~= filter(rc.filter)), _," + //
						"{rcs, child | rcs.exists(rc | child ~= rc)})}) && " + //
						"select(x | x.id == $2 && x.version == $3).traverse(set(), _, {requirementsCache, parent | select(" + //
						"parent.requiredCapabilities.unique(requirementsCache).select(rc | rc.filter == null || $4 ~= filter(rc.filter)), _," + //
						"{rcs, child | rcs.exists(rc | child ~= rc)})})");

		Map env = new Hashtable();
		env.put("osgi.os", "linux");
		env.put("osgi.ws", "gtk");
		env.put("osgi.arch", "x86");

		ExpressionQuery query = new ExpressionQuery(IInstallableUnit.class, expr, new Object[] { //
				"org.eclipse.pde.feature.group", //
						Version.create("3.5.0.v20090123-7Z7YF8NFE-z0VXhWU26Hu8gY"), //
						"org.eclipse.gmf.feature.group", //
						Version.create("1.1.1.v20090114-0940-7d8B0FXwkKwFanGNHeHHq8ymBgZ"), //
						env});

		IMetadataRepository repo = getMDR("/testData/galileoM7");
		Collector result = repo.query(query, new NullProgressMonitor());
		assertEquals(result.size(), 184);
	}

	public void testMatchQueryInjectionInPredicate() throws Exception {
		IMetadataRepository repo = getMDR("/testData/galileoM7");
		ItemExpression expr = parser.parsePredicate("iquery($0) || iquery($1)");
		MatchQuery q1 = new MatchQuery() {
			@Override
			public boolean isMatch(Object candidate) {
				return "true".equals(((IInstallableUnit) candidate).getProperty("org.eclipse.equinox.p2.type.category"));
			}
		};
		MatchQuery q2 = new MatchQuery() {
			@Override
			public boolean isMatch(Object candidate) {
				return "true".equals(((IInstallableUnit) candidate).getProperty("org.eclipse.equinox.p2.type.group"));
			}
		};
		Collector result = repo.query(new PredicateQuery(expr, new Object[] {q1, q2}), new NullProgressMonitor());
		assertEquals(result.size(), 497);
	}

	public void testMatchQueryInjectionInContext() throws Exception {
		IMetadataRepository repo = getMDR("/testData/galileoM7");
		ContextExpression expr = parser.parseQuery("select(x | iquery($0, x) || iquery($1, x)).latest()");
		MatchQuery q1 = new MatchQuery() {
			@Override
			public boolean isMatch(Object candidate) {
				return "true".equals(((IInstallableUnit) candidate).getProperty("org.eclipse.equinox.p2.type.category"));
			}
		};
		MatchQuery q2 = new MatchQuery() {
			@Override
			public boolean isMatch(Object candidate) {
				return "true".equals(((IInstallableUnit) candidate).getProperty("org.eclipse.equinox.p2.type.group"));
			}
		};
		Collector result = repo.query(new ExpressionQuery(expr, new Object[] {q1, q2}), new NullProgressMonitor());
		assertEquals(result.size(), 497);
	}

	public void testTranslationFragment() {
		File foo_fragment = new File(TestActivator.getTestDataFolder(), "FragmentPublisherTest/foo.fragment");//$NON-NLS-1$
		File foo = new File(TestActivator.getTestDataFolder(), "FragmentPublisherTest/foo");//$NON-NLS-1$
		BundlesAction bundlesAction = new BundlesAction(new File[] {foo_fragment});
		PublisherInfo info = new PublisherInfo();
		PublisherResult results = new PublisherResult();

		bundlesAction.perform(info, results, new NullProgressMonitor());
		Collection ius = results.getIUs(null, null);
		assertEquals("1.0", 1, ius.size());

		info = new PublisherInfo();
		results = new PublisherResult();
		bundlesAction = new BundlesAction(new File[] {foo});
		bundlesAction.perform(info, results, new NullProgressMonitor());

		bundlesAction = new BundlesAction(new File[] {foo_fragment});
		bundlesAction.perform(info, results, new NullProgressMonitor());
		ius = results.getIUs(null, null);
		assertEquals("2.0", 3, ius.size());
		QueryableArray queryableArray = new QueryableArray((IInstallableUnit[]) ius.toArray(new IInstallableUnit[ius.size()]));
		Collector result = queryableArray.query(new InstallableUnitQuery("foo"), null);
		assertEquals("2.1", 1, result.size());
		IInstallableUnit iu = (IInstallableUnit) result.iterator().next();

		ContextExpression localePropertyQuery = parser.parseQuery("" + //
				"[[select(f | f ~= class('org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnitFragment') && f.host.exists(h | $0 ~= h) && f.providedCapabilities.exists(pc | pc.namespace == 'org.eclipse.equinox.p2.localization' && pc.name ~= $2))" + //
				".collect(f | localizedKeys($2,$1).collect(lk | f.properties[lk])).flatten(), localizedKeys($2,$1).collect(lk | $0.properties[lk])].flatten().first(v | v != null)]");//

		IQuery lq = new ExpressionQuery(localePropertyQuery, new Object[] {iu, "foo", Locale.getDefault()});
		Collector c = queryableArray.query(lq, null);
		Object[] pqr = c.toArray(Object.class);
		assertTrue(pqr.length == 1);
		assertEquals("3.2", "English Foo", pqr[0]);

		ContextExpression cacheQuery = parser.parseQuery("select(f | f ~= class('org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnitFragment') && f.host.exists(h | $0 ~= h) && f.providedCapabilities.exists(pc | pc.namespace == 'org.eclipse.equinox.p2.localization' && pc.name ~= $1))");
		IQuery fragsQuery = new ExpressionQuery(cacheQuery, new Object[] {iu, Locale.GERMAN});
		Collector frags = queryableArray.query(fragsQuery, null);
		assertEquals(frags.size(), 1);

		localePropertyQuery = parser.parseQuery("[$0].collect(localizedKeys($2,$1), _, { lks, iu | " + //
				"[$3.collect(f | lks.collect(lk | f.properties[lk])).flatten(), lks.collect(lk | iu.properties[lk])].flatten().first(v | v != null)})");//

		lq = new ExpressionQuery(localePropertyQuery, new Object[] {iu, "foo", Locale.GERMAN, frags});
		c = queryableArray.query(lq, null);
		pqr = c.toArray(String.class);
		assertTrue(pqr.length == 1);
		assertEquals("2.2", "German Foo", pqr[0]);

		lq = new ExpressionQuery("localizedMap($0, $1).select(e | localizedKeys($0, $2).exists(k | k == e.key)).collect(k | k.value)", Locale.GERMAN, iu, "foo");
		c = queryableArray.query(lq, null);
		pqr = c.toArray(String.class);
		assertTrue(pqr.length == 2);
		assertEquals("2.3", "German Foo", pqr[0]);
		assertEquals("2.4", "English Foo", pqr[1]); // Default

		lq = new ExpressionQuery("localizedMap($0, $1).select(e | localizedKeys($0, $2).exists(k | k == e.key)).collect(k | k.value).limit(1)", Locale.GERMAN, iu, "foo");
		c = queryableArray.query(lq, null);
		pqr = c.toArray(String.class);
		assertTrue(pqr.length == 1);
		assertEquals("2.5", "German Foo", pqr[0]);

		lq = new ExpressionQuery("[localizedProperty($0, $1, $2)]", Locale.GERMAN, iu, "foo");
		c = queryableArray.query(lq, null);
		pqr = c.toArray(String.class);
		assertTrue(pqr.length == 1);
		assertEquals("2.6", "German Foo", pqr[0]);

		lq = new ExpressionQuery("select(x | localizedProperty($0, x, 'foo') ~= /German*/)", Locale.GERMAN);
		c = queryableArray.query(lq, null);
		pqr = c.toArray(IInstallableUnit.class);
		assertTrue(pqr.length == 1);
		assertEquals("2.7", "foo", ((IInstallableUnit) pqr[0]).getId());
	}

	private IMetadataRepository getMDR(String uri) throws Exception {
		URI metadataRepo = getTestData("1.1", uri).toURI();

		IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
		assertNotNull(metadataManager);

		return metadataManager.loadRepository(metadataRepo, new NullProgressMonitor());
	}
}
