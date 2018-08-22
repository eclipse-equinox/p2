/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.discovery;

import org.eclipse.osgi.util.NLS;

/**
 * @author David Green
 */
class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.discovery.messages"; //$NON-NLS-1$

	public static String Catalog_bundle_references_unknown_category;

	public static String Catalog_duplicate_category_id;

	public static String Catalog_exception_disposing;

	public static String Catalog_Failed_to_discovery_all_Error;

	public static String Catalog_illegal_filter_syntax;

	public static String Catalog_Strategy_failed_Error;

	public static String Catalog_task_discovering_connectors;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}

}
