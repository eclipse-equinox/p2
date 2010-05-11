/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others. All rights reserved. This
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

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ExecutablesDescriptor;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.eclipse.EquinoxExecutableAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;

@SuppressWarnings({"restriction", "unchecked"})
public class EquinoxExecutableActionTest extends ActionTest {

	private static final File MAC_EXEC = new File(TestActivator.getTestDataFolder(), "EquinoxExecutableActionTest/macosx/"); //$NON-NLS-1$
	private static final File LINUX_EXEC = new File(TestActivator.getTestDataFolder(), "EquinoxExecutableActionTest/linux/"); //$NON-NLS-1$
	private static final File WIN_EXEC = new File(TestActivator.getTestDataFolder(), "EquinoxExecutableActionTest/win/"); //$NON-NLS-1$
	private final String EXECUTABLE_NAME = "eclipse"; //$NON-NLS-1$
	private String macConfig = "carbon.macosx.ppc"; //$NON-NLS-1$
	private String macConfigCocoa = "cocoa.macosx.x86"; //$NON-NLS-1$
	private String winConfig = "win32.win32.x86"; //$NON-NLS-1$
	private String linuxConfig = "linux.gtk.x86"; //$NON-NLS-1$
	private ExecutablesDescriptor executablesDescriptor;
	private IArtifactRepository artifactRepository;
	private Version version = Version.create("1.2.3"); //$NON-NLS-1$

	public void setUp() throws Exception {
		setupPublisherInfo();
		setupPublisherResult();
	}

	private void setupArtifactRepository() {
		artifactRepository = new TestArtifactRepository(getAgent());
	}

	public void testMacCarbon() throws Exception {
		testExecutableAction("mac", "macosx", macConfig, MAC_EXEC); //$NON-NLS-1$//$NON-NLS-2$
	}

	public void testMacCocoa() throws Exception {
		testExecutableAction("macCocoa", "macosx", macConfigCocoa, MAC_EXEC); //$NON-NLS-1$//$NON-NLS-2$
	}

	public void testWin() throws Exception {
		testExecutableAction("win", "win32", winConfig, WIN_EXEC); //$NON-NLS-1$//$NON-NLS-2$
	}

	public void testLinux() throws Exception {
		testExecutableAction("linux", "linux", linuxConfig, LINUX_EXEC); //$NON-NLS-1$//$NON-NLS-2$
	}

	private void testExecutableAction(String idBase, String osArg, String config, File exec) {
		executablesDescriptor = ExecutablesDescriptor.createDescriptor(osArg, EXECUTABLE_NAME, exec);
		testAction = new EquinoxExecutableAction(executablesDescriptor, config, idBase, version, flavorArg);
		testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		verifyResults(idBase, config);
		debug("Completed EquinoxExecutableActionTest " + idBase + " test."); //$NON-NLS-1$ //$NON-NLS-2$		
	}

	@SuppressWarnings("hiding")
	private void verifyResults(String idBase, String configSpec) {
		ArrayList iuList = new ArrayList(publisherResult.getIUs(null, IPublisherResult.ROOT));
		verifyExecIU(iuList, idBase, configSpec);
		verifyEclipseIU(iuList, idBase, configSpec);
		verifyCU(iuList, idBase, configSpec);
		assertTrue(iuList.size() == 3);
	}

	@SuppressWarnings("hiding")
	private void verifyCU(ArrayList iuList, String idBase, String configSpec) {
		String[] config = AbstractPublisherAction.parseConfigSpec(configSpec);
		String ws = config[0];
		String os = config[1];
		String arch = config[2];
		for (int i = 0; i < iuList.size(); i++) {
			IInstallableUnit possibleEclipse = (IInstallableUnit) iuList.get(i);
			if (possibleEclipse.getId().equals(flavorArg + idBase + ".executable." + configSpec)) {//$NON-NLS-1$ 
				IInstallableUnitFragment fragment = (IInstallableUnitFragment) iuList.get(i);
				Collection<IProvidedCapability> providedCapability = fragment.getProvidedCapabilities();
				verifyProvidedCapability(providedCapability, IInstallableUnit.NAMESPACE_IU_ID, flavorArg + idBase + ".executable." + configSpec, version); //$NON-NLS-1$ 
				assertTrue(providedCapability.size() == 1);
				Collection<IRequirement> requiredCapability = fragment.getHost();
				verifyRequiredCapability(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, idBase + ".executable." + configSpec, new VersionRange(version, true, version, true)); //$NON-NLS-1$ 
				assertTrue(requiredCapability.size() == 1);
				assertTrue(fragment.getFilter().equals(InstallableUnit.parseFilter("(& (osgi.ws=" + ws + ")(osgi.os=" + os + ")(osgi.arch=" + arch + "))"))); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
				assertTrue(fragment.getProperty("org.eclipse.equinox.p2.type.fragment").equals("true")); //$NON-NLS-1$ //$NON-NLS-2$
				return;//pass
			}
		}
		fail();
	}

