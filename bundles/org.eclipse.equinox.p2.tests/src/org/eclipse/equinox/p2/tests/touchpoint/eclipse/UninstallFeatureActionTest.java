/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.*;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.UninstallFeatureAction;
import org.eclipse.equinox.internal.p2.update.Site;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class UninstallFeatureActionTest extends AbstractProvisioningTest {

	private static final String UPDATE_FEATURE_APPLICATION_PROP = "org.eclipse.update.feature.application"; //$NON-NLS-1$
	private static final String UPDATE_FEATURE_PLUGIN_PROP = "org.eclipse.update.feature.plugin"; //$NON-NLS-1$
	private static final String UPDATE_FEATURE_PRIMARY_PROP = "org.eclipse.update.feature.primary"; //$NON-NLS-1$

	public UninstallFeatureActionTest(String name) {
		super(name);
	}

	public UninstallFeatureActionTest() {
		super("");
	}

	public void testExecuteUndo() {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		profileProperties.setProperty(IProfile.PROP_CACHE, installFolder.toString());
		IProfile profile = createProfile("test", profileProperties);

		IFileArtifactRepository bundlePool = Util.getBundlePoolRepository(getAgent(), profile);
		File featureSource = getTestData("1.0", "/testData/eclipseTouchpoint/features/org.eclipse.rcp_3.3.0.v20070607-8y8eE8NEbsN3X_fjWS8HPNG");
		File targetPlugins = new File(installFolder, "features");
		assertTrue(targetPlugins.mkdir());
		File featureTarget = new File(targetPlugins, "org.eclipse.rcp_3.3.0.v20070607-8y8eE8NEbsN3X_fjWS8HPNG");
		copy("2.0", featureSource, featureTarget);

		FeatureParser parser = new FeatureParser();
		Feature feature = parser.parse(featureTarget);

		final PublisherInfo info = new PublisherInfo();
		info.setArtifactRepository(bundlePool);
		IArtifactKey key = FeaturesAction.createFeatureArtifactKey(feature.getId(), feature.getVersion());
		IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(info, key, featureTarget);
		((SimpleArtifactDescriptor) descriptor).setRepositoryProperty("artifact.folder", Boolean.TRUE.toString());
		IInstallableUnit iu = FeaturesAction.createFeatureJarIU(feature, info);

		bundlePool.addDescriptor(descriptor);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);
		InstallableUnitOperand operand = new InstallableUnitOperand(null, iu);
		parameters.put("iu", operand.second());
		touchpoint.initializeOperand(profile, parameters);

		parameters.put(ActionConstants.PARM_FEATURE, key.toString());
		parameters.put(ActionConstants.PARM_FEATURE_ID, ActionConstants.PARM_DEFAULT_VALUE);
		parameters.put(ActionConstants.PARM_FEATURE_VERSION, ActionConstants.PARM_DEFAULT_VALUE);
		parameters = Collections.unmodifiableMap(parameters);

		PlatformConfigurationWrapper configuration = (PlatformConfigurationWrapper) parameters.get(EclipseTouchpoint.PARM_PLATFORM_CONFIGURATION);
		assertNotNull(configuration);

		// Note: This is from code in InstallFeatureAction
		String pluginId = iu.getProperty(UPDATE_FEATURE_PLUGIN_PROP);
		boolean isPrimary = Boolean.parseBoolean(iu.getProperty(UPDATE_FEATURE_PRIMARY_PROP));
		String application = iu.getProperty(UPDATE_FEATURE_APPLICATION_PROP);
		// TODO this isn't right... but we will leave it for now because we don't actually use the value in the install
		String pluginVersion = key.getVersion().toString();
		configuration.addFeatureEntry(featureTarget, feature.getId(), feature.getVersion(), pluginId, pluginVersion, isPrimary, application, /*root*/null, iu.getProperty(Site.PROP_LINK_FILE));

		URI siteURI = featureTarget.getParentFile().getParentFile().toURI();
		assertTrue(configuration.containsFeature(siteURI, feature.getId(), feature.getVersion()));
		UninstallFeatureAction action = new UninstallFeatureAction();
		action.execute(parameters);
		assertFalse(configuration.containsFeature(siteURI, feature.getId(), feature.getVersion()));
		action.undo(parameters);
		assertTrue(configuration.containsFeature(siteURI, feature.getId(), feature.getVersion()));
	}
}
