/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import junit.framework.TestCase;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.tests.IUPropertyUtils;
import org.eclipse.equinox.p2.tests.TestData;

/**
 * This tests localization in the publisher
 */
public class LocalizationTests extends TestCase {

	private PublisherInfo info;
	private PublisherResult results;
	private IProgressMonitor monitor;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		info = new PublisherInfo();
		results = new PublisherResult();
		monitor = new NullProgressMonitor();
	}

	public void testFeatureLocalizationDefault() throws IOException {
		File file = TestData.getFile("localizationtests/foofeature", "");
		FeaturesAction featuresAction = new FeaturesAction(new File[] {file});
		featuresAction.perform(info, results, monitor);
		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("fooFeature.feature.group"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "Foo English Feature", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME));
		assertEquals("1.1", "Foo English Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER));
		assertEquals("1.3", "Foo English Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION));
		assertEquals("1.4", "Foo English Copyright", utils.getCopyright(iu).getBody());
		assertEquals("1.5", "Foo English License", utils.getLicense(iu).getBody());
	}

	public void testFeatureLocalizationEN() throws IOException {
		File file = TestData.getFile("localizationtests/foofeature", "");
		FeaturesAction featuresAction = new FeaturesAction(new File[] {file});
		featuresAction.perform(info, results, monitor);
		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("fooFeature.feature.group"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "Foo English Feature", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH));
		assertEquals("1.1", "Foo English Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH));
		assertEquals("1.3", "Foo English Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION, Locale.ENGLISH));
		assertEquals("1.4", "Foo English Copyright", utils.getCopyright(iu, Locale.ENGLISH).getBody());
		assertEquals("1.5", "Foo English License", utils.getLicense(iu, Locale.ENGLISH).getBody());
	}

	public void testFeatureLocalizatioDE() throws IOException {
		File file = TestData.getFile("localizationtests/foofeature", "");
		FeaturesAction featuresAction = new FeaturesAction(new File[] {file});
		featuresAction.perform(info, results, monitor);
		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("fooFeature.feature.group"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "Foo German Feature", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN));
		assertEquals("1.1", "Foo German Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN));
		assertEquals("1.3", "Foo German Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION, Locale.GERMAN));
		assertEquals("1.4", "Foo German Copyright", utils.getCopyright(iu, Locale.GERMAN).getBody());
		assertEquals("1.5", "Foo German License", utils.getLicense(iu, Locale.GERMAN).getBody());
	}

	public void testFeatureJarLocalizationDefault() throws IOException {
		File file = TestData.getFile("localizationtests/foofeature", "");
		FeaturesAction featuresAction = new FeaturesAction(new File[] {file});
		featuresAction.perform(info, results, monitor);
		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("fooFeature.feature.jar"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "Foo English Feature", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME));
		assertEquals("1.1", "Foo English Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER));
		assertEquals("1.3", "Foo English Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION));
		assertEquals("1.4", "Foo English Copyright", utils.getCopyright(iu).getBody());
		assertEquals("1.5", "Foo English License", utils.getLicense(iu).getBody());
	}

	public void testFeatureJarLocalizationEN() throws IOException {
		File file = TestData.getFile("localizationtests/foofeature", "");
		FeaturesAction featuresAction = new FeaturesAction(new File[] {file});
		featuresAction.perform(info, results, monitor);
		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("fooFeature.feature.jar"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "Foo English Feature", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH));
		assertEquals("1.1", "Foo English Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH));
		assertEquals("1.3", "Foo English Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION, Locale.ENGLISH));
		assertEquals("1.4", "Foo English Copyright", utils.getCopyright(iu, Locale.ENGLISH).getBody());
		assertEquals("1.5", "Foo English License", utils.getLicense(iu, Locale.ENGLISH).getBody());
	}

	public void testFeatureJarLocalizatioDE() throws IOException {
		File file = TestData.getFile("localizationtests/foofeature", "");
		FeaturesAction featuresAction = new FeaturesAction(new File[] {file});
		featuresAction.perform(info, results, monitor);
		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("fooFeature.feature.jar"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "Foo German Feature", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN));
		assertEquals("1.1", "Foo German Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN));
		assertEquals("1.3", "Foo German Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION, Locale.GERMAN));
		assertEquals("1.4", "Foo German Copyright", utils.getCopyright(iu, Locale.GERMAN).getBody());
		assertEquals("1.5", "Foo German License", utils.getLicense(iu, Locale.GERMAN).getBody());
	}

	public void testBundleLocalizationEN() throws IOException {
		File file = TestData.getFile("localizationtests/foobundle", "");
		BundlesAction action = new BundlesAction(new File[] {file});
		action.perform(info, results, monitor);
		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("foobundle"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "Foo English Bundle", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH));
		assertEquals("1.1", "Foo English Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH));
	}

	public void testBundleLocalizationDE() throws IOException {
		File file = TestData.getFile("localizationtests/foobundle", "");
		BundlesAction action = new BundlesAction(new File[] {file});
		action.perform(info, results, monitor);
		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("foobundle"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "Foo German Bundle", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN));
		assertEquals("1.1", "Foo German Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN));
	}

	public void testBundleLocalizationEN_alternatePropFile() throws IOException {
		File file = TestData.getFile("localizationtests/barbundle", "");
		BundlesAction action = new BundlesAction(new File[] {file});
		action.perform(info, results, monitor);
		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("barbundle"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "Bar English Bundle", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH));
		assertEquals("1.1", "Bar English Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH));
	}

	public void testBundleLocalizationDE_alternatePropFile() throws IOException {
		File file = TestData.getFile("localizationtests/barbundle", "");
		BundlesAction action = new BundlesAction(new File[] {file});
		action.perform(info, results, monitor);
		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("barbundle"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "Bar German Bundle", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN));
		assertEquals("1.1", "Bar German Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN));
	}

	public void testBundleLocalizationEN_fragment() throws IOException {
		File bundle = TestData.getFile("localizationtests/foobundle2", "");
		File fragment = TestData.getFile("localizationtests/foofragment2", "");
		BundlesAction action = new BundlesAction(new File[] {bundle, fragment});
		action.perform(info, results, monitor);

		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("foobundle2"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "Foo English Bundle - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH));
		assertEquals("1.1", "Foo English Provider - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH));
	}

	public void testBundleLocalizationDE_fragment() throws IOException {
		File bundle = TestData.getFile("localizationtests/foobundle2", "");
		File fragment = TestData.getFile("localizationtests/foofragment2", "");
		BundlesAction action = new BundlesAction(new File[] {bundle, fragment});
		action.perform(info, results, monitor);

		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("foobundle2"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "Foo German Bundle - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN));
		assertEquals("1.1", "Foo German Provider - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN));
	}

	public void testBundleLocalizationEN_alternatePropFile_fragment() throws IOException {
		File bundle = TestData.getFile("localizationtests/barbundle2", "");
		File fragment = TestData.getFile("localizationtests/barfragment2", "");
		BundlesAction action = new BundlesAction(new File[] {bundle, fragment});
		action.perform(info, results, monitor);

		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("barbundle2"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "Bar English Bundle - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH));
		assertEquals("1.1", "Bar English Provider - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH));
	}

	public void testBundleLocalizationDE_alternatePropFile_fragment() throws IOException {
		File bundle = TestData.getFile("localizationtests/barbundle2", "");
		File fragment = TestData.getFile("localizationtests/barfragment2", "");
		BundlesAction action = new BundlesAction(new File[] {bundle, fragment});
		action.perform(info, results, monitor);

		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("barbundle2"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "Bar German Bundle - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN));
		assertEquals("1.1", "Bar German Provider - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN));
	}

	public void testFragmentTranslation_EN() throws IOException {
		File bundle = TestData.getFile("localizationtests/barbundle2", "");
		File fragment = TestData.getFile("localizationtests/barfragment2", "");
		BundlesAction action = new BundlesAction(new File[] {bundle, fragment});
		action.perform(info, results, monitor);

		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("barfragment2"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "English Fragment Name", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH));
		assertEquals("1.1", "English Fragment Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH));
	}

	public void testFragmentTranslation_DE() throws IOException {
		File bundle = TestData.getFile("localizationtests/barbundle2", "");
		File fragment = TestData.getFile("localizationtests/barfragment2", "");
		BundlesAction action = new BundlesAction(new File[] {bundle, fragment});
		action.perform(info, results, monitor);

		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("barfragment2"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "German Fragment Name", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN));
		assertEquals("1.1", "German Fragment Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN));
	}

	public void testSite_EN() throws IOException {
		File feature = TestData.getFile("localizationtests/foofeature", "");
		URI site = TestData.getFile("localizationtests/site", "").toURI();

		FeaturesAction action = new FeaturesAction(new File[] {feature});
		SiteXMLAction siteAction = new SiteXMLAction(site, "foo");
		action.perform(info, results, monitor);
		siteAction.perform(info, results, monitor);

		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("foo.new_category_1"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "English Category Name", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH));
		assertEquals("1.1", "English Category Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION, Locale.ENGLISH));

	}

	public void testSite_DE() throws IOException {
		File feature = TestData.getFile("localizationtests/foofeature", "");
		URI site = TestData.getFile("localizationtests/site", "").toURI();

		FeaturesAction action = new FeaturesAction(new File[] {feature});
		SiteXMLAction siteAction = new SiteXMLAction(site, "foo");
		action.perform(info, results, monitor);
		siteAction.perform(info, results, monitor);

		IUPropertyUtils utils = new IUPropertyUtils(results.query(new InstallableUnitQuery((String) null), new Collector(), monitor));
		Collector collector = results.query(new InstallableUnitQuery("foo.new_category_1"), new Collector(), monitor);
		IInstallableUnit iu = (IInstallableUnit) collector.iterator().next();
		assertEquals("1.0", "German Category Name", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN));
		assertEquals("1.1", "German Category Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION, Locale.GERMAN));

	}
}
