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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.KeyWithLocale;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IContextExpression;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.expression.IExpressionFactory;
import org.eclipse.equinox.p2.metadata.expression.IExpressionParser;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.MatchQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.Filter;

public class EvaluatorTest extends AbstractProvisioningTest {
	private static final IExpressionParser parser = ExpressionUtil.getParser();
	private static final IExpressionFactory factory = ExpressionUtil.getFactory();

	public void testQueryTouchpointSetJvm() throws Exception {
		IMetadataRepository repo = getMDR("/testData/bug571836");
		{
			IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createMatchQuery(//
					"touchpointType != null && touchpointType.id == 'org.eclipse.equinox.p2.osgi' && touchpointData.exists(t | t.instructions.exists(entry | entry.value.body ~= /*org.eclipse.equinox.p2.touchpoint.eclipse.setJvm*/))"),
					new NullProgressMonitor());
			assertEquals("Found wrong number of touchpoints with SetJVM action", 1, queryResultSize(result));
			Iterator<?> iterator = result.iterator();
			while (iterator.hasNext()) {
				Object r = iterator.next();
				System.out.println(r.getClass() + " : " + r);
			}
		}
		{
			IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createQuery(//
					"everything.select(x | x.touchpointType != null && x.touchpointType.id == 'org.eclipse.equinox.p2.osgi' && x.touchpointData.exists(t | t.instructions.exists(entry | entry.value.body ~= /*org.eclipse.equinox.p2.touchpoint.eclipse.setJvm*/)))"),
					new NullProgressMonitor());
			assertEquals("Found wrong number of touchpoints with SetJVM action", 1, queryResultSize(result));
			Iterator<?> iterator = result.iterator();
			while (iterator.hasNext()) {
				Object r = iterator.next();
				System.out.println(r.getClass() + " : " + r);
			}
		}
	}

	public void testArguments() throws Exception {
		IExpression expr = parser.parse("'a' == $0 && 'b' == $1 && 'c' == $2");
		assertEquals(Boolean.TRUE, expr.evaluate(factory.createContext("a", "b", "c")));
	}

	public void testAnonymousMember() throws Exception {
		IExpression expr = parser.parse("$0.class == $1");
		assertEquals(Boolean.TRUE, expr.evaluate(factory.createContext("a", String.class)));
	}

	public void testInstanceOf() throws Exception {
		// Explicit instanceof when rhs is a class
		IExpression expr = parser.parse("$0 ~= $1");
		assertEquals(Boolean.TRUE, expr.evaluate(factory.createContext(Integer.valueOf(4), Number.class)));
	}

	public void testArray() throws Exception {
		IExpression expr = parser.parse("['a', 'b', 'c'].exists(x | x == 'b') && ['a', 'b', 'c'].all(x | 'd' > x)");
		IEvaluationContext ctx = factory.createContext();
		assertEquals(Boolean.TRUE, expr.evaluate(ctx));
		expr = parser.parse("['d', 'e', 'f'].exists(x | ['a', 'b', 'c'].exists(y | x > y))");
		assertEquals(Boolean.TRUE, expr.evaluate(ctx));
		expr = parser.parse("[['d', 'e', 'f'], ['h', 'i', 'j']].exists(x | x.all(y | ['a', 'b', 'c'].all(z | y > z)))");
		assertEquals(Boolean.TRUE, expr.evaluate(ctx));
		expr = parser.parse("[['d', 'e', 'f'], ['h', '3', 'j']].exists(x | x.all(y | ['a', 'b', 'c'].exists(z | y > z)))");
		assertEquals(Boolean.TRUE, expr.evaluate(ctx));
		expr = parser.parse("[['d', 'e', 'f'], ['h', 'i', 'j']].all(x | x.all(y | ['a', 'b', 'c'].all(z | y > z)))");
		assertEquals(Boolean.TRUE, expr.evaluate(ctx));
		expr = parser.parse("[['d', 'e', 'f'], ['h', '3', 'j']].all(x | x.all(y | ['a', 'b', 'c'].all(z | y > z)))");
		assertEquals(Boolean.FALSE, expr.evaluate(ctx)); // 3 < 'b'
	}

	public void testLatest() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/multipleversions1");
		IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createQuery("latest(x | x.id == $0)", "test.bundle"), new NullProgressMonitor());
		assertTrue(queryResultSize(result) == 1);
	}

	public void testRange() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/multipleversions1");
		IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createMatchQuery("version ~= $0", new VersionRange("2.0.0")), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 2);
	}

	public void testProperty() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/multipleversions1");

		IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createMatchQuery("properties.exists(p | boolean(p.value))"), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 3);

		result = repo.query(QueryUtil.createMatchQuery("boolean(properties['org.eclipse.equinox.p2.type.group'])"), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 3);

		Filter filter = TestActivator.context.createFilter("(org.eclipse.equinox.p2.type.group=true)");
		result = repo.query(QueryUtil.createMatchQuery("properties ~= $0", filter), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 3);
	}

	public void testToString() throws Exception {
		String exprString = "select(x | x.id == $0 && (x.version == $1 || x.version == $2)).traverse(set(), _, {requirementsCache, parent | select(" + //
				"parent.requirerements.unique(requirementsCache).select(rc | rc.filter == null || $2 ~= filter(rc.filter)), _, " + //
				"{rcs, child | rcs.exists(rc | child ~= rc)})}).limit(10)";

		IContextExpression<Object> expr = factory.contextExpression(parser.parseQuery(exprString));
		System.out.println(expr.toString());
		assertEquals(exprString, expr.toString());
	}

	public void testSomeAPI() throws Exception {
		// Create some expressions. Note the use of identifiers instead of
		// indexes for the parameters

		IExpression item = factory.variable("item");
		IExpression cmp1 = factory.equals(factory.member(item, "id"), factory.indexedParameter(0));
		IExpression cmp2 = factory.equals(factory.at(factory.member(item, "properties"), factory.indexedParameter(1)), factory.indexedParameter(2));

		IExpression lambda = factory.lambda(item, factory.and(cmp1, cmp2));
		IExpression latest = factory.latest(factory.select(factory.variable("everything"), lambda));

		// Create the query
		IContextExpression<IInstallableUnit> e3 = factory.contextExpression(latest, "test.bundle", "org.eclipse.equinox.p2.type.group", "true");
		IMetadataRepository repo = getMDR("/testData/metadataRepo/multipleversions1");
		IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createQuery(e3), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 1);
	}

	public void testPatch() throws Exception {
		IRequirement[][] applicability = new IRequirement[2][2];
		applicability[0][0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "javax.wsdl", null, null, false, false);
		applicability[0][1] = MetadataFactory.createRequirement("org.eclipse.equinox.p2.eclipse.type", "bundle", null, null, false, false);
		applicability[1][0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "tooling.source.default", null, null, false, false);
		applicability[1][1] = MetadataFactory.createRequirement("org.eclipse.equinox.p2.flavor", "tooling", null, null, false, false);

		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createMatchQuery("$0.exists(rcs | rcs.all(rc | this ~= rc))", (Object) applicability), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 3);
	}

	public void testPattern() throws Exception {
		IProvidedCapability pc = MetadataFactory.createProvidedCapability("org.eclipse.equinox.p2.eclipse.type", "source", null);
		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createMatchQuery("id ~= /tooling.*.default/", pc), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 3);
	}

	public void testLimit() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createQuery("select(x | x.id ~= /tooling.*/).limit(1)"), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 1);

		result = repo.query(QueryUtil.createQuery("select(x | x.id ~= /tooling.*/).limit($0)", Integer.valueOf(2)), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 2);
	}

	public void testNot() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createMatchQuery("!(id ~= /tooling.*/)"), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 4);
	}

	public void testArtifactQuery() throws Exception {
		URI artifactRepo = getTestData("1.1", "/testData/artifactRepo/simple").toURI();

		IArtifactRepositoryManager artifactManager = getArtifactRepositoryManager();
		assertNotNull(artifactManager);

		IArtifactRepository repo = artifactManager.loadRepository(artifactRepo, new NullProgressMonitor());
		IQueryResult<IArtifactKey> result = repo.query(QueryUtil.createMatchQuery(IArtifactKey.class, "classifier ~= /*/"), new NullProgressMonitor());
		assertTrue(queryResultSize(result) > 1);
		Iterator<IArtifactKey> itor = result.iterator();
		while (itor.hasNext())
			assertNotNull(itor.next());

		IQueryResult<IArtifactDescriptor> result2 = repo.descriptorQueryable().query(QueryUtil.createMatchQuery(IArtifactDescriptor.class, "artifactKey.classifier ~= /*/"), new NullProgressMonitor());
		assertTrue(queryResultSize(result2) > 1);
		Iterator<IArtifactDescriptor> itor2 = result2.iterator();
		while (itor2.hasNext())
			assertNotNull(itor2.next());
	}

	public void testClassConstructor() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createQuery(//
				"select(x | x ~= class('org.eclipse.equinox.p2.metadata.IInstallableUnitFragment'))"), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 4);
		repo = getMDR("/testData/galileoM7");
	}

	public void testTouchpoints() throws Exception {
		IMetadataRepository repo = getMDR("/testData/galileoM7");
		IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createMatchQuery(//
				"touchpointType != null && touchpointType.id == 'org.eclipse.equinox.p2.osgi' && touchpointData.exists(t | t.instructions.exists(entry | entry.key == 'zipped'))"), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 616);
	}

	public void testTraverse() throws Exception {
		IMetadataRepository repo = getMDR("/testData/galileoM7");
		IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createQuery(//
				"select(x | x.id == $0 && x.version == $1).traverse(parent | parent.requirements.collect(rc | select(iu | iu ~= rc)).flatten())", //
				"org.eclipse.sdk.feature.group", Version.create("3.5.0.v20090423-7Q7bA7DPR-wM38__Q4iRsmx9z0KOjbpx3AbyvXd-Uq7J2")), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 463);
	}

	public void testTraverseWithFilter() throws Exception {
		// Add some filtering of requirements
		Map<String, String> env = new Hashtable<>();
		env.put("osgi.os", "linux");
		env.put("osgi.ws", "gtk");
		env.put("osgi.arch", "x86");
		IInstallableUnit envIU = InstallableUnit.contextIU(env);

		IContextExpression<IInstallableUnit> expr = factory.contextExpression(parser.parseQuery("" + //
				"select(x | x.id == $0 && x.version == $1).traverse(parent |" + //
				"parent.requirements.select(rc | rc.filter == null || $2 ~= rc.filter).collect(rc | select(iu | iu ~= rc)).flatten())"), "org.eclipse.sdk.feature.group", Version.create("3.5.0.v20090423-7Q7bA7DPR-wM38__Q4iRsmx9z0KOjbpx3AbyvXd-Uq7J2"), envIU);

		IQuery<IInstallableUnit> query = QueryUtil.createQuery(expr);
		IMetadataRepository repo = getMDR("/testData/galileoM7");
		IQueryResult<IInstallableUnit> result = repo.query(query, new NullProgressMonitor());
		assertEquals(queryResultSize(result), 411);
	}

	public void testCommonRequirements() throws Exception {
		// Add some filtering of requirements

		IMetadataRepository repo = getMDR("/testData/galileoM7");
		IInstallableUnit envIU = InstallableUnit.contextIU("gtk", "linux", "x86");
		IContextExpression<IInstallableUnit> expr = factory.contextExpression(parser.parseQuery("" + //
				"select(x | x.id == $0 && x.version == $1).traverse(parent |" + //
				"parent.requirements.select(rc | rc.filter == null || $4 ~= rc.filter).collect(rc | select(iu | iu ~= rc)).flatten()).intersect(" + //
				"select(x | x.id == $2 && x.version == $3).traverse(parent |" + //
				"parent.requirements.select(rc | rc.filter == null || $4 ~= rc.filter).collect(rc | select(iu | iu ~= rc)).flatten()))"), //
				"org.eclipse.pde.feature.group", //
				Version.create("3.5.0.v20090123-7Z7YF8NFE-z0VXhWU26Hu8gY"), //
				"org.eclipse.gmf.feature.group", //
				Version.create("1.1.1.v20090114-0940-7d8B0FXwkKwFanGNHeHHq8ymBgZ"), //
				envIU);

		IQuery<IInstallableUnit> query = QueryUtil.createQuery(expr);
		IQueryResult<IInstallableUnit> result = repo.query(query, new NullProgressMonitor());
		assertEquals(queryResultSize(result), 184);
	}

	public void testMatchQueryInjectionInPredicate() throws Exception {
		IMetadataRepository repo = getMDR("/testData/galileoM7");
		IMatchExpression<IInstallableUnit> expr = factory.matchExpression(parser.parse("iquery($0) || iquery($1)"), new MatchQuery<IInstallableUnit>() {
			@Override
			public boolean isMatch(IInstallableUnit candidate) {
				return "true".equals(candidate.getProperty("org.eclipse.equinox.p2.type.category"));
			}
		}, new MatchQuery<IInstallableUnit>() {
			@Override
			public boolean isMatch(IInstallableUnit candidate) {
				return "true".equals(candidate.getProperty("org.eclipse.equinox.p2.type.group"));
			}
		});
		IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createMatchQuery(expr), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 497);
	}

	public static class MyObject {
		String id;
		Map<String, String> properties = new HashMap<>();

		public MyObject(String id, String key, String value) {
			this.id = id;
			this.properties.put(key, value);
		}

		public String getId() {
			return this.id;
		}

		public Map<String, String> getProperties() {
			return this.properties;
		}
	}

	public void testRootVariableSerialization() throws Exception {
		List<Object> items = new ArrayList<>();

		items.add(new MyObject("ian bull", "foo", "true"));

		// Create some expressions. Note the use of identifiers instead of
		// indexes for the parameters
		IExpression item = factory.variable("item");

		IExpression cmp1 = factory.equals(factory.member(item, "id"), factory.indexedParameter(0));

		IExpression everything = factory.variable("everything");
		IExpression lambda = factory.lambda(item, cmp1);

		IContextExpression<Object> e3 = factory.contextExpression(factory.select(everything, lambda));

		IContextExpression<Object> contextExpression = factory.contextExpression(parser.parseQuery(e3.toString()), "ian bull");
		IQuery<Object> query = QueryUtil.createQuery(Object.class, contextExpression);
		System.out.println(e3);

		IQueryResult<Object> queryResult = query.perform(items.iterator());
		Iterator<Object> iterator = queryResult.iterator();
		while (iterator.hasNext()) {
			System.out.println(iterator.next());
		}

	}

	public void testMatchQueryInjectionInContext() throws Exception {
		IMetadataRepository repo = getMDR("/testData/galileoM7");
		IContextExpression<IInstallableUnit> expr = factory.contextExpression(parser.parseQuery("select(x | iquery($0, x) || iquery($1, x)).latest()"), new MatchQuery<IInstallableUnit>() {
			@Override
			public boolean isMatch(IInstallableUnit candidate) {
				return "true".equals(candidate.getProperty("org.eclipse.equinox.p2.type.category"));
			}
		}, new MatchQuery<IInstallableUnit>() {
			@Override
			public boolean isMatch(IInstallableUnit candidate) {
				return "true".equals(candidate.getProperty("org.eclipse.equinox.p2.type.group"));
			}
		});
		IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createQuery(expr), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 497);
	}

	public void testTranslations() {
		File foo_fragment = new File(TestActivator.getTestDataFolder(), "FragmentPublisherTest/foo.fragment");//$NON-NLS-1$
		File foo = new File(TestActivator.getTestDataFolder(), "FragmentPublisherTest/foo");//$NON-NLS-1$
		BundlesAction bundlesAction = new BundlesAction(new File[] {foo_fragment});
		PublisherInfo info = new PublisherInfo();
		PublisherResult results = new PublisherResult();

		bundlesAction.perform(info, results, new NullProgressMonitor());
		Collection<IInstallableUnit> ius = results.getIUs(null, null);
		assertEquals("1.0", 1, ius.size());

		info = new PublisherInfo();
		results = new PublisherResult();
		bundlesAction = new BundlesAction(new File[] {foo});
		bundlesAction.perform(info, results, new NullProgressMonitor());

		bundlesAction = new BundlesAction(new File[] {foo_fragment});
		bundlesAction.perform(info, results, new NullProgressMonitor());
		ius = results.getIUs(null, null);
		assertEquals("2.0", 3, ius.size());
		QueryableArray queryableArray = new QueryableArray(ius);
		IQueryResult<IInstallableUnit> result = queryableArray.query(QueryUtil.createIUQuery("foo"), null);
		assertEquals("2.1", 1, queryResultSize(result));
		IQuery<IInstallableUnit> lq = QueryUtil.createMatchQuery("translatedProperties[$0] ~= /German*/", new KeyWithLocale("org.eclipse.equinox.p2.name", Locale.GERMAN));
		Iterator<IInstallableUnit> itr = queryableArray.query(lq, new NullProgressMonitor()).iterator();
		assertTrue(itr.hasNext());
		assertEquals("2.8", "foo", itr.next().getId());
		assertFalse(itr.hasNext());
	}

	private IMetadataRepository getMDR(String uri) throws Exception {
		URI metadataRepo = getTestData("1.1", uri).toURI();

		IMetadataRepositoryManager metadataManager = getAgent().getService(IMetadataRepositoryManager.class);
		assertNotNull(metadataManager);

		return metadataManager.loadRepository(metadataRepo, new NullProgressMonitor());
	}

	public void testConsistency() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/qltest");

		IQuery<IInstallableUnit> query1 = QueryUtil.createIUGroupQuery();
		IQuery<IInstallableUnit> query2 = QueryUtil.createQuery("select(iu2 | iu2.requirements.exists(r | r != null) || exists(iu | iu.requirements.exists(r | iu2 ~= r)))");

		IQueryResult<IInstallableUnit> rt2 = repo.query(QueryUtil.createPipeQuery(query1, query2), null);

		IQueryResult<IInstallableUnit> rt = repo.query(query1, null);
		IQueryResult<IInstallableUnit> rt1 = rt.query(QueryUtil.createQuery("select(iu2 | iu2.requirements.exists(r | r != null))"), null);
		rt = rt.query(QueryUtil.createQuery("select(iu | $0.exists(i | i.requirements.exists(r | iu ~= r)))", (Object) rt1.toArray(IInstallableUnit.class)), null);
		/**
		 * should use below line to replace above two lines,
		 * but it throws an unsupported operation exception
		 */
		//		rt = rt.query(query2, null);
		Set<IInstallableUnit> set = rt.toSet();
		set.addAll(rt1.toSet());

		assertTrue("Query results are inconsistent.", set.size() == rt2.toSet().size());
	}
}
