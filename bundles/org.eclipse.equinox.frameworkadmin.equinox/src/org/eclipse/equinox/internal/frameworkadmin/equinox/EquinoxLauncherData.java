/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.equinox;

import java.io.File;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;

public class EquinoxLauncherData extends LauncherData {
	File previousLauncher = null;

	public EquinoxLauncherData(String fwName, String fwVersion, String launcherName, String launcherVersion) {
		super(fwName, fwVersion, launcherName, launcherVersion);
	}

	public void setLauncher(File launcherFile) {
		if (previousLauncher == null && launcherFile != null && !launcherFile.equals(getLauncher()))
			previousLauncher = EquinoxManipulatorImpl.getLauncherConfigLocation(this);
		super.setLauncher(launcherFile);
	}

	File getPreviousLauncherIni() {
		return previousLauncher;
	}
}
