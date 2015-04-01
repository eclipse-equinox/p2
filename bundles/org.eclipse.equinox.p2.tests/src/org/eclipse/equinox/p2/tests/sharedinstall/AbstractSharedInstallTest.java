/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *     Ericsson AB - initial API and implementation
 *     Red Hat, Inc. - fragments support
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.sharedinstall;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.tests.reconciler.dropins.AbstractReconcilerTest;

public abstract class AbstractSharedInstallTest extends AbstractReconcilerTest {
	public static final boolean WINDOWS = java.io.File.separatorChar == '\\';
	protected static File readOnlyBase;
	protected static File userBase;
	protected static String profileId;
	public boolean runningWithReconciler = true;

	public File getUserBundleInfo() {
		return new File(userBase, "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
	}

	protected String getTestRepo() {
		return getTestData("repo for shared install tests", "testData/sharedInstall/repo").toURI().toString();
	}

	protected File getUserBundleInfoTimestamp() {
		return new File(userBase, "configuration/org.eclipse.equinox.simpleconfigurator/.baseBundlesInfoTimestamp");
	}

	protected File getUserProfileRegistryFolder() {
		return new File(userBase, "p2/org.eclipse.equinox.p2.engine/profileRegistry/");
	}

	protected File getUserProfileFolder() {
		return new File(getUserProfileRegistryFolder(), profileId + ".profile");
	}

	protected File getBaseProfileRegistryFolder() {
		return new File(output, getRootFolder() + "p2/org.eclipse.equinox.p2.engine/profileRegistry/");
	}

	protected long[] getProfileTimestampsFromUser() {
		return new SimpleProfileRegistry(getAgent(), getUserProfileRegistryFolder()).listProfileTimestamps(profileId);
	}

	protected long getMostRecentProfileTimestamp(File profileFolder) {
		long[] ts = new SimpleProfileRegistry(getAgent(), profileFolder).listProfileTimestamps(profileId);
		return ts[ts.length - 1];
	}

	protected long getMostRecentProfileTimestampFromBase() {
		return getMostRecentProfileTimestamp(getBaseProfileRegistryFolder());
	}

	protected void assertProfileStatePropertiesHasValue(File profileFolder, String value) {
		try {
			Properties p = loadProperties(new File(profileFolder, "state.properties"));
			Collection<Object> values = p.values();
			for (Object v : values) {
				if (((String) v).contains(value)) {
					return;
				}
			}
			fail("Value: " + value + " not found.");
		} catch (IOException e) {
			fail("exception while loading profile state properties in " + profileFolder.getAbsolutePath());
		}

	}

	protected File getConfigIniTimestamp() {
		return new File(userBase, "configuration/.baseConfigIniTimestamp");
	}

	protected void assertProfileStatePropertiesHasKey(File profileFolder, String key) {
		try {
			Properties p = loadProperties(new File(profileFolder, "state.properties"));
			Set<Object> keys = p.keySet();
			for (Object k : keys) {
				if (((String) k).contains(key)) {
					return;
				}
			}
			fail("Key: " + key + " not found.");
		} catch (IOException e) {
			fail("exception while loading profile state properties in " + profileFolder.getAbsolutePath());
		}

	}

	protected void installFeature1AndVerifierInUser() {
		//TODO Install something into eclipse - make sure that this can be done in an automated setup
		runEclipse("Installing in user", output, new String[] {"-configuration", userBase.getAbsolutePath() + java.io.File.separatorChar + "configuration", "-application", "org.eclipse.equinox.p2.director", "-installIU", "p2TestFeature1.feature.group,Verifier.feature.group", "-repository", getTestRepo()});
	}

	@Override
	protected void tearDown() throws Exception {
		if (readOnlyBase != null && readOnlyBase.exists()) {
			setReadOnly(readOnlyBase, false);
		}
		delete(readOnlyBase);
		delete(userBase);
		super.tearDown();
	}

	protected void installFeature1InUser() {
		runEclipse("user2", output, new String[] {"-configuration", userBase.getAbsolutePath() + java.io.File.separatorChar + "configuration", "-application", "org.eclipse.equinox.p2.director", "-installIU", "p2TestFeature1.feature.group", "-repository", getTestRepo()});
	}

	protected void installFeature1InUserWithoutSpecifyingConfiguration() {
		runEclipse("user2", output, new String[] {"-application", "org.eclipse.equinox.p2.director", "-installIU", "p2TestFeature1.feature.group", "-repository", getTestRepo()});
	}

	protected void installFeature2InUser() {
		runEclipse("user2", output, new String[] {"-configuration", userBase.getAbsolutePath() + java.io.File.separatorChar + "configuration", "-application", "org.eclipse.equinox.p2.director", "-installIU", "p2TestFeature2.feature.group", "-repository", getTestRepo()});
	}

	protected void installVerifierInBase() {
		setReadOnly(readOnlyBase, false);
		runEclipse("Running eclipse", output, new String[] {"-application", "org.eclipse.equinox.p2.director", "-installIU", "Verifier.feature.group", "-repository", getTestRepo()});
		setReadOnly(readOnlyBase, true);
	}

	protected void installVerifierAndFeature1InBase() {
		setReadOnly(readOnlyBase, false);
		runEclipse("Running eclipse", output, new String[] {"-application", "org.eclipse.equinox.p2.director", "-installIU", "Verifier.feature.group,p2TestFeature1.feature.group", "-repository", getTestRepo()});
		setReadOnly(readOnlyBase, true);
	}

	protected void uninstallFeature1InBase() {
		setReadOnly(readOnlyBase, false);
		runEclipse("Running eclipse", output, new String[] {"-application", "org.eclipse.equinox.p2.director", "-uninstallIU", "p2TestFeature1.feature.group", "-repository", getTestRepo()});
		setReadOnly(readOnlyBase, true);
	}

	protected void installFeature2InBase() {
		setReadOnly(readOnlyBase, false);
		runEclipse("Running eclipse", output, new String[] {"-application", "org.eclipse.equinox.p2.director", "-installIU", "p2TestFeature2.feature.group", "-repository", getTestRepo()});
		setReadOnly(readOnlyBase, true);
	}

	protected boolean isInUserBundlesInfo(String bundleId) {
		try {
			return isInBundlesInfo(getUserBundlesInfo(), bundleId, null, null);
		} catch (IOException e) {
			fail("Problem reading bundles.info");
		}
		//should never be reached
		return false;
	}

	protected File getUserBundlesInfo() {
		return new File(userBase, "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
	}

	protected void startEclipseAsUser() {
		runEclipse("Running eclipse", output, new String[] {"-configuration", userBase.getAbsolutePath() + java.io.File.separatorChar + "configuration", "-application", "org.eclipse.equinox.p2.director", "-listInstalledRoots"});
	}

	protected void executeVerifierWithoutSpecifyingConfiguration(Properties verificationProperties) {
		realExecuteVerifier(verificationProperties, false);
	}

	protected void realExecuteVerifier(Properties verificationProperties, boolean withConfigFlag) {
		File verifierConfig = new File(getTempFolder(), "verification.properties");
		try {
			writeProperties(verifierConfig, verificationProperties);
		} catch (IOException e) {
			fail("Failing to write out properties to configure verifier", e);
		}

		String[] args = null;
		if (withConfigFlag)
			args = new String[] {"-configuration", userBase.getAbsolutePath() + java.io.File.separatorChar + "configuration", "-application", "org.eclipse.equinox.p2.tests.verifier.application", "-verifier.properties", verifierConfig.getAbsolutePath(), "-consoleLog"};
		else
			args = new String[] {"-application", "org.eclipse.equinox.p2.tests.verifier.application", "-verifier.properties", verifierConfig.getAbsolutePath(), "-consoleLog"};

		assertEquals(0, runEclipse("Running verifier", output, args));
	}

	protected void executeVerifier(Properties verificationProperties) {
		realExecuteVerifier(verificationProperties, true);
	}

	public static void reallyReadOnly(File folder) {
		if (!Platform.getOS().equals(Platform.OS_WIN32))
			return;

		try {
			Path path = folder.toPath();
			AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class);
			AclEntry newEntry = AclEntry.newBuilder().setFlags(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT).setPermissions(AclEntryPermission.WRITE_DATA, AclEntryPermission.APPEND_DATA, AclEntryPermission.WRITE_NAMED_ATTRS, AclEntryPermission.WRITE_ATTRIBUTES).setPrincipal(view.getOwner()).setType(AclEntryType.DENY).build();

			List<AclEntry> acl = view.getAcl();
			acl.add(0, newEntry); // insert before any DENY entries
			view.setAcl(acl);
		} catch (IOException e) {
			fail("can't mark the folder " + folder + " read-only.");
		}
	}

	public static void removeReallyReadOnly(File folder) {
		if (!Platform.getOS().equals(Platform.OS_WIN32))
			return;

		try {
			Path path = folder.toPath();
			AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class);

			List<AclEntry> acl = view.getAcl();
			acl.remove(0);
			view.setAcl(acl);
		} catch (IOException e) {
			fail("oh shit");
		}
	}

