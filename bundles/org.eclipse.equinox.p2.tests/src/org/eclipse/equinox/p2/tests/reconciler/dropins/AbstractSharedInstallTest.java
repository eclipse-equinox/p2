/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *     Ericsson AB - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.*;
import java.util.Properties;

public abstract class AbstractSharedInstallTest extends AbstractReconcilerTest {
	static final boolean WINDOWS = java.io.File.separatorChar == '\\';
	protected static File readOnlyBase;
	protected static File userBase;
	protected static String profileId;

	public File getUserBundleInfo() {
		return new File(userBase, "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
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
		readOnlyBase = new File(output, "eclipse");
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

}
