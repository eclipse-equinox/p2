/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.comparator.java;

public class ClassFileAttribute extends ClassFileStruct {
	public static final ClassFileAttribute[] NO_ATTRIBUTES = new ClassFileAttribute[0];
	private long attributeLength;
	private int attributeNameIndex;
	private char[] attributeName;

	public ClassFileAttribute(byte[] classFileBytes, ConstantPool constantPool, int offset) throws ClassFormatException {
		this.attributeNameIndex = u2At(classFileBytes, 0, offset);
		this.attributeLength = u4At(classFileBytes, 2, offset);
		ConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(this.attributeNameIndex);
		if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Utf8) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.attributeName = constantPoolEntry.getUtf8Value();
	}

	public int getAttributeNameIndex() {
		return this.attributeNameIndex;
	}

	/*
	 * @see IClassFileAttribute#getAttributeName()
	 */
	public char[] getAttributeName() {
		return this.attributeName;
	}

	/*
	 * @see IClassFileAttribute#getAttributeLength()
	 */
	public long getAttributeLength() {
		return this.attributeLength;
	}

}
