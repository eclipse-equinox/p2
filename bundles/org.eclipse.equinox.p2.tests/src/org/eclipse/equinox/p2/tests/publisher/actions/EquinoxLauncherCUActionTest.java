/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.easymock.EasyMock.*;

import java.util.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.IVersionAdvice;
import org.eclipse.equinox.p2.publisher.actions.VersionAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.EquinoxLauncherCUAction;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

@SuppressWarnings({"unchecked"})
public class EquinoxLauncherCUActionTest extends ActionTest {

	private static String a_ID = "iua.source"; //$NON-NLS-1$
	private static String b_ID = "iub";//$NON-NLS-1$
	private static String c_ID = EquinoxLauncherCUAction.ORG_ECLIPSE_EQUINOX_LAUNCHER;
	private static String d_ID = EquinoxLauncherCUAction.ORG_ECLIPSE_EQUINOX_LAUNCHER + ".source";//$NON-NLS-1$

	public void setUp() throws Exception {
		setupPublisherInfo();
		setupPublisherResult();
		testAction = new EquinoxLauncherCUAction(flavorArg, new String[] {configSpec});
	}

	public void testEquinoxLauncherCUAction() throws Exception {
		testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		verifyResults();
		debug("Completed EquinoxLauncherCUAction test.");//$NON-NLS-1$
	}

	protected IInstallableUnit mockIU(String id, Version version, boolean fragment) {
		IInstallableUnit result = createMock(IInstallableUnit.class);
		expect(result.getId()).andReturn(id).anyTimes();
		if (version == null)
			version = Version.emptyVersion;
		expect(result.getVersion()).andReturn(version).anyTimes();
		expect(result.getFilter()).andReturn(null).anyTimes();
		replay(result);
		return result;
	}

	private void verifyResults() {
		ArrayList ius = new ArrayList(publisherResult.getIUs(null, null));
		IInstallableUnit iu;
		for (int i = 0; i < ius.size(); i++) {
			iu = (IInstallableUnit) ius.get(i);
			if (iu.getId().equals(flavorArg + EquinoxLauncherCUAction.ORG_ECLIPSE_EQUINOX_LAUNCHER)) {
				assertTrue(iu instanceof InstallableUnitFragment);
				//verify required capability
				verifyRequiredCapability(((InstallableUnitFragment) iu).getHost(), PublisherHelper.OSGI_BUNDLE_CLASSIFIER, EquinoxLauncherCUAction.ORG_ECLIPSE_EQUINOX_LAUNCHER, VersionRange.emptyRange);
				verifyRequiredCapability(((InstallableUnitFragment) iu).getHost(), PublisherHelper.NAMESPACE_ECLIPSE_TYPE, "bundle", new VersionRange(Version.create("1.0.0"), true, Version.create("2.0.0"), false), 1, 1, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
				assertEquals(2, (((InstallableUnitFragment) iu).getHost().size()));

				Collection<IProvidedCapability> cap = iu.getProvidedCapabilities();
				verifyProvidedCapability(cap, IInstallableUnit.NAMESPACE_IU_ID, flavorArg + "org.eclipse.equinox.launcher", Version.emptyVersion); //$NON-NLS-1$ 
				verifyProvidedCapability(cap, "org.eclipse.equinox.p2.flavor", flavorArg, Version.create("1.0.0")); //$NON-NLS-1$//$NON-NLS-2$ 
				assertTrue(cap.size() == 2);

				Map prop = iu.getProperties();
				assertTrue(prop.get("org.eclipse.equinox.p2.type.fragment").equals("true")); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}
		}
		fail();
	}

	public void setupPublisherResult() {
		publisherResult = new PublisherResult();
		ArrayList iuList = new ArrayList();
		iuList.add(mockIU(a_ID, null, true));
		iuList.add(mockIU(b_ID, null, true));
		iuList.add(mockIU(c_ID, null, false));
		iuList.add(mockIU(d_ID, null, false));
		publisherResult.addIUs(iuList, IPublisherResult.ROOT);
	}

	protected void insertPublisherInfoBehavior() {
		VersionAdvice versionAdvice = new VersionAdvice();
		versionAdvice.setVersion(IInstallableUnit.NAMESPACE_IU_ID, flavorArg + "org.eclipse.equinox.launcher", Version.emptyVersion); //$NON-NLS-1$
		versionAdvice.setVersion(IInstallableUnit.NAMESPACE_IU_ID, "org.eclipse.equinox.launcher", Version.emptyVersion); //$NON-NLS-1$
		versionAdvice.setVersion("org.eclipse.equinox.p2.flavor", flavorArg, Version.create("1.0.0")); //$NON-NLS-1$//$NON-NLS-2$

		ArrayList versionList = new ArrayList();
		versionList.add(versionAdvice);
		expect(publisherInfo.getAdvice(null, true, EquinoxLauncherCUAction.ORG_ECLIPSE_EQUINOX_LAUNCHER, null, IVersionAdvice.class)).andReturn(versionList);
		expect(publisherInfo.getAdvice(configSpec, true, EquinoxLauncherCUAction.ORG_ECLIPSE_EQUINOX_LAUNCHER + "." + configSpec, null, IVersionAdvice.class)).andReturn(versionList); //$NON-NLS-1$
	}
}
