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

public class InnerClassesAttribute extends ClassFileAttribute {
	private static final InnerClassesAttributeEntry[] NO_ENTRIES = new InnerClassesAttributeEntry[0];

	private int numberOfClasses;
	private InnerClassesAttributeEntry[] entries;

	/**
	 * Constructor for InnerClassesAttribute.
	 */
	public InnerClassesAttribute(byte[] classFileBytes, ConstantPool constantPool, int offset) throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		this.numberOfClasses = u2At(classFileBytes, 6, offset);
		final int length = this.numberOfClasses;
		if (length != 0) {
			int readOffset = 8;
			this.entries = new InnerClassesAttributeEntry[length];
			for (int i = 0; i < length; i++) {
				this.entries[i] = new InnerClassesAttributeEntry(classFileBytes, constantPool, offset + readOffset);
				readOffset += 8;
			}
		} else {
			this.entries = NO_ENTRIES;
		}
	}

	/*
	 * @see IInnerClassesAttribute#getInnerClassAttributesEntries()
	 */
	public InnerClassesAttributeEntry[] getInnerClassAttributesEntries() {
		return this.entries;
	}

	/*
	 * @see IInnerClassesAttribute#getNumberOfClasses()
	 */
	public int getNumberOfClasses() {
		return this.numberOfClasses;
	}

}
