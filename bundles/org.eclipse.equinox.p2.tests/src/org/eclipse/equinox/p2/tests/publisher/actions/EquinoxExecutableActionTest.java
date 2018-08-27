/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ExecutablesDescriptor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.EquinoxExecutableAction;
import org.eclipse.equinox.p2.publisher.eclipse.IBrandingAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;

public class EquinoxExecutableActionTest extends ActionTest {

	private static final File MAC_EXEC = new File(TestActivator.getTestDataFolder(), "EquinoxExecutableActionTest/macosx/"); //$NON-NLS-1$
	private static final File LINUX_EXEC = new File(TestActivator.getTestDataFolder(), "EquinoxExecutableActionTest/linux/"); //$NON-NLS-1$
	private static final File WIN_EXEC = new File(TestActivator.getTestDataFolder(), "EquinoxExecutableActionTest/win/"); //$NON-NLS-1$
	private final String EXECUTABLE_NAME = "LauncherName"; //$NON-NLS-1$
	private Collection<IBrandingAdvice> brandingAdvice = new LinkedList<>();
	private String macConfigCocoa = "cocoa.macosx.x86"; //$NON-NLS-1$
	private String winConfig = "win32.win32.x86"; //$NON-NLS-1$
	private String linuxConfig = "linux.gtk.x86"; //$NON-NLS-1$
	private ExecutablesDescriptor executablesDescriptor;
	private IArtifactRepository artifactRepository;
	private Version version = Version.create("1.2.3"); //$NON-NLS-1$
	private String id;
	private String[] expectedExecutablesContents;

	@Override
	public void setUp() throws Exception {
		setupPublisherInfo();
		setupPublisherResult();
	}

	private void setupArtifactRepository() {
		artifactRepository = new TestArtifactRepository(getAgent());
	}

	public void testMacCocoa() throws Exception {
		File icon = File.createTempFile(EXECUTABLE_NAME, ".icns");
		FileUtils.copyStream(new FileInputStream(new File(MAC_EXEC, "eclipse.app/Contents/Resources/eclipse.icns")), true, new FileOutputStream(icon), true);

		expectedExecutablesContents = new String[] {"Info.plist", "MacOS/" + EXECUTABLE_NAME, "MacOS/" + EXECUTABLE_NAME + ".ini", "Resources/" + icon.getName()};
		testExecutableAction("macCocoa", "macosx", macConfigCocoa, MAC_EXEC, icon); //$NON-NLS-1$//$NON-NLS-2$

		// cleanup
		icon.delete();
	}

	public void testWin() throws Exception {
		File icon = File.createTempFile(EXECUTABLE_NAME, ".ico");
		FileUtils.copyStream(new FileInputStream(new File(WIN_EXEC, "eclipse.ico")), true, new FileOutputStream(icon), true);

		// FIXME: is there any way to test that the .ico has been replaced?
		expectedExecutablesContents = new String[] {EXECUTABLE_NAME + ".exe"};
		testExecutableAction("win", "win32", winConfig, WIN_EXEC, icon); //$NON-NLS-1$//$NON-NLS-2$

		// cleanup
		icon.delete();
	}

	public void testLinux() throws Exception {
		File icon = File.createTempFile(EXECUTABLE_NAME, ".xpm");
		FileUtils.copyStream(new FileInputStream(new File(LINUX_EXEC, "eclipse.xpm")), true, new FileOutputStream(icon), true);

		expectedExecutablesContents = new String[] {EXECUTABLE_NAME, "icon.xpm"};
		testExecutableAction("linux", "linux", linuxConfig, LINUX_EXEC, icon); //$NON-NLS-1$//$NON-NLS-2$

		// cleanup
		icon.delete();
	}

