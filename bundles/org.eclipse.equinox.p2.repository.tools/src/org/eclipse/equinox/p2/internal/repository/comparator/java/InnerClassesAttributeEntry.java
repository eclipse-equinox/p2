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

public class InnerClassesAttributeEntry extends ClassFileStruct {

	private final int innerClassNameIndex;
	private final int outerClassNameIndex;
	private final int innerNameIndex;
	private char[] innerClassName;
	private char[] outerClassName;
	private char[] innerName;
	private final int accessFlags;

	public InnerClassesAttributeEntry(byte classFileBytes[], ConstantPool constantPool, int offset) throws ClassFormatException {
		this.innerClassNameIndex = u2At(classFileBytes, 0, offset);
		this.outerClassNameIndex = u2At(classFileBytes, 2, offset);
		this.innerNameIndex = u2At(classFileBytes, 4, offset);
		this.accessFlags = u2At(classFileBytes, 6, offset);
		ConstantPoolEntry constantPoolEntry;
		if (this.innerClassNameIndex != 0) {
			constantPoolEntry = constantPool.decodeEntry(this.innerClassNameIndex);
			if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Class) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.innerClassName = constantPoolEntry.getClassInfoName();
		}
		if (this.outerClassNameIndex != 0) {
			constantPoolEntry = constantPool.decodeEntry(this.outerClassNameIndex);
			if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Class) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.outerClassName = constantPoolEntry.getClassInfoName();
		}
		if (this.innerNameIndex != 0) {
			constantPoolEntry = constantPool.decodeEntry(this.innerNameIndex);
			if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Utf8) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.innerName = constantPoolEntry.getUtf8Value();
		}
	}

	/*
	 * @see IInnerClassesAttributeEntry#getAccessFlags()
	 */
	public int getAccessFlags() {
		return this.accessFlags;
	}

	/*
	 * @see IInnerClassesAttributeEntry#getInnerClassName()
	 */
	public char[] getInnerClassName() {
		return this.innerClassName;
	}

	/*
	 * @see IInnerClassesAttributeEntry#getInnerClassNameIndex()
	 */
	public int getInnerClassNameIndex() {
		return this.innerClassNameIndex;
	}

	/*
	 * @see IInnerClassesAttributeEntry#getInnerName()
	 */
	public char[] getInnerName() {
		return this.innerName;
	}

	/*
	 * @see IInnerClassesAttributeEntry#getInnerNameIndex()
	 */
	public int getInnerNameIndex() {
		return this.innerNameIndex;
	}

	/*
	 * @see IInnerClassesAttributeEntry#getOuterClassName()
	 */
	public char[] getOuterClassName() {
		return this.outerClassName;
	}

	/*
	 * @see IInnerClassesAttributeEntry#getOuterClassNameIndex()
	 */
	public int getOuterClassNameIndex() {
		return this.outerClassNameIndex;
	}
}
