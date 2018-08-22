/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
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
 *     Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.persistence;

import java.util.List;
import org.eclipse.equinox.p2.metadata.Version;

public interface XMLConstants {

	// Constants used in defining a default processing instruction
	// including a class name and a version of the associated XML
	// for some category of objects.
	//	e.g. <?category class='a.b.c.SomeClass' version='1.2.3'?>
	//
	public static final String PI_CLASS_ATTRIBUTE = "class"; //$NON-NLS-1$
	public static final String PI_VERSION_ATTRIBUTE = "version"; //$NON-NLS-1$

	// Element and attribute names for a standard property collection.
	//	e.g. <properties size='1'>
	//			<property name='some_name' value='some_value'/>
	//		 </properties>
	public static final String PROPERTIES_ELEMENT = "properties"; //$NON-NLS-1$
	public static final String PROPERTY_ELEMENT = "property"; //$NON-NLS-1$
	public static final String PROPERTY_NAME_ATTRIBUTE = "name"; //$NON-NLS-1$
	public static final String PROPERTY_VALUE_ATTRIBUTE = "value"; //$NON-NLS-1$
	public static final String PROPERTY_TYPE_ATTRIBUTE = "type"; //$NON-NLS-1$
	public static final String[] PROPERTY_ATTRIBUTES = new String[] {PROPERTY_NAME_ATTRIBUTE, PROPERTY_VALUE_ATTRIBUTE};
	public static final String[] PROPERTY_OPTIONAL_ATTRIBUTES = new String[] {PROPERTY_TYPE_ATTRIBUTE};
	public static final String PROPERTY_TYPE_LIST = List.class.getSimpleName();
	public static final String PROPERTY_TYPE_STRING = String.class.getSimpleName();
	public static final String PROPERTY_TYPE_INTEGER = Integer.class.getSimpleName();
	public static final String PROPERTY_TYPE_LONG = Long.class.getSimpleName();
	public static final String PROPERTY_TYPE_FLOAT = Float.class.getSimpleName();
	public static final String PROPERTY_TYPE_DOUBLE = Double.class.getSimpleName();
	public static final String PROPERTY_TYPE_BYTE = Byte.class.getSimpleName();
	public static final String PROPERTY_TYPE_SHORT = Short.class.getSimpleName();
	public static final String PROPERTY_TYPE_CHARACTER = Character.class.getSimpleName();
	public static final String PROPERTY_TYPE_BOOLEAN = Boolean.class.getSimpleName();
	public static final String PROPERTY_TYPE_VERSION = Version.class.getSimpleName();

	// Constants for the names of common general attributes
	public static final String ID_ATTRIBUTE = "id"; //$NON-NLS-1$
	public static final String PARENT_ID_ATTRIBUTE = "parentId"; //$NON-NLS-1$
	public static final String TYPE_ATTRIBUTE = "type"; //$NON-NLS-1$
	public static final String NAME_ATTRIBUTE = "name"; //$NON-NLS-1$
	public static final String VERSION_ATTRIBUTE = "version"; //$NON-NLS-1$
	public static final String VERSION_RANGE_ATTRIBUTE = "range"; //$NON-NLS-1$
	public static final String NAMESPACE_ATTRIBUTE = "namespace"; //$NON-NLS-1$
	public static final String MATCH_ATTRIBUTE = "match"; //$NON-NLS-1$
	public static final String MATCH_PARAMETERS_ATTRIBUTE = "matchParameters"; //$NON-NLS-1$
	public static final String MIN_ATTRIBUTE = "min"; //$NON-NLS-1$
	public static final String MAX_ATTRIBUTE = "max"; //$NON-NLS-1$
	public static final String CLASSIFIER_ATTRIBUTE = "classifier"; //$NON-NLS-1$
	public static final String DESCRIPTION_ATTRIBUTE = "description"; //$NON-NLS-1$
	public static final String PROVIDER_ATTRIBUTE = "provider"; //$NON-NLS-1$
	public static final String URL_ATTRIBUTE = "url"; //$NON-NLS-1$
	public static final String URI_ATTRIBUTE = "uri"; //$NON-NLS-1$

	// Constants for the license and copyright elements
	public static final String LICENSES_ELEMENT = "licenses"; //$NON-NLS-1$
	public static final String LICENSE_ELEMENT = "license"; //$NON-NLS-1$
	public static final String COPYRIGHT_ELEMENT = "copyright"; //$NON-NLS-1$

	// A constant for the name of an attribute of a collection or array element
	// specifying the size or length
	public static final String COLLECTION_SIZE_ATTRIBUTE = "size"; //$NON-NLS-1$

	// A constant for an empty array of attribute names
	public static String[] noAttributes = new String[0];

	//Constants for attributes of a composite repository
	public static final String CHILDREN_ELEMENT = "children"; //$NON-NLS-1$
	public static final String CHILD_ELEMENT = "child"; //$NON-NLS-1$
	public static final String LOCATION_ELEMENT = "location"; //$NON-NLS-1$
}
