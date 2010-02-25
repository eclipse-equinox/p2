/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

/**
 * @since 2.0
 */
public interface IProfileEvent {

	public static final byte ADDED = 0;
	public static final byte REMOVED = 1;
	public static final byte CHANGED = 2;

	public byte getReason();

	public String getProfileId();

}