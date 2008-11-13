/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

/**
 * Help context ids for the P2 Automatic Updater.
 * <p>
 * This interface contains constants only; it is not intended to be implemented
 * or extended.
 * </p>
 * @since 3.5
 */

public interface IAutomaticUpdaterHelpContextIds {
	public static final String PREFIX = AutomaticUpdatePlugin.PLUGIN_ID + "."; //$NON-NLS-1$

	public static final String AUTOMATIC_UPDATES_PREFERENCE_PAGE = PREFIX + "automatic_updates_preference_page_context"; //$NON-NLS-1$
}
