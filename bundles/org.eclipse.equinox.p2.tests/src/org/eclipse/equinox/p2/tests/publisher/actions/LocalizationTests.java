/******************************************************************************* 
* Copyright (c) 2009, 2011 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
*   IBM - Ongoing development
******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.TranslationSupport;
import org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.*;

/**
 * This tests localization in the publisher
 */
public class LocalizationTests extends AbstractProvisioningTest {

	private PublisherInfo info;
	private IProgressMonitor monitor;
	private PublisherResult results;

	private TranslationSupport getTranslationSupport() {
		TranslationSupport utils = new TranslationSupport();
		utils.setTranslationSource(results.query(QueryUtil.createIUQuery((String) null), monitor));
		return utils;
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		info = new PublisherInfo();
		info.setMetadataRepository(new TestMetadataRepository(getAgent(), new IInstallableUnit[0]));
		results = new PublisherResult();
		monitor = new NullProgressMonitor();
	}

	public void testBundleLocalizationDE() throws IOException {
		//Tests with the default localization file location
		File file = TestData.getFile("localizationtests/foobundle", "");
		BundlesAction action = new BundlesAction(new File[] {file});
		action.perform(info, results, monitor);
		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("foobundle"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Foo German Bundle", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN.toString()));
		assertEquals("1.1", "Foo German Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN.toString()));
	}

	public void testBundleLocalizationDefaultDE() throws IOException {
		File file = TestData.getFile("localizationtests/foobundle_default", "");
		BundlesAction action = new BundlesAction(new File[] {file});
		action.perform(info, results, monitor);
		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("foobundle"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Foo German Bundle", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN.toString()));
		assertEquals("1.1", "Foo German Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN.toString()));
	}

	public void testBundleLocalizationDefaultDEJar() throws IOException {
		File file = TestData.getFile("localizationtests/foobundle_1.0.0.qualifier.jar", "");
		BundlesAction action = new BundlesAction(new File[] {file});
		action.perform(info, results, monitor);
		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("foobundle"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Foo German Bundle", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN.toString()));
		assertEquals("1.1", "Foo German Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN.toString()));
	}

	public void testBundleLocalizationOSGI_INF_DE() throws IOException {
		File file = TestData.getFile("localizationtests/foobundle_osgi-inf", "");
		BundlesAction action = new BundlesAction(new File[] {file});
		action.perform(info, results, monitor);
		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("foobundle"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Foo German Bundle", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN.toString()));
		assertEquals("1.1", "Foo German Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN.toString()));
	}

	public void testBundleLocalizationDE_alternatePropFile() throws IOException {
		File file = TestData.getFile("localizationtests/barbundle", "");
		BundlesAction action = new BundlesAction(new File[] {file});
		action.perform(info, results, monitor);
		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("barbundle"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Bar German Bundle", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN.toString()));
		assertEquals("1.1", "Bar German Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN.toString()));
	}

	public void testBundleLocalizationDE_alternatePropFile_fragment() throws IOException {
		File bundle = TestData.getFile("localizationtests/barbundle2", "");
		File fragment = TestData.getFile("localizationtests/barfragment2", "");
		BundlesAction action = new BundlesAction(new File[] {bundle, fragment});
		action.perform(info, results, monitor);

		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("barbundle2"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Bar German Bundle - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN.toString()));
		assertEquals("1.1", "Bar German Provider - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN.toString()));
	}

	public void testBundleLocalizationDE_fragment() throws IOException {
		File bundle = TestData.getFile("localizationtests/foobundle2", "");
		File fragment = TestData.getFile("localizationtests/foofragment2", "");
		BundlesAction action = new BundlesAction(new File[] {bundle, fragment});
		action.perform(info, results, monitor);

		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("foobundle2"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Foo German Bundle - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN.toString()));
		assertEquals("1.1", "Foo German Provider - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN.toString()));
	}

	public void testBundleLocalizationOSGI_INF_EN() throws IOException {
		File file = TestData.getFile("localizationtests/foobundle_osgi-inf", "");
		BundlesAction action = new BundlesAction(new File[] {file});
		action.perform(info, results, monitor);
		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("foobundle"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Foo English Bundle", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH.toString()));
		assertEquals("1.1", "Foo English Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH.toString()));
	}

	public void testBundleLocalizationEN() throws IOException {
		File file = TestData.getFile("localizationtests/foobundle", "");
		BundlesAction action = new BundlesAction(new File[] {file});
		action.perform(info, results, monitor);
		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("foobundle"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Foo English Bundle", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH.toString()));
		assertEquals("1.1", "Foo English Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH.toString()));
	}

	public void testBundleLocalizationENDefault() throws IOException {
		// Tests with the default localization file location
		File file = TestData.getFile("localizationtests/foobundle_default", "");
		BundlesAction action = new BundlesAction(new File[] {file});
		action.perform(info, results, monitor);
		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("foobundle"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Foo English Bundle", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH.toString()));
		assertEquals("1.1", "Foo English Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH.toString()));
	}

	public void testBundleLocalizationEN_alternatePropFile() throws IOException {
		File file = TestData.getFile("localizationtests/barbundle", "");
		BundlesAction action = new BundlesAction(new File[] {file});
		action.perform(info, results, monitor);
		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("barbundle"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Bar English Bundle", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH.toString()));
		assertEquals("1.1", "Bar English Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH.toString()));
	}

	public void testBundleLocalizationEN_alternatePropFile_fragment() throws IOException {
		File bundle = TestData.getFile("localizationtests/barbundle2", "");
		File fragment = TestData.getFile("localizationtests/barfragment2", "");
		BundlesAction action = new BundlesAction(new File[] {bundle, fragment});
		action.perform(info, results, monitor);

		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("barbundle2"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Bar English Bundle - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH.toString()));
		assertEquals("1.1", "Bar English Provider - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH.toString()));
	}

	public void testBundleLocalizationEN_fragment() throws IOException {
		File bundle = TestData.getFile("localizationtests/foobundle2", "");
		File fragment = TestData.getFile("localizationtests/foofragment2", "");
		BundlesAction action = new BundlesAction(new File[] {bundle, fragment});
		action.perform(info, results, monitor);

		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("foobundle2"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Foo English Bundle - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH.toString()));
		assertEquals("1.1", "Foo English Provider - Translated in the Fragment", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH.toString()));
	}

	public void testFeatureJarLocalizatioDE() throws IOException {
		File file = TestData.getFile("localizationtests/foofeature", "");
		FeaturesAction featuresAction = new FeaturesAction(new File[] {file});
		featuresAction.perform(info, results, monitor);
		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("fooFeature.feature.jar"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Foo German Feature", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN.toString()));
		assertEquals("1.1", "Foo German Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN.toString()));
		assertEquals("1.3", "Foo German Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION, Locale.GERMAN.toString()));
		assertEquals("1.4", "Foo German Copyright", utils.getCopyright(iu, Locale.GERMAN.toString()).getBody());
		assertEquals("1.5", "Foo German License", utils.getLicenses(iu, Locale.GERMAN.toString())[0].getBody());
	}

	public void testFeatureJarLocalizationDefault() throws IOException {
		File file = TestData.getFile("localizationtests/foofeature", "");
		FeaturesAction featuresAction = new FeaturesAction(new File[] {file});
		featuresAction.perform(info, results, monitor);
		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("fooFeature.feature.jar"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Foo English Feature", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME));
		assertEquals("1.1", "Foo English Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER));
		assertEquals("1.3", "Foo English Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION));
		assertEquals("1.4", "Foo English Copyright", utils.getCopyright(iu, null).getBody());
		assertEquals("1.5", "Foo English License", utils.getLicenses(iu, null)[0].getBody());
	}

	public void testFeatureJarLocalizationEN() throws IOException {
		File file = TestData.getFile("localizationtests/foofeature", "");
		FeaturesAction featuresAction = new FeaturesAction(new File[] {file});
		featuresAction.perform(info, results, monitor);
		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("fooFeature.feature.jar"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Foo English Feature", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH.toString()));
		assertEquals("1.1", "Foo English Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH.toString()));
		assertEquals("1.3", "Foo English Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION, Locale.ENGLISH.toString()));
		assertEquals("1.4", "Foo English Copyright", utils.getCopyright(iu, Locale.ENGLISH.toString()).getBody());
		assertEquals("1.5", "Foo English License", utils.getLicenses(iu, Locale.ENGLISH.toString())[0].getBody());
	}

	public void testFeatureLocalizatioDE() throws IOException {
		File file = TestData.getFile("localizationtests/foofeature", "");
		FeaturesAction featuresAction = new FeaturesAction(new File[] {file});
		featuresAction.perform(info, results, monitor);
		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("fooFeature.feature.group"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Foo German Feature", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN.toString()));
		assertEquals("1.1", "Foo German Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN.toString()));
		assertEquals("1.3", "Foo German Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION, Locale.GERMAN.toString()));
		assertEquals("1.4", "Foo German Copyright", utils.getCopyright(iu, Locale.GERMAN.toString()).getBody());
		assertEquals("1.5", "Foo German License", utils.getLicenses(iu, Locale.GERMAN.toString())[0].getBody());
	}

	public void testFeatureLocalizationDefault() throws IOException {
		File file = TestData.getFile("localizationtests/foofeature", "");
		FeaturesAction featuresAction = new FeaturesAction(new File[] {file});
		featuresAction.perform(info, results, monitor);
		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("fooFeature.feature.group"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Foo English Feature", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME));
		assertEquals("1.1", "Foo English Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER));
		assertEquals("1.3", "Foo English Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION));
		assertEquals("1.4", "Foo English Copyright", utils.getCopyright(iu, null).getBody());
		assertEquals("1.5", "Foo English License", utils.getLicenses(iu, null)[0].getBody());
	}

	public void testFeatureLocalizationEN() throws IOException {
		File file = TestData.getFile("localizationtests/foofeature", "");
		FeaturesAction featuresAction = new FeaturesAction(new File[] {file});
		featuresAction.perform(info, results, monitor);
		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("fooFeature.feature.group"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "Foo English Feature", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH.toString()));
		assertEquals("1.1", "Foo English Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH.toString()));
		assertEquals("1.3", "Foo English Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION, Locale.ENGLISH.toString()));
		assertEquals("1.4", "Foo English Copyright", utils.getCopyright(iu, Locale.ENGLISH.toString()).getBody());
		assertEquals("1.5", "Foo English License", utils.getLicenses(iu, Locale.ENGLISH.toString())[0].getBody());
	}

	public void testFragmentTranslation_DE() throws IOException {
		File bundle = TestData.getFile("localizationtests/barbundle2", "");
		File fragment = TestData.getFile("localizationtests/barfragment2", "");
		BundlesAction action = new BundlesAction(new File[] {bundle, fragment});
		action.perform(info, results, monitor);

		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("barfragment2"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "German Fragment Name", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN.toString()));
		assertEquals("1.1", "German Fragment Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.GERMAN.toString()));
	}

	public void testFragmentTranslation_EN() throws IOException {
		File bundle = TestData.getFile("localizationtests/barbundle2", "");
		File fragment = TestData.getFile("localizationtests/barfragment2", "");
		BundlesAction action = new BundlesAction(new File[] {bundle, fragment});
		action.perform(info, results, monitor);

		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("barfragment2"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "English Fragment Name", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH.toString()));
		assertEquals("1.1", "English Fragment Provider", utils.getIUProperty(iu, IInstallableUnit.PROP_PROVIDER, Locale.ENGLISH.toString()));
	}

	public void testSite_DE() throws IOException {
		File feature = TestData.getFile("localizationtests/foofeature", "");
		URI site = TestData.getFile("localizationtests/site", "").toURI();

		FeaturesAction action = new FeaturesAction(new File[] {feature});
		SiteXMLAction siteAction = new SiteXMLAction(site, "foo");
		action.perform(info, results, monitor);
		siteAction.perform(info, results, monitor);

		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("foo.new_category_1"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "German Category Name", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.GERMAN.toString()));
		assertEquals("1.1", "German Category Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION, Locale.GERMAN.toString()));

	}

	public void testSite_EN() throws IOException {
		File feature = TestData.getFile("localizationtests/foofeature", "");
		URI site = TestData.getFile("localizationtests/site", "").toURI();

		FeaturesAction action = new FeaturesAction(new File[] {feature});
		SiteXMLAction siteAction = new SiteXMLAction(site, "foo");
		action.perform(info, results, monitor);
		siteAction.perform(info, results, monitor);

		TranslationSupport utils = getTranslationSupport();
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery("foo.new_category_1"), monitor);
		IInstallableUnit iu = (IInstallableUnit) queryResult.iterator().next();
		assertEquals("1.0", "English Category Name", utils.getIUProperty(iu, IInstallableUnit.PROP_NAME, Locale.ENGLISH.toString()));
		assertEquals("1.1", "English Category Description", utils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION, Locale.ENGLISH.toString()));

	}
}
