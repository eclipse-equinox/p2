/*******************************************************************************
 * Copyright (c) 2012 Wind River and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import java.util.EventObject;

public class DownloadPauseResumeEvent extends EventObject {

	private static final long serialVersionUID = 5782796765127875200L;
	/**
	 * It means downloading job should be paused.
	 */
	public static final int TYPE_PAUSE = 1;
	/**
	 * It means downloading job should be resumed.
	 */
	public static final int TYPE_RESUME = 2;

	private int type;

	public DownloadPauseResumeEvent(int type) {
		super(type);
		this.type = type;
	}

	public int getType() {
		return type;
	}

}