	private void testExecutableAction(String idBase, final String osArg, String config, File exec, File icon) {
		id = idBase;
		setupBrandingAdvice(osArg, configSpec, exec, icon);
		executablesDescriptor = ExecutablesDescriptor.createDescriptor(osArg, "eclipse", exec);
		testAction = new EquinoxExecutableAction(executablesDescriptor, config, idBase, version, flavorArg);
		testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		verifyResults(idBase, config);
		debug("Completed EquinoxExecutableActionTest " + idBase + " test."); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void verifyResults(String idBase, String confSpec) {
		ArrayList<IInstallableUnit> iuList = new ArrayList<>(publisherResult.getIUs(null, IPublisherResult.ROOT));
		verifyEclipseIU(iuList, idBase, confSpec);
		verifyCU(iuList, idBase, confSpec);
		verifyExecIU(iuList, idBase, confSpec);
		assertTrue(iuList.size() == 3);
	}

	private void verifyCU(ArrayList<IInstallableUnit> iuList, String idBase, String confSpec) {
		String[] config = AbstractPublisherAction.parseConfigSpec(confSpec);
		String _ws = config[0];
		String _os = config[1];
		String _arch = config[2];
		for (int i = 0; i < iuList.size(); i++) {
			IInstallableUnit possibleEclipse = iuList.get(i);
			if (possibleEclipse.getId().equals(flavorArg + idBase + ".executable." + confSpec)) {//$NON-NLS-1$
				IInstallableUnitFragment fragment = (IInstallableUnitFragment) iuList.get(i);
				Collection<IProvidedCapability> providedCapability = fragment.getProvidedCapabilities();
				verifyProvidedCapability(providedCapability, IInstallableUnit.NAMESPACE_IU_ID, flavorArg + idBase + ".executable." + confSpec, version); //$NON-NLS-1$
				assertTrue(providedCapability.size() == 1);
				Collection<IRequirement> requiredCapability = fragment.getHost();
				verifyRequirement(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, idBase + ".executable." + confSpec, new VersionRange(version, true, version, true)); //$NON-NLS-1$
				assertTrue(requiredCapability.size() == 1);

				assertTrue(fragment.getFilter().getParameters()[0].toString().indexOf("(osgi.ws=" + _ws + ")") != -1);
				assertTrue(fragment.getFilter().getParameters()[0].toString().indexOf("(osgi.os=" + _os + ")") != -1);
				assertTrue(fragment.getFilter().getParameters()[0].toString().indexOf("(osgi.arch=" + _arch + ")") != -1);
				assertTrue(fragment.getProperty("org.eclipse.equinox.p2.type.fragment").equals("true")); //$NON-NLS-1$ //$NON-NLS-2$
				return;//pass
			}
		}
		fail();
	}

	private void verifyEclipseIU(ArrayList<IInstallableUnit> iuList, String idBase, String confSpec) {
		for (int i = 0; i < iuList.size(); i++) {
			IInstallableUnit possibleEclipse = iuList.get(i);
			if (possibleEclipse.getId().equals((idBase + ".executable." + confSpec + "." + EXECUTABLE_NAME))) { //$NON-NLS-1$//$NON-NLS-2$
				assertTrue(possibleEclipse.getVersion().equals(version));
				Collection<IProvidedCapability> providedCapability = possibleEclipse.getProvidedCapabilities();
				verifyProvidedCapability(providedCapability, IInstallableUnit.NAMESPACE_IU_ID, idBase + ".executable." + confSpec + "." + EXECUTABLE_NAME, version); //$NON-NLS-1$ //$NON-NLS-2$
				assertTrue(providedCapability.size() == 1);
				Collection<IRequirement> req = possibleEclipse.getRequirements();
				assertTrue(req.size() == 0);
				return;//pass
			}
		}
		fail();
	}

	private void verifyExecIU(ArrayList<IInstallableUnit> iuList, String idBase, String confSpec) {
		String[] config = AbstractPublisherAction.parseConfigSpec(confSpec);
		String _ws = config[0];
		String _os = config[1];
		String _arch = config[2];
		for (int i = 0; i < iuList.size(); i++) {
			IInstallableUnit possibleExec = iuList.get(i);
			if (possibleExec.getId().equals(idBase + ".executable." + confSpec)) { //$NON-NLS-1$
				//keep checking
				assertTrue(possibleExec.getFilter().equals(InstallableUnit.parseFilter("(& (osgi.ws=" + _ws + ")(osgi.os=" + _os + ")(osgi.arch=" + _arch + "))"))); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
				IArtifactKey eKey = possibleExec.getArtifacts().iterator().next();
				assertTrue(eKey.getClassifier().equals("binary")); //$NON-NLS-1$
				assertTrue(eKey.getId().equals(idBase + ".executable." + confSpec)); //$NON-NLS-1$
				assertTrue(eKey.getVersion().equals(version));
				Collection<IProvidedCapability> providedCapabilities = possibleExec.getProvidedCapabilities();
				verifyProvidedCapability(providedCapabilities, IInstallableUnit.NAMESPACE_IU_ID, idBase + ".executable." + confSpec, version); //$NON-NLS-1$
				verifyProvidedCapability(providedCapabilities, flavorArg + idBase, idBase + ".executable", version); //$NON-NLS-1$
				assertTrue(providedCapabilities.size() == 2);

				Collection<IRequirement> requiredCapability = possibleExec.getRequirements();
				verifyRequirement(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, "org.eclipse.equinox.launcher." + (idBase.equals("mac") || idBase.equals("macCocoa") ? confSpec.substring(0, confSpec.lastIndexOf(".")) : confSpec), VersionRange.emptyRange); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				assertTrue(requiredCapability.size() == 1);

				try {
					checkExecutableContents(eKey);
				} catch (IOException e) {
					fail();
				}
				return;//pass
			}
		}
		fail();
	}

	private void checkExecutableContents(IArtifactKey key) throws IOException {
		File file = File.createTempFile("exec", ".zip");

		try (FileOutputStream fos = new FileOutputStream(file)) {
			IArtifactDescriptor ad = artifactRepository.createArtifactDescriptor(key);
			IStatus result = artifactRepository.getArtifact(ad, fos, new NullProgressMonitor());
			assertTrue("executable not published?", result.isOK());
		}

		try (ZipFile zip = new ZipFile(file)) {
			for (String path : expectedExecutablesContents) {
				assertNotNull("executable zip missing " + path, zip.getEntry(path));
			}
			if (key.getId().contains("macosx"))
				checkInfoPlist(zip);
		}

		// cleanup
		file.delete();
	}

	/**
	 * If present, check that the Info.plist had its various values
	 * properly rewritten.
	 * @param zip file to check for the Info.plist
	 */
	private void checkInfoPlist(ZipFile zip) {
		ZipEntry candidate = null;
		boolean found = false;
		for (Enumeration<? extends ZipEntry> iter = zip.entries(); !found && iter.hasMoreElements();) {
			candidate = iter.nextElement();
			found = candidate.getName().endsWith("Info.plist");
		}
		assertTrue(found);
		try {
			String contents = readContentsAndClose(zip.getInputStream(candidate));
			assertEquals(id, getPlistStringValue(contents, "CFBundleIdentifier"));
			assertEquals(EXECUTABLE_NAME, getPlistStringValue(contents, "CFBundleExecutable"));
			assertEquals(EXECUTABLE_NAME, getPlistStringValue(contents, "CFBundleName"));
			assertEquals(EXECUTABLE_NAME, getPlistStringValue(contents, "CFBundleDisplayName"));
			assertEquals(version.toString(), getPlistStringValue(contents, "CFBundleVersion"));
		} catch (IOException e) {
			fail();
		}
	}

	private String getPlistStringValue(String contents, String key) {
		Pattern p = Pattern.compile("<key>" + key + "</key>\\s*<string>([^<]*)</string>");
		Matcher m = p.matcher(contents);
		if (m.find()) {
			return m.group(1);
		}
		return null;
	}

	private String readContentsAndClose(InputStream inputStream) throws IOException {
		StringBuilder sb = new StringBuilder();
		try (Reader is = new InputStreamReader(inputStream)) {
			char[] buf = new char[1024];
			int rc;
			while ((rc = is.read(buf)) >= 0) {
				sb.append(buf, 0, rc - 1);
			}
			return sb.toString();
		}
	}

	@Override
	protected void insertPublisherInfoBehavior() {
		setupArtifactRepository();
		expect(publisherInfo.getArtifactRepository()).andReturn(artifactRepository).anyTimes();
		expect(publisherInfo.getArtifactOptions()).andReturn(IPublisherInfo.A_PUBLISH).anyTimes();
		expect(publisherInfo.getAdvice((String) anyObject(), anyBoolean(), (String) anyObject(), (Version) anyObject(), (Class<IBrandingAdvice>) anyObject())).andReturn(brandingAdvice);
	}

	private void setupBrandingAdvice(final String osArg, final String config, final File exec, final File icon) {
		brandingAdvice.add(new IBrandingAdvice() {
			@Override
			public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
				return true;
			}

			@Override
			public String getOS() {
				return osArg;
			}

			@Override
			public String[] getIcons() {
				return icon == null ? null : new String[] {icon.getAbsolutePath()};
			}

			@Override
			public String getExecutableName() {
				return EXECUTABLE_NAME;
			}
		});
	}
}
