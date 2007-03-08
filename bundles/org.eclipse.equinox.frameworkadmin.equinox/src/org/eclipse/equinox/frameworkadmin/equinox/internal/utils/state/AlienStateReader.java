/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.frameworkadmin.equinox.internal.utils.state;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.adaptor.LocationManager;
import org.eclipse.equinox.frameworkadmin.equinox.internal.Log;
import org.eclipse.equinox.frameworkadmin.equinox.internal.utils.FileUtils;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.storagemanager.StorageManager;
import org.osgi.service.log.LogService;

public class AlienStateReader {
	private StorageManager storageManager;
	private File configuration;
	private File parentConfiguration;
	private State state;
	private final static boolean DEBUG = false;

	public AlienStateReader(File configuration, File parentConfiguration) {
		this.configuration = configuration;
		//this.configuration = new File(configuration.getParentFile(),"configuration2");
		this.parentConfiguration = parentConfiguration;

	}

	private File copyStateToTempLocation(File[] toCopy) throws IOException {
		File temp = BundleHelper.getDefault().getDataFile("aliens");
		File[] list = temp.listFiles();
		if (list != null)
			for (int i = 0; i < list.length; i++) {
				if (!list[i].delete())
					return null;
			}
		FileUtils.copy(toCopy[0], new File(temp, LocationManager.STATE_FILE));
		FileUtils.copy(toCopy[1], new File(temp, LocationManager.LAZY_FILE));
		return temp;
	}

	//	private File findStateBaseDir() {
	//		//TODO Need to do some cleanup with respect to constants coming from the fwk
	//		storageManager = initStorageManager(new File(configuration, FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME), "none", false); //TODO Here we proabaly want to lock?
	//		File stateFile = null;
	//		File lazyFile = null;
	//		try {
	//			stateFile = storageManager.lookup(LocationManager.STATE_FILE, false);
	//			lazyFile = storageManager.lookup(LocationManager.LAZY_FILE, false);
	//		} catch (IOException ex) {
	//			//TODO I don't think there is much we can do in the this case.
	//		}
	//		if (DEBUG) {
	//			Log.log(LogService.LOG_DEBUG, this, "findStateFiles()", "stateFile=" + stateFile);
	//			Log.log(LogService.LOG_DEBUG, this, "findStateFiles()", "lazyFile=" + lazyFile);
	//		}
	//		if (stateFile == null || lazyFile == null || !stateFile.getParentFile().equals(lazyFile.getParentFile()))
	//			return null;
	//		return stateFile.getParentFile();
	//
	//		//		//if it does not exist, try to read it from the parent
	//		//		if (stateFile == null || !stateFile.isFile()) { // NOTE this check is redundant since it
	//		//			// is done in StateManager, however it
	//		//			// is more convenient to have it here
	//		//			if (parentConfiguration != null) {
	//		//				try {
	//		//					File stateLocationDir = new File(parentConfiguration, FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME);
	//		//					storageManager = initStorageManager(stateLocationDir, "none", true); //$NON-NLS-1$);
	//		//					stateFile = storageManager.lookup(LocationManager.STATE_FILE, false);
	//		//					lazyFile = storageManager.lookup(LocationManager.LAZY_FILE, false);
	//		//				} catch (IOException ex) {
	//		//					Log.log(LogService.LOG_DEBUG, this, "findStateFiles()", "Error reading state file ", ex);
	//		//
	//		//					stateFile = null;
	//		//					lazyFile = null;
	//		//				}
	//		//			}
	//		//		}
	//		//		return new File[] {stateFile, lazyFile};
	//	}

	//	private File findStateFilesCopy() {
	//		File[] toCopy = this.findStateFiles();
	//		File temp = BundleHelper.getDefault().getDataFile("aliens");
	//		System.out.println("temp=" + temp);
	//		System.out.println("toCopy[0]=" + toCopy[0]);
	//		System.out.println("toCopy[1]=" + toCopy[1]);
	//		if (toCopy[0] == null || toCopy[1] == null)
	//			return null;
	//		FileUtils.copy(toCopy[0], new File(temp, LocationManager.STATE_FILE));
	//		FileUtils.copy(toCopy[1], new File(temp, LocationManager.LAZY_FILE));
	//		storageManager.close();
	//		return temp;
	//	}

