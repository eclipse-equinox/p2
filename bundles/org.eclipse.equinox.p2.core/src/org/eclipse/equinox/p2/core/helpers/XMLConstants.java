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
package org.eclipse.equinox.p2.core.helpers;

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

	// Constants for the names of common general attributes
	public static final String ID_ATTRIBUTE = "id"; //$NON-NLS-1$
	public static final String TYPE_ATTRIBUTE = "type"; //$NON-NLS-1$
	public static final String NAME_ATTRIBUTE = "name"; //$NON-NLS-1$
	public static final String VERSION_ATTRIBUTE = "version"; //$NON-NLS-1$
	public static final String VERSION_RANGE_ATTRIBUTE = "range"; //$NON-NLS-1$
	public static final String NAMESPACE_ATTRIBUTE = "namespace"; //$NON-NLS-1$
	public static final String CLASSIFIER_ATTRIBUTE = "classifier"; //$NON-NLS-1$
	public static final String DESCRIPTION_ATTRIBUTE = "description"; //$NON-NLS-1$
	public static final String PROVIDER_ATTRIBUTE = "provider"; //$NON-NLS-1$

	// A constant for the name of an attribute of a collection or array element
	// specifying the size or length
	public static final String COLLECTION_SIZE_ATTRIBUTE = "size"; //$NON-NLS-1$

	// A constant for an empty array of attribute names
	public static String[] noAttributes = new String[0];

}
