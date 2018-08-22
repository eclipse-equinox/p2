/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.p2.internal.repository.comparator.java;

public class ConstantValueAttribute extends ClassFileAttribute {

	private int constantValueIndex;
	private ConstantPoolEntry constantPoolEntry;

	ConstantValueAttribute(byte[] classFileBytes, ConstantPool constantPool, int offset) throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		this.constantValueIndex = u2At(classFileBytes, 6, offset);
		this.constantPoolEntry = constantPool.decodeEntry(this.constantValueIndex);
	}

	/*
	 * @see IConstantValueAttribute#getConstantValue()
	 */
	public ConstantPoolEntry getConstantValue() {
		return this.constantPoolEntry;
	}

	/*
	 * @see IConstantValueAttribute#getConstantValueIndex()
	 */
	public int getConstantValueIndex() {
		return this.constantValueIndex;
	}
}
