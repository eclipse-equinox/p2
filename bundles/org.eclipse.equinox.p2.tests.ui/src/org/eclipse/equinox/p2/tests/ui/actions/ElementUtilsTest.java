/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.tests.ui.actions;

import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;

/**
 * @since 3.5
 *
 */
public class ElementUtilsTest extends ProfileModificationActionTest {

	public void testEmpty() {
		assertEquals(getEmptySelection().length, ElementUtils.elementsToIUs(getEmptySelection()).length);
	}

	public void testInvalid() {
		assertTrue(ElementUtils.elementsToIUs(getInvalidSelection()).length == 0);
	}

	public void testIUs() {
		assertEquals(getTopLevelIUs().length, ElementUtils.elementsToIUs(getTopLevelIUs()).length);
	}

	public void testElements() {
		assertEquals(getTopLevelIUElements().length, ElementUtils.elementsToIUs(getTopLevelIUElements()).length);
	}

	public void testMixedIUsAndNonIUs() {
		assertTrue(getMixedIUsAndNonIUs().length != ElementUtils.elementsToIUs(getMixedIUsAndNonIUs()).length);
	}

	public void testMixedIUsAndElements() {
		assertEquals(getMixedIUsAndElements().length, ElementUtils.elementsToIUs(getMixedIUsAndElements()).length);
	}
}
