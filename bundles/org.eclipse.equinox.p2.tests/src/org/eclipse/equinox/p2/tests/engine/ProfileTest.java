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
package org.eclipse.equinox.p2.tests.engine;

import junit.framework.TestCase;
import org.eclipse.equinox.p2.engine.Profile;

/**
 * Simple test of the engine API.
 */
public class ProfileTest extends TestCase {
	public ProfileTest(String name) {
		super(name);
	}

	public ProfileTest() {
		super("");
	}

	public void testNullProfile() {
		try {
			new Profile(null);
		} catch (IllegalArgumentException exepcted) {
			return;
		}
		fail();
	}

	public void testEmptyProfile() {
		try {
			new Profile("");
		} catch (IllegalArgumentException exepcted) {
			return;
		}
		fail();
	}
}
