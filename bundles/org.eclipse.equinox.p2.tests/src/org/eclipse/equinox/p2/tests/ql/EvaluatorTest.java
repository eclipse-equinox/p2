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
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.MatchQuery;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.ql.*;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.Filter;

public class EvaluatorTest extends AbstractProvisioningTest {
	private static final IExpressionParser parser = QL.newParser();

	public void testArguments() throws Exception {
		IMatchExpression expr = parser.parsePredicate("'a' == $0 && 'b' == $1 && 'c' == $2");
		assertEquals(Boolean.TRUE, expr.evaluate(expr.createContext(new Object[] {"a", "b", "c"})));
	}

	public void testAnonymousMember() throws Exception {
		IMatchExpression expr = parser.parsePredicate("$0.class == $1");
		assertEquals(Boolean.TRUE, expr.evaluate(expr.createContext(new Object[] {"a", String.class})));
	}

	public void testInstanceOf() throws Exception {
		// Explicit instanceof when rhs is a class
		IMatchExpression expr = parser.parsePredicate("$0 ~= $1");
		assertEquals(Boolean.TRUE, expr.evaluate(expr.createContext(new Object[] {new Integer(4), Number.class})));
	}

	public void testArray() throws Exception {
		IMatchExpression expr = parser.parsePredicate("['a', 'b', 'c'].exists(x | x == 'b') && ['a', 'b', 'c'].all(x | 'd' > x)");
		assertEquals(Boolean.TRUE, expr.evaluate(expr.createContext(null)));
		expr = parser.parsePredicate("['d', 'e', 'f'].exists(x | ['a', 'b', 'c'].exists(y | x > y))");
		assertEquals(Boolean.TRUE, expr.evaluate(expr.createContext(null)));
		expr = parser.parsePredicate("[['d', 'e', 'f'], ['h', 'i', 'j']].exists(x | x.all(y | ['a', 'b', 'c'].all(z | y > z)))");
		assertEquals(Boolean.TRUE, expr.evaluate(expr.createContext(null)));
		expr = parser.parsePredicate("[['d', 'e', 'f'], ['h', '3', 'j']].exists(x | x.all(y | ['a', 'b', 'c'].exists(z | y > z)))");
		assertEquals(Boolean.TRUE, expr.evaluate(expr.createContext(null)));
		expr = parser.parsePredicate("[['d', 'e', 'f'], ['h', 'i', 'j']].all(x | x.all(y | ['a', 'b', 'c'].all(z | y > z)))");
		assertEquals(Boolean.TRUE, expr.evaluate(expr.createContext(null)));
		expr = parser.parsePredicate("[['d', 'e', 'f'], ['h', '3', 'j']].all(x | x.all(y | ['a', 'b', 'c'].all(z | y > z)))");
		assertEquals(Boolean.FALSE, expr.evaluate(expr.createContext(null))); // 3 < 'b'
	}

