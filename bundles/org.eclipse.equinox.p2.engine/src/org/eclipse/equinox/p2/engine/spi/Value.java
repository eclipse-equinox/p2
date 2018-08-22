/*******************************************************************************
 * Copyright (c) 2012, 2017 Landmark Graphics Corporation and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Landmark Graphics Corporation - initial API and implementation
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.equinox.p2.engine.spi;

/**
 * An object that encapsulates the result of performing a provisioning action.
 * 
 * @see ProvisioningAction#getResult()
 * @since 2.3
 */
public class Value<T> {
	public static final Value<Object> NO_VALUE = new Value<>(null);
	private T value;
	private Class<T> clazz;

	public Value(T val) {
		value = val;
	}

	public T getValue() {
		return value;
	}

	public Class<T> getClazz() {
		return clazz;
	}

}
