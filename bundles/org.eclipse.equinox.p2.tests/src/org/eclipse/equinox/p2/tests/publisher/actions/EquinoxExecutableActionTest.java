/*******************************************************************************
 * Copyright (c) 2008, 2024 Code 9 and others.
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
 *   SAP SE - support macOS bundle URL types
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.eclipse.equinox.p2.publisher.eclipse.IMacOsBundleUrlType;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;

public class EquinoxExecutableActionTest extends ActionTest {

	private static final File MAC_EXEC = new File(TestActivator.getTestDataFolder(), "EquinoxExecutableActionTest/macosx/"); //$NON-NLS-1$
	private static final File LINUX_EXEC = new File(TestActivator.getTestDataFolder(), "EquinoxExecutableActionTest/linux/"); //$NON-NLS-1$
	private static final File WIN_EXEC = new File(TestActivator.getTestDataFolder(), "EquinoxExecutableActionTest/win/"); //$NON-NLS-1$
	private final String EXECUTABLE_NAME = "LauncherName"; //$NON-NLS-1$
	private String macConfigCocoa = "cocoa.macosx.x86"; //$NON-NLS-1$
	private String winConfig = "win32.win32.x86_64"; //$NON-NLS-1$
	private String linuxConfig = "linux.gtk.x86_64"; //$NON-NLS-1$
	private ExecutablesDescriptor executablesDescriptor;
	private IArtifactRepository artifactRepository;
	private Version version = Version.create("1.2.3"); //$NON-NLS-1$
	private String id;
	private String[] expectedExecutablesContents;

	@Override
	public void setUp() throws Exception {
		setupPublisherInfo();
		setupPublisherResult();
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
		when(publisherInfo.getArtifactRepository()).thenReturn(artifactRepository);
		when(publisherInfo.getArtifactOptions()).thenReturn(IPublisherInfo.A_PUBLISH);
		when(publisherInfo.getAdvice(anyString(), anyBoolean(), nullable(String.class), nullable(Version.class),
				eq(IBrandingAdvice.class))).then(invocation -> setupBrandingAdvice(osArg, icon));
		executablesDescriptor = ExecutablesDescriptor.createDescriptor(osArg, "eclipse", exec);
		testAction = new EquinoxExecutableAction(executablesDescriptor, config, idBase, version, flavorArg);
		testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		verifyResults(idBase, config);
		debug("Completed EquinoxExecutableActionTest " + idBase + " test."); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void verifyResults(String idBase, String confSpec) {
		ArrayList<IInstallableUnit> iuList = new ArrayList<>(publisherResult.getIUs(null, IPublisherResult.ROOT));
		assertEquals(3, iuList.size());
		verifyEclipseIU(iuList, idBase, confSpec);
		verifyCU(iuList, idBase, confSpec);
		verifyExecIU(iuList, idBase, confSpec);
	}

	private void verifyCU(ArrayList<IInstallableUnit> iuList, String idBase, String confSpec) {
		String[] config = AbstractPublisherAction.parseConfigSpec(confSpec);
		String _ws = config[0];
		String _os = config[1];
		String _arch = config[2];
		for (IInstallableUnit possibleEclipse : iuList) {
			if (possibleEclipse.getId().equals(flavorArg + idBase + ".executable." + confSpec)) {//$NON-NLS-1$
				IInstallableUnitFragment fragment = (IInstallableUnitFragment) possibleEclipse;
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
		for (IInstallableUnit possibleEclipse : iuList) {
			if (possibleEclipse.getId().equals((idBase + ".executable." + confSpec + "." + EXECUTABLE_NAME))) { //$NON-NLS-1$//$NON-NLS-2$
				assertEquals(version, possibleEclipse.getVersion());
				Collection<IProvidedCapability> providedCapability = possibleEclipse.getProvidedCapabilities();
				verifyProvidedCapability(providedCapability, IInstallableUnit.NAMESPACE_IU_ID, idBase + ".executable." + confSpec + "." + EXECUTABLE_NAME, version); //$NON-NLS-1$ //$NON-NLS-2$
				assertEquals(1, providedCapability.size());
				Collection<IRequirement> req = possibleEclipse.getRequirements();
				assertEquals(0, req.size());
				return;//pass
			}
		}
		fail("No executable installable unit.");
	}

	private void verifyExecIU(ArrayList<IInstallableUnit> iuList, String idBase, String confSpec) {
		String[] config = AbstractPublisherAction.parseConfigSpec(confSpec);
		String _ws = config[0];
		String _os = config[1];
		String _arch = config[2];
		for (IInstallableUnit possibleExec : iuList) {
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
				verifyRequirement(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID,
						"org.eclipse.equinox.launcher." + confSpec, VersionRange.emptyRange); //$NON-NLS-1$
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
	private void checkInfoPlist(ZipFile zip) throws IOException {
		var candidate = zip.stream().filter(e -> e.getName().endsWith("Info.plist")).findFirst();
		assertTrue(candidate.isPresent());
		try (InputStream is = zip.getInputStream(candidate.get())) {
			String contents = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			assertEquals(id, getPlistStringValue(contents, "CFBundleIdentifier"));
			assertEquals(EXECUTABLE_NAME, getPlistStringValue(contents, "CFBundleExecutable"));
			assertEquals(EXECUTABLE_NAME, getPlistStringValue(contents, "CFBundleName"));
			assertEquals(EXECUTABLE_NAME, getPlistStringValue(contents, "CFBundleDisplayName"));
			assertEquals(version.toString(), getPlistStringValue(contents, "CFBundleVersion"));
			assertEquals("""
					<dict>
						<key>CFBundleURLName</key>
						<string>Eclipse Command</string>
						<key>CFBundleURLSchemes</key>
						<array>
							<string>eclipse+command</string>
						</array>
					</dict>
					<dict>
						<key>CFBundleURLName</key>
						<string>Vendor Application</string>
						<key>CFBundleURLSchemes</key>
						<array>
							<string>vendor</string>
						</array>
					</dict>""".replaceAll(">\\s+<", "><"), getPlistBundleUrlTypesXmlWithoutWhitespace(contents));
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

	private static final Pattern PLIST_BUNDLE_URL_TYPES_PATTERN = Pattern
			.compile("<key>CFBundleURLTypes</key>\\s*<array>\\s*(.*</dict>.*?)\\s*</array>", Pattern.DOTALL);

	private String getPlistBundleUrlTypesXmlWithoutWhitespace(String contents) {
		Matcher m = PLIST_BUNDLE_URL_TYPES_PATTERN.matcher(contents);
		if (m.find()) {
			return m.group(1).replace("\\n", "").replaceAll(">\\s+<", "><");
		}
		return null;
	}

	private List<IBrandingAdvice> setupBrandingAdvice(final String osArg, final File icon) {
		List<IBrandingAdvice> brandingAdvice = new LinkedList<>();
		brandingAdvice.add(new IBrandingAdvice() {
			@SuppressWarnings("hiding")
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

			@Override
			public List<IMacOsBundleUrlType> getMacOsBundleUrlTypes() {
				IMacOsBundleUrlType scheme1 = mock(IMacOsBundleUrlType.class);
				when(scheme1.getName()).thenReturn("Eclipse Command");
				when(scheme1.getScheme()).thenReturn("eclipse+command");
				IMacOsBundleUrlType scheme2 = mock(IMacOsBundleUrlType.class);
				when(scheme2.getName()).thenReturn("Vendor Application");
				when(scheme2.getScheme()).thenReturn("vendor");
				return List.of(scheme1, scheme2);
			}
		});
		return brandingAdvice;
	}
}
