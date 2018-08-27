/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import java.util.AbstractList;
import org.easymock.Capture;

/**
 * An object that adapts an EasyMock Capture to a List.
 */
public class CaptureList<E> extends AbstractList<E> {
	private Capture<E> capture;

	public CaptureList(Capture<E> capture) {
		this.capture = capture;
	}

	@Override
	public E get(int arg0) {
		return capture.getValue();
	}

	@Override
	public int size() {
		return capture.hasCaptured() ? 1 : 0;
	}

}
