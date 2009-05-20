/*******************************************************************************
 *  Copyright (c) 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.update;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.update.messages"; //$NON-NLS-1$
	public static String empty_feature_site;
	public static String error_saving_config;
	public static String error_reading_config;
	public static String error_parsing_config;

	static {
		// load message values from bundle file and assign to fields below
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
}