	public static Properties loadProperties(File inputFile) throws FileNotFoundException, IOException {
		Properties props = new Properties();
		InputStream is = null;
		try {
			is = new FileInputStream(inputFile);
			props.load(is);
		} finally {
			if (is != null)
				is.close();
			is = null;
		}
		return props;
	}

	public static void setupReadOnlyInstall() {
		readOnlyBase = new File(output, getRootFolder());
		readOnlyBase.mkdirs();
		assertTrue(readOnlyBase.canWrite());
		setReadOnly(readOnlyBase, true);
		userBase = new File(output, "user");
		userBase.mkdir();
		String[] files = new File(readOnlyBase, "p2/org.eclipse.equinox.p2.engine/profileRegistry/").list();
		if (files.length > 1 || files.length == 0)
			fail("The profile for the read only install located at: " + output + "could not be determined");
		else
			profileId = files[0].substring(0, files[0].indexOf('.'));
	}

	public static void setReadOnly(File target, boolean readOnly) {
		if (WINDOWS) {
			String targetPath = target.getAbsolutePath();
			String[] command = new String[] {"attrib", readOnly ? "+r" : "-r", targetPath, "/s", "/d"};
			run("setReadOnly " + readOnly + " failed on" + target.getAbsolutePath(), command);
			if (target.isDirectory()) {
				targetPath += "\\*.*";
				command = new String[] {"attrib", readOnly ? "+r" : "-r", targetPath, "/s", "/d"};
				run("setReadOnly " + readOnly + " failed on" + target.getAbsolutePath(), command);
			}
		} else {
			String[] command = new String[] {"chmod", "-R", readOnly ? "a-w" : "a+w", target.getAbsolutePath()};
			run("setReadOnly " + readOnly + " failed on" + target.getAbsolutePath(), command);
		}
	}

	public AbstractSharedInstallTest(String name) {
		super(name);
	}

	public void replaceDotEclipseProductFile(File base, String id, String version) {
		File eclipseProductFile = new File(base, ".eclipseproduct");
		eclipseProductFile.delete();

		Properties newProps = new Properties();
		newProps.put("id", id);
		newProps.put("version", version);
		OutputStream os = null;
		try {
			try {
				os = new FileOutputStream(eclipseProductFile);
				newProps.save(os, "file generated for tests " + getName());
			} finally {
				if (os != null)
					os.close();
			}
		} catch (IOException e) {
			fail("Failing setting up the .eclipseproduct file at:" + eclipseProductFile.getAbsolutePath());
		}
	}

}