	public void testLatest() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/multipleversions1");
		IQueryResult result = repo.query(new QLContextQuery("latest(x | x.id == $0)", "test.bundle"), new NullProgressMonitor());
		assertTrue(queryResultSize(result) == 1);
	}

	public void testRange() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/multipleversions1");
		IQueryResult result = repo.query(new QLMatchQuery("version ~= $0", new VersionRange("2.0.0")), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 2);
	}

	public void testProperty() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/multipleversions1");

		IQueryResult result = repo.query(new QLMatchQuery("properties.exists(p | boolean(p.value))"), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 3);

		result = repo.query(new QLMatchQuery("boolean(properties['org.eclipse.equinox.p2.type.group'])"), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 3);

		Filter filter = TestActivator.context.createFilter("(org.eclipse.equinox.p2.type.group=true)");
		result = repo.query(new QLMatchQuery("properties ~= $0", filter), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 3);
	}

	public void testToString() throws Exception {
		String exprString = "select(x | x.id == $0 && (x.version == $1 || x.version == $2)).traverse(set(), _, {requirementsCache, parent | select(" + //
				"parent.requiredCapabilities.unique(requirementsCache).select(rc | rc.filter == null || $2 ~= filter(rc.filter)), _, " + //
				"{rcs, child | rcs.exists(rc | child ~= rc)})}).limit(10)";

		IContextExpression expr = parser.parseQuery(exprString);
		System.out.println(expr.toString());
		assertEquals(exprString, expr.toString());
	}

	public void testSomeAPI() throws Exception {
		// Create some expressions. Note the use of identifiers instead of
		// indexes for the parameters

		IExpressionFactory factory = QL.getFactory();
		IExpression item = factory.variable("item");
		IExpression cmp1 = factory.equals(factory.member(item, "id"), factory.keyedParameter("id"));
		IExpression cmp2 = factory.equals(factory.at(factory.member(item, "properties"), factory.keyedParameter("propKey")), factory.keyedParameter("propValue"));

		IExpression everything = factory.variable("everything");
		IExpression lambda = factory.lambda(item, factory.and(new IExpression[] {cmp1, cmp2}));
		IExpression latest = factory.latest(factory.select(everything, lambda));
		IContextExpression e3 = factory.contextExpression(latest);

		// Put the parameters in a map
		Map args = new HashMap();
		args.put("id", "test.bundle");
		args.put("propKey", "org.eclipse.equinox.p2.type.group");
		args.put("propValue", "true");

		// Create the query
		IMetadataRepository repo = getMDR("/testData/metadataRepo/multipleversions1");
		IQueryResult result = repo.query(new QLContextQuery(IInstallableUnit.class, e3, new Object[] {args}), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 1);
	}

	public void testMember() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		IProvidedCapability pc = MetadataFactory.createProvidedCapability("org.eclipse.equinox.p2.eclipse.type", "source", null);
		IQueryResult result = repo.query(new QLMatchQuery(IInstallableUnitFragment.class, "host.exists(h | $0 ~= h)", new Object[] {pc}), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 1);
	}

	public void testPatch() throws Exception {
		IRequiredCapability[][] applicability = new IRequiredCapability[2][2];
		applicability[0][0] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "javax.wsdl", null, null, false, false);
		applicability[0][1] = MetadataFactory.createRequiredCapability("org.eclipse.equinox.p2.eclipse.type", "bundle", null, null, false, false);
		applicability[1][0] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "tooling.source.default", null, null, false, false);
		applicability[1][1] = MetadataFactory.createRequiredCapability("org.eclipse.equinox.p2.flavor", "tooling", null, null, false, false);

		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		IQueryResult result = repo.query(new QLMatchQuery("$0.exists(rcs | rcs.all(rc | item ~= rc))", applicability), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 3);
	}

	public void testPattern() throws Exception {
		IProvidedCapability pc = MetadataFactory.createProvidedCapability("org.eclipse.equinox.p2.eclipse.type", "source", null);
		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		IQueryResult result = repo.query(new QLMatchQuery("id ~= /tooling.*.default/", pc), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 3);
	}

	public void testLimit() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		IQueryResult result = repo.query(new QLContextQuery("select(x | x.id ~= /tooling.*/).limit(1)"), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 1);

		result = repo.query(new QLContextQuery("select(x | x.id ~= /tooling.*/).limit($0)", new Integer(2)), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 2);
	}

	public void testNot() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		IQueryResult result = repo.query(new QLMatchQuery("!(id ~= /tooling.*/)"), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 4);
	}

	public void testArtifactQuery() throws Exception {
		URI artifactRepo = getTestData("1.1", "/testData/artifactRepo/simple").toURI();

		IArtifactRepositoryManager artifactManager = getArtifactRepositoryManager();
		assertNotNull(artifactManager);

		IArtifactRepository repo = artifactManager.loadRepository(artifactRepo, new NullProgressMonitor());
		IQueryResult result = repo.query(new QLMatchQuery(IArtifactKey.class, "classifier ~= /*/", null), new NullProgressMonitor());
		assertTrue(queryResultSize(result) > 1);
		Iterator itor = result.iterator();
		while (itor.hasNext())
			assertTrue(itor.next() instanceof IArtifactKey);

		result = repo.query(new QLMatchQuery(IArtifactDescriptor.class, "artifactKey.classifier ~= /*/", null), new NullProgressMonitor());
		assertTrue(queryResultSize(result) > 1);
		itor = result.iterator();
		while (itor.hasNext())
			assertTrue(itor.next() instanceof IArtifactDescriptor);
	}

	public void testClassConstructor() throws Exception {
		IMetadataRepository repo = getMDR("/testData/metadataRepo/wsdlTestRepo");
		IQueryResult result = repo.query(new QLContextQuery(//
				"select(x | x ~= class('org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnitFragment'))"), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 4);
		repo = getMDR("/testData/galileoM7");
	}

	public void testTraverseWithoutIndex() throws Exception {
		IMetadataRepository repo = getMDR("/testData/galileoM7");
		IQueryResult result = repo.query(new QLContextQuery( //
				"select(x | x.id == $0 && x.version == $1).traverse(parent | select(" + //
						"child | parent.requiredCapabilities.exists(rc | child ~= rc)))", //
				"org.eclipse.sdk.feature.group", Version.create("3.5.0.v20090423-7Q7bA7DPR-wM38__Q4iRsmx9z0KOjbpx3AbyvXd-Uq7J2")), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 463);
	}

	public void testTraverseWithIndex() throws Exception {
		IMetadataRepository repo = getMDR("/testData/galileoM7");
		IQueryResult result = repo.query(//
				new QLContextQuery("" + //
						"select(x | x.id == $0 && x.version == $1).traverse(capabilityIndex(everything), _, { index, parent |" + //
						"index.satisfiesAny(parent.requiredCapabilities)})", //
						"org.eclipse.sdk.feature.group",//
						Version.create("3.5.0.v20090423-7Q7bA7DPR-wM38__Q4iRsmx9z0KOjbpx3AbyvXd-Uq7J2")),//
				new NullProgressMonitor());
		assertEquals(queryResultSize(result), 463);
	}

	public void testTraverseWithIndexAndFilter() throws Exception {
		// Add some filtering of requirements
		IContextExpression expr = parser.parseQuery("" + //
				"select(x | x.id == $0 && x.version == $1).traverse(capabilityIndex(everything), _, { index, parent |" + //
				"index.satisfiesAny(parent.requiredCapabilities.select(rc | rc.filter == null || $2 ~= filter(rc.filter)))})");

		Map env = new Hashtable();
		env.put("osgi.os", "linux");
		env.put("osgi.ws", "gtk");
		env.put("osgi.arch", "x86");
		QLContextQuery query = new QLContextQuery(IInstallableUnit.class, expr, new Object[] {"org.eclipse.sdk.feature.group", Version.create("3.5.0.v20090423-7Q7bA7DPR-wM38__Q4iRsmx9z0KOjbpx3AbyvXd-Uq7J2"), env});

		IMetadataRepository repo = getMDR("/testData/galileoM7");
		IQueryResult result = repo.query(query, new NullProgressMonitor());
		assertEquals(queryResultSize(result), 411);
	}

	public void testCommonRequirements() throws Exception {
		// Add some filtering of requirements

		IMetadataRepository repo = getMDR("/testData/galileoM7");
		QLContextQuery indexQuery = new QLContextQuery("capabilityIndex(everything)");
		Object index = indexQuery.query(QL.newQueryContext(repo));

		IContextExpression expr = parser.parseQuery("" + //
				"select(x | x.id == $0 && x.version == $1).traverse(parent |" + //
				"$5.satisfiesAny(parent.requiredCapabilities.select(rc | rc.filter == null || $4 ~= filter(rc.filter)))) && " + //
				"select(x | x.id == $2 && x.version == $3).traverse(parent |" + //
				"$5.satisfiesAny(parent.requiredCapabilities.select(rc | rc.filter == null || $4 ~= filter(rc.filter))))");

		Map env = new Hashtable();
		env.put("osgi.os", "linux");
		env.put("osgi.ws", "gtk");
		env.put("osgi.arch", "x86");

		QLContextQuery query = new QLContextQuery(IInstallableUnit.class, expr, new Object[] { //
				"org.eclipse.pde.feature.group", //
						Version.create("3.5.0.v20090123-7Z7YF8NFE-z0VXhWU26Hu8gY"), //
						"org.eclipse.gmf.feature.group", //
						Version.create("1.1.1.v20090114-0940-7d8B0FXwkKwFanGNHeHHq8ymBgZ"), //
						env,//
						index});

		IQueryResult result = repo.query(query, new NullProgressMonitor());
		assertEquals(queryResultSize(result), 184);
	}

	public void testMatchQueryInjectionInPredicate() throws Exception {
		IMetadataRepository repo = getMDR("/testData/galileoM7");
		IMatchExpression expr = parser.parsePredicate("iquery($0) || iquery($1)");
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
		IQueryResult result = repo.query(new QLMatchQuery(IInstallableUnit.class, expr, new Object[] {q1, q2}), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 497);
	}

	public void testMatchQueryInjectionInContext() throws Exception {
		IMetadataRepository repo = getMDR("/testData/galileoM7");
		IContextExpression expr = parser.parseQuery("select(x | iquery($0, x) || iquery($1, x)).latest()");
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
		IQueryResult result = repo.query(new QLContextQuery(IInstallableUnit.class, expr, new Object[] {q1, q2}), new NullProgressMonitor());
		assertEquals(queryResultSize(result), 497);
	}

	public void testTranslations() {
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
		IQueryResult result = queryableArray.query(new InstallableUnitQuery("foo"), null);
		assertEquals("2.1", 1, queryResultSize(result));

		QLMatchQuery lq = new QLMatchQuery("translations['org.eclipse.equinox.p2.name'] ~= /German*/");
		lq.setLocale(Locale.GERMAN);
		Iterator itr = queryableArray.query(lq, new NullProgressMonitor()).iterator();
		assertTrue(itr.hasNext());
		assertEquals("2.8", "foo", ((IInstallableUnit) itr.next()).getId());
		assertFalse(itr.hasNext());
	}

	private IMetadataRepository getMDR(String uri) throws Exception {
		URI metadataRepo = getTestData("1.1", uri).toURI();

		IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
		assertNotNull(metadataManager);

		return metadataManager.loadRepository(metadataRepo, new NullProgressMonitor());
	}
}
