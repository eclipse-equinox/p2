/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.prov.engine;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.prov.core.helpers.LogHelper;
import org.eclipse.equinox.prov.core.helpers.MultiStatus;
import org.eclipse.equinox.prov.engine.ITouchpoint;
import org.eclipse.equinox.prov.metadata.TouchpointType;
import org.eclipse.osgi.util.NLS;

//TODO This needs to support multiple version of each touchpoint and have a lookup that supports version semantics
public class TouchpointManager implements IRegistryChangeListener {

	private static TouchpointManager instance;

	public static TouchpointManager getInstance() {
		if (instance == null) {
			instance = new TouchpointManager();
		}
		return instance;
	}

	private static final String PT_TOUCHPOINTS = "touchpoints"; //$NON-NLS-1$
	private static final String ELEMENT_TOUCHPOINT = "touchpoint"; //$NON-NLS-1$
	private static final String ELEMENT_TOUCHPOINT_DATA = "data"; //$NON-NLS-1$
	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
	private static final String ATTRIBUTE_TYPE = "type"; //$NON-NLS-1$

	private class TouchpointEntry {

		private IConfigurationElement element;
		private boolean createdExtension;
		private ITouchpoint touchpoint;

		public TouchpointEntry(IConfigurationElement element) {
			super();
			this.element = element;
			this.touchpoint = null;
			this.createdExtension = false;
		}

		public TouchpointEntry(IConfigurationElement element, ITouchpoint touchpoint) {
			super();
			this.element = element;
			this.touchpoint = touchpoint;
			this.createdExtension = (touchpoint != null ? true : false);
		}

		public boolean hasTouchpoint() {
			return (this.touchpoint != null);
		}

		public ITouchpoint getTouchpoint() {
			if (!createdExtension) {
				String id = element.getAttribute(ATTRIBUTE_TYPE);
				try {
					ITouchpoint touchpoint = (ITouchpoint) element.createExecutableExtension(ATTRIBUTE_CLASS);
					if (touchpoint != null) {
						if (!id.equals(touchpoint.getTouchpointType().getId())) {
							reportError(NLS.bind(Messages.TouchpointManager_Touchpoint_Type_Mismatch, id, touchpoint.getTouchpointType().getId()), null);
						}
						this.touchpoint = touchpoint;
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

		public String toString() {
			StringBuffer result = new StringBuffer(element.toString());
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
	private Map touchpointEntries;

	private TouchpointManager() {
		RegistryFactory.getRegistry().addRegistryChangeListener(this, EngineActivator.ID);
	}

	/*
	 * Return the touchpoint which is registered for the given id,
	 * or <code>null</code> if none are registered.
	 */
	public ITouchpoint getTouchpoint(TouchpointType id) {
		if (id == null || CommonDef.EmptyString.equals(id.getId()))
			throw new IllegalArgumentException(Messages.TouchpointManager_Null_Touchpoint_Type_Argument);
		if (touchpointEntries == null) {
			initializeTouchpoints();
		}
		TouchpointEntry entry = (TouchpointEntry) touchpointEntries.get(id.getId());
		return entry == null ? null : entry.getTouchpoint();
	}

	public ITouchpoint[] getAllTouchpoints() {
		if (touchpointEntries == null) {
			initializeTouchpoints();
		}
		Collection adapters = touchpointEntries.values();

		ArrayList touchpoints = new ArrayList(adapters.size());
		for (Iterator iter = adapters.iterator(); iter.hasNext();) {
			TouchpointEntry entry = (TouchpointEntry) iter.next();
			ITouchpoint touchpoint = entry.getTouchpoint();
			if (touchpoint != null) {
				touchpoints.add(touchpoint);
			}
		}
		return (ITouchpoint[]) touchpoints.toArray(new ITouchpoint[touchpoints.size()]);
	}

	public ITouchpoint[] getCreatedTouchpoints() {
		if (touchpointEntries == null)
			return new ITouchpoint[0];
		Collection adapters = touchpointEntries.values();

		ArrayList touchpoints = new ArrayList(adapters.size());
		for (Iterator iter = adapters.iterator(); iter.hasNext();) {
			TouchpointEntry entry = (TouchpointEntry) iter.next();
			if (entry.hasTouchpoint()) {
				ITouchpoint touchpoint = entry.getTouchpoint();
				if (touchpoint != null) {
					touchpoints.add(touchpoint);
				}
			}
		}
		return (ITouchpoint[]) touchpoints.toArray(new ITouchpoint[touchpoints.size()]);
	}

	public IStatus validateTouchpoints(String[] requiredTypes) {
		MultiStatus status = (touchpointEntries == null ? initializeTouchpoints() : new MultiStatus());

		for (int i = 0; i < requiredTypes.length; i++) {
			TouchpointEntry entry = (TouchpointEntry) touchpointEntries.get(requiredTypes[i]);
			if (entry == null) {
				reportError(NLS.bind(Messages.TouchpointManager_Required_Touchpoint_Not_Found, requiredTypes[i]), status);
			}
		}
		return status;
	}

	/*
	 * Construct a map of the extensions that implement the touchpoints extension point.
	 */
	private MultiStatus initializeTouchpoints() {
		MultiStatus status = new MultiStatus();
		IExtensionPoint point = RegistryFactory.getRegistry().getExtensionPoint(EngineActivator.ID, PT_TOUCHPOINTS);
		if (point == null) {
			reportError(NLS.bind(Messages.TouchpointManager_No_Extension_Point, EngineActivator.ID, PT_TOUCHPOINTS), status);
			touchpointEntries = new HashMap(0);
			return status;
		}

		IExtension[] extensions = point.getExtensions();
		touchpointEntries = new HashMap(extensions.length);
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				String elementName = elements[j].getName();
				if (!ELEMENT_TOUCHPOINT.equalsIgnoreCase(elements[j].getName())) {
					if (!ELEMENT_TOUCHPOINT_DATA.equals(elementName)) { // TODO: are 'data' elements still needed?
						reportError(NLS.bind(Messages.TouchpointManager_Incorrectly_Named_Extension, elements[j].getName(), ELEMENT_TOUCHPOINT), status);
					}
					continue;
				}
				String id = elements[j].getAttribute(ATTRIBUTE_TYPE);
				if (id == null) {
					reportError(NLS.bind(Messages.TouchpointManager_Attribute_Not_Specified, ATTRIBUTE_TYPE), status);
					continue;
				}
				if (touchpointEntries.get(id) == null) {
					touchpointEntries.put(id, new TouchpointEntry(elements[j]));
				} else {
					reportError(NLS.bind(Messages.TouchpointManager_Conflicting_Touchpoint_Types, ATTRIBUTE_TYPE, id), status);
				}
			}
		}
		return status;
	}

	private static void reportError(String errorMsg, MultiStatus status) {
		Status errorStatus = new Status(IStatus.ERROR, EngineActivator.ID, 1, errorMsg, null);
		if (status != null) {
			status.add(errorStatus);
		}
		LogHelper.log(errorStatus);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IRegistryChangeListener#registryChanged(org.eclipse.core.runtime.IRegistryChangeEvent)
	 */
	public void registryChanged(IRegistryChangeEvent event) {
		// just flush the cache when something changed.  It will be recomputed on demand.
		touchpointEntries = null;
	}
}
