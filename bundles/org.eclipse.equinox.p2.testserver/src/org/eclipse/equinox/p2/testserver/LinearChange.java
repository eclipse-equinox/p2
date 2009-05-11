/*******************************************************************************
 * Copyright (c) 2009, Cloudsmith Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.testserver;

import java.util.NoSuchElementException;

public class LinearChange {
	boolean started;
	long startValue;
	long current;
	long step;
	long max;
	long min;
	public static final LinearChange NoChange = new LinearChange(0, 0);

	public LinearChange() {
		started = false;
		startValue = 0;
		current = 0;
		step = 1;
		max = Long.MAX_VALUE;
		min = Long.MIN_VALUE;
	}

	public void reset() {
		started = false;
		current = startValue;
	}

	public LinearChange fork() {
		LinearChange forked = new LinearChange();
		forked.started = started;
		forked.startValue = startValue;
		forked.current = current;
		forked.step = step;
		forked.max = max;
		forked.min = min;
		return forked;
	}

	public LinearChange(long firstValue, long step) {
		this();
		current = startValue = firstValue;
		this.step = step;
	}

	public LinearChange(long firstValue, long step, long max, long min) {
		this(firstValue, step);
		this.max = max;
		this.min = min;
	}

	public long next() {
		if (!hasNext())
			throw new NoSuchElementException();
		if (started)
			current += step;
		started = true;

		return current;
	}

	public boolean hasNext() {
		if (step == 0)
			return false;
		if (step > 0 && current + step > max)
			return false;
		if (step < 0 && current + step < min)
			return false;
		return true;
	}
}
