/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository.io;

import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.core.Version;

public interface XMLConstants extends org.eclipse.equinox.internal.p2.persistence.XMLConstants {

	// Constants defining the structure of the XML for metadata objects

	// A format version number for metadata XML.
	public static final Version CURRENT_VERSION = new Version(0, 0, 1);
	public static final VersionRange XML_TOLERANCE = new VersionRange(CURRENT_VERSION, true, new Version(2, 0, 0), false);

	// Constants for processing Instructions
	public static final String PI_METADATA_TARGET = "metadata"; //$NON-NLS-1$

	// Constants for metadata elements
	public static final String INSTALLABLE_UNITS_ELEMENT = "units"; //$NON-NLS-1$
	public static final String INSTALLABLE_UNIT_ELEMENT = "unit"; //$NON-NLS-1$

	//Constants for repository references
	public static final String REPOSITORY_REFERENCES_ELEMENT = "references"; //$NON-NLS-1$
	public static final String REPOSITORY_REFERENCE_ELEMENT = "repository"; //$NON-NLS-1$
	public static final String OPTIONS_ATTRIBUTE = "options"; //$NON-NLS-1$

	// Constants for sub-elements of an installable unit element
	public static final String ARTIFACT_KEYS_ELEMENT = "artifacts"; //$NON-NLS-1$
	public static final String ARTIFACT_KEY_ELEMENT = "artifact"; //$NON-NLS-1$
	public static final String REQUIRED_CAPABILITIES_ELEMENT = "requires"; //$NON-NLS-1$
	public static final String HOST_REQUIRED_CAPABILITIES_ELEMENT = "hostRequirements"; //$NON-NLS-1$
	public static final String REQUIRED_CAPABILITY_ELEMENT = "required"; //$NON-NLS-1$
	public static final String PROVIDED_CAPABILITIES_ELEMENT = "provides"; //$NON-NLS-1$
	public static final String PROVIDED_CAPABILITY_ELEMENT = "provided"; //$NON-NLS-1$
	public static final String TOUCHPOINT_TYPE_ELEMENT = "touchpoint"; //$NON-NLS-1$
	public static final String TOUCHPOINT_DATA_ELEMENT = "touchpointData"; //$NON-NLS-1$
	public static final String IU_FILTER_ELEMENT = "filter"; //$NON-NLS-1$
	public static final String UPDATE_DESCRIPTOR_ELEMENT = "update"; //$NON-NLS-1$

	public static final String APPLICABILITY_SCOPE = "patchScope"; //$NON-NLS-1$
	public static final String APPLY_ON = "scope"; //$NON-NLS-1$
	public static final String REQUIREMENT_CHANGES = "changes"; //$NON-NLS-1$
	public static final String REQUIREMENT_CHANGE = "change"; //$NON-NLS-1$
	public static final String REQUIREMENT_FROM = "from"; //$NON-NLS-1$
	public static final String REQUIREMENT_TO = "to"; //$NON-NLS-1$
	public static final String LIFECYCLE = "lifeCycle"; //$NON-NLS-1$

	// Constants for attributes of an installable unit element
	public static final String SINGLETON_ATTRIBUTE = "singleton"; //$NON-NLS-1$
	public static final String FRAGMENT_ATTRIBUTE = "fragment"; //$NON-NLS-1$

	// Constants for sub-elements of a required capability element
	public static final String CAPABILITY_FILTER_ELEMENT = "filter"; //$NON-NLS-1$
	public static final String CAPABILITY_SELECTORS_ELEMENT = "selectors"; //$NON-NLS-1$
	public static final String CAPABILITY_SELECTOR_ELEMENT = "selector"; //$NON-NLS-1$

	// Constants for attributes of a required capability element
	public static final String CAPABILITY_OPTIONAL_ATTRIBUTE = "optional"; //$NON-NLS-1$
	public static final String CAPABILITY_MULTIPLE_ATTRIBUTE = "multiple"; //$NON-NLS-1$
	public static final String CAPABILITY_GREED_ATTRIBUTE = "greedy"; //$NON-NLS-1$

	// Constants for attributes of an artifact key element
	public static final String ARTIFACT_KEY_CLASSIFIER_ATTRIBUTE = "classifier"; //$NON-NLS-1$

	// Constants for sub-elements of a touchpoint data element
	public static final String TOUCHPOINT_DATA_INSTRUCTIONS_ELEMENT = "instructions"; //$NON-NLS-1$
	public static final String TOUCHPOINT_DATA_INSTRUCTION_ELEMENT = "instruction"; //$NON-NLS-1$

	// Constants for attributes of an a touchpoint data instruction element
	public static final String TOUCHPOINT_DATA_INSTRUCTION_KEY_ATTRIBUTE = "key"; //$NON-NLS-1$
	public static final String TOUCHPOINT_DATA_INSTRUCTION_IMPORT_ATTRIBUTE = "import"; //$NON-NLS-1$

	// Constants for attributes of an update descriptor
	public static final String UPDATE_DESCRIPTOR_SEVERITY = "severity"; //$NON-NLS-1$
}