	@SuppressWarnings("hiding")
	private void verifyEclipseIU(ArrayList iuList, String idBase, String configSpec) {
		for (int i = 0; i < iuList.size(); i++) {
			IInstallableUnit possibleEclipse = (IInstallableUnit) iuList.get(i);
			if (possibleEclipse.getId().equals((idBase + ".executable." + configSpec + ".eclipse"))) { //$NON-NLS-1$//$NON-NLS-2$
				assertTrue(possibleEclipse.getVersion().equals(version));
				Collection<IProvidedCapability> providedCapability = possibleEclipse.getProvidedCapabilities();
				verifyProvidedCapability(providedCapability, IInstallableUnit.NAMESPACE_IU_ID, idBase + ".executable." + configSpec + ".eclipse", version); //$NON-NLS-1$ //$NON-NLS-2$ 
				assertTrue(providedCapability.size() == 1);
				Collection<IRequirement> req = possibleEclipse.getRequirements();
				assertTrue(req.size() == 0);
				return;//pass
			}
		}
		fail();
	}

	@SuppressWarnings("hiding")
	private void verifyExecIU(ArrayList iuList, String idBase, String configSpec) {
		String[] config = AbstractPublisherAction.parseConfigSpec(configSpec);
		String ws = config[0];
		String os = config[1];
		String arch = config[2];
		for (int i = 0; i < iuList.size(); i++) {
			IInstallableUnit possibleExec = (IInstallableUnit) iuList.get(i);
			if (possibleExec.getId().equals(idBase + ".executable." + configSpec)) { //$NON-NLS-1$
				//keep checking
				assertTrue(possibleExec.getFilter().equals(InstallableUnit.parseFilter("(& (osgi.ws=" + ws + ")(osgi.os=" + os + ")(osgi.arch=" + arch + "))"))); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
				IArtifactKey eKey = possibleExec.getArtifacts().iterator().next();
				assertTrue(eKey.getClassifier().equals("binary")); //$NON-NLS-1$
				assertTrue(eKey.getId().equals(idBase + ".executable." + configSpec)); //$NON-NLS-1$
				assertTrue(eKey.getVersion().equals(version));
				Collection<IProvidedCapability> providedCapabilities = possibleExec.getProvidedCapabilities();
				verifyProvidedCapability(providedCapabilities, IInstallableUnit.NAMESPACE_IU_ID, idBase + ".executable." + configSpec, version); //$NON-NLS-1$ 
				verifyProvidedCapability(providedCapabilities, flavorArg + idBase, idBase + ".executable", version); //$NON-NLS-1$
				assertTrue(providedCapabilities.size() == 2);

				Collection<IRequirement> requiredCapability = possibleExec.getRequirements();
				verifyRequiredCapability(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, "org.eclipse.equinox.launcher." + (idBase.equals("mac") || idBase.equals("macCocoa") ? configSpec.substring(0, configSpec.lastIndexOf(".")) : configSpec), VersionRange.emptyRange); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
				assertTrue(requiredCapability.size() == 1);
				return;//pass
			}
		}
		fail();
	}

	protected void insertPublisherInfoBehavior() {
		setupArtifactRepository();
		expect(publisherInfo.getArtifactRepository()).andReturn(artifactRepository).anyTimes();
		expect(publisherInfo.getArtifactOptions()).andReturn(IPublisherInfo.A_PUBLISH).anyTimes();
		expect(publisherInfo.getAdvice((String) anyObject(), anyBoolean(), (String) anyObject(), (Version) anyObject(), (Class) anyObject())).andReturn(Collections.emptyList());
	}
}