	private File[] findStateFiles() {
		//TODO Need to do some cleanup with respect to constants coming from the fwk
		storageManager = initStorageManager(new File(configuration, FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME), "java.io", false); //TODO Here we proabaly want to lock?
		//storageManager = initStorageManager(new File(configuration, FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME), "none", false); //TODO Here we proabaly want to lock?
		File stateFile = null;
		File lazyFile = null;
		try {
			stateFile = storageManager.lookup(LocationManager.STATE_FILE, false);
			lazyFile = storageManager.lookup(LocationManager.LAZY_FILE, false);
		} catch (IOException e) {
			//TODO I don't think there is much we can do in the this case.
			e.printStackTrace();
			Log.log(LogService.LOG_WARNING, this, "findStateFiles()", "Fail to readState", e);
		}
		//		if (DEBUG) {
		//			Log.log(LogService.LOG_DEBUG, this, "findStateFiles()", "stateFile=" + stateFile);
		//			Log.log(LogService.LOG_DEBUG, this, "findStateFiles()", "lazyFile=" + lazyFile);
		//		}
		//if it does not exist, try to read it from the parent
		if (stateFile == null || !stateFile.isFile()) { // NOTE this check is redundant since it
			// is done in StateManager, however it
			// is more convenient to have it here
			if (parentConfiguration != null) {
				try {
					File stateLocationDir = new File(parentConfiguration, FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME);
					storageManager = initStorageManager(stateLocationDir, "none", true); //$NON-NLS-1$);
					stateFile = storageManager.lookup(LocationManager.STATE_FILE, false);
					lazyFile = storageManager.lookup(LocationManager.LAZY_FILE, false);
				} catch (IOException ex) {
					Log.log(LogService.LOG_DEBUG, this, "findStateFiles()", "Error reading state file ", ex);

					stateFile = null;
					lazyFile = null;
				}
			}
		}
		return new File[] {stateFile, lazyFile};
	}

	public BundleDescription[] getBundleDescriptions() throws IOException {
		if (readState() == null)
			return new BundleDescription[0];
		return state.getBundles();
	}

	public State readState() throws IOException {
		PlatformAdmin platformAdmin = (PlatformAdmin) BundleHelper.getDefault().acquireService(PlatformAdmin.class.getName());
		if (platformAdmin == null)
			return null;

		try {
			File[] stateFiles = findStateFiles();
			if (stateFiles[0] == null || stateFiles[1] == null)
				return null;
			File targetLocation = copyStateToTempLocation(stateFiles);
			//		System.out.println("targetLocation=" + targetLocation);
			//			try {
			state = platformAdmin.getFactory().readState(targetLocation);
			Resolver resolver = platformAdmin.getResolver();
			state.setResolver(resolver);
			//			} catch (IOException e) {
			//				e.printStackTrace();
			//				Log.log(LogService.LOG_WARNING, this, "getBundleDescriptions()", "Fail to readState", e);
			//			}
		} finally {
			//in any case we need to close the storage manager
			if (storageManager != null)
				storageManager.close();
		}
		return state;
	}

	public State getState() {
		return state;
	}

	private StorageManager initStorageManager(File baseDir, String lockMode, boolean readOnly) {
		if (DEBUG)
			Log.log(LogService.LOG_DEBUG, this, "initStorageManager()", "baseDir=" + baseDir + ";lockMode=" + lockMode + ";readOnly=" + readOnly);
		StorageManager manager = new StorageManager(baseDir, lockMode, readOnly);
		try {
			manager.open(!readOnly);
		} catch (IOException ex) {
			//TODO Need to see what we want to do here
			Log.log(LogService.LOG_ERROR, this, "initStorageManager()", "Error reading framework metadata: ", ex);
			//			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
			//				Debug.println("Error reading framework metadata: " + ex.getMessage()); //$NON-NLS-1$
			//				Debug.printStackTrace(ex);
			//			}
			//			String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_FILEMANAGER_OPEN_ERROR, ex.getMessage());
			//			FrameworkLogEntry logEntry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, message, 0, ex, null);
			//			getFrameworkLog().log(logEntry);
		}
		return manager;
	}
}
