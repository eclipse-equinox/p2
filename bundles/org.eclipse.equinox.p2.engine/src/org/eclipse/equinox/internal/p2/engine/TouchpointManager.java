/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.engine.spi.Touchpoint;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.osgi.util.NLS;

//TODO This needs to support multiple version of each touchpoint and have a lookup that supports version semantics
public class TouchpointManager implements IRegistryChangeListener {

	private static final String PT_TOUCHPOINTS = "touchpoints"; //$NON-NLS-1$
	private static final String ELEMENT_TOUCHPOINT = "touchpoint"; //$NON-NLS-1$
	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
	private static final String ATTRIBUTE_TYPE = "type"; //$NON-NLS-1$
	private static final String ATTRIBUTE_VERSION = "version"; //$NON-NLS-1$

	private static class TouchpointEntry {

		private final IConfigurationElement element;
		private boolean createdExtension = false;
		private Touchpoint touchpoint = null;

		public TouchpointEntry(IConfigurationElement element) {
			this.element = element;
		}

		public Touchpoint getTouchpoint() {
			if (!createdExtension) {
				String id = getType();
				try {
					Touchpoint touchpointInstance = (Touchpoint) element.createExecutableExtension(ATTRIBUTE_CLASS);
					if (touchpointInstance != null) {
						this.touchpoint = touchpointInstance;
					} else {
						String errorMsg = NLS.bind(Messages.TouchpointManager_Null_Creating_Touchpoint_Extension, id);
						throw new CoreException(new Status(IStatus.ERROR, EngineActivator.ID, 1, errorMsg, null));
					}
				} catch (CoreException cexcpt) {
					LogHelper.log(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.TouchpointManager_Exception_Creating_Touchpoint_Extension, id), cexcpt));
				} catch (ClassCastException ccexcpt) {
					LogHelper.log(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.TouchpointManager_Exception_Creating_Touchpoint_Extension, id), ccexcpt));
				}
				// Mark as created even in error cases,
				// so exceptions are not logged multiple times
				createdExtension = true;
			}
			return this.touchpoint;
		}

		public Version getVersion() {
			try {
				return Version.create(element.getAttribute(ATTRIBUTE_VERSION));
			} catch (InvalidRegistryObjectException e) {
				return null;
			}
		}

		public String getType() {
			try {
				return element.getAttribute(ATTRIBUTE_TYPE);
			} catch (InvalidRegistryObjectException e) {
				return null;
			}
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder(element.toString());
			if (createdExtension) {
				String touchpointString = touchpoint != null ? touchpoint.toString() : "not created"; //$NON-NLS-1$
				result.append(" => " + touchpointString); //$NON-NLS-1$
			}
			return result.toString();
		}
	}

	// TODO: Do we really want to store the touchpoints? The danger is 
	//	     that if two installations are performed simultaneously, then...
	// TODO: Figure out locking, concurrency requirements for touchpoints.
	private Map<String, TouchpointEntry> touchpointEntries;

	public TouchpointManager() {
		RegistryFactory.getRegistry().addRegistryChangeListener(this, EngineActivator.ID);
	}

	/*
	 * Return the touchpoint which is registered for the given type,
	 * or <code>null</code> if none are registered.
	 */
	public synchronized Touchpoint getTouchpoint(ITouchpointType type) {
		if (type == null)
			throw new IllegalArgumentException(Messages.TouchpointManager_Null_Touchpoint_Type_Argument);
		return getTouchpoint(type.getId(), type.getVersion().toString());
	}

	/*
	 * Return the touchpoint which is registered for the given type and optionally version,
	 * or <code>null</code> if none are registered.
	 */
	public Touchpoint getTouchpoint(String typeId, String versionRange) {
		if (typeId == null || typeId.length() == 0)
			throw new IllegalArgumentException(Messages.TouchpointManager_Null_Touchpoint_Type_Argument);

		TouchpointEntry entry = getTouchpointEntries().get(typeId);
		if (entry == null)
			return null;
		if (versionRange != null) {
			VersionRange range = VersionRange.create(versionRange);
			if (!range.isIncluded(entry.getVersion()))
				return null;
		}

		return entry.getTouchpoint();
	}

	/*
	 * Construct a map of the extensions that implement the touchpoints extension point.
	 */
	private synchronized Map<String, TouchpointEntry> getTouchpointEntries() {
		if (touchpointEntries != null)
			return touchpointEntries;

		IExtensionPoint point = RegistryFactory.getRegistry().getExtensionPoint(EngineActivator.ID, PT_TOUCHPOINTS);
		IExtension[] extensions = point.getExtensions();
		touchpointEntries = new HashMap<>(extensions.length);
		for (IExtension extension : extensions) {
			try {
				IConfigurationElement[] elements = extension.getConfigurationElements();
				for (IConfigurationElement element : elements) {
					String elementName = element.getName();
					if (!ELEMENT_TOUCHPOINT.equalsIgnoreCase(elementName)) {
						reportError(NLS.bind(Messages.TouchpointManager_Incorrectly_Named_Extension, element.getName(), ELEMENT_TOUCHPOINT));
						continue;
					}
					String id = element.getAttribute(ATTRIBUTE_TYPE);
					if (id == null) {
						reportError(NLS.bind(Messages.TouchpointManager_Attribute_Not_Specified, ATTRIBUTE_TYPE));
						continue;
					}
					if (touchpointEntries.get(id) == null) {
						touchpointEntries.put(id, new TouchpointEntry(element));
					} else {
						reportError(NLS.bind(Messages.TouchpointManager_Conflicting_Touchpoint_Types, id));
					}
				}
			}catch (InvalidRegistryObjectException e) {
				//skip this extension
			}
		}
		return touchpointEntries;
	}

	static void reportError(String errorMsg) {
		Status errorStatus = new Status(IStatus.ERROR, EngineActivator.ID, 1, errorMsg, null);
		LogHelper.log(errorStatus);
	}

	@Override
	public synchronized void registryChanged(IRegistryChangeEvent event) {
		// just flush the cache when something changed.  It will be recomputed on demand.
		touchpointEntries = null;
	}
}
