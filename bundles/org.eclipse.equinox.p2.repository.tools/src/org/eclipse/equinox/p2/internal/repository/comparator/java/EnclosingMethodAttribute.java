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

public class EnclosingMethodAttribute extends ClassFileAttribute {

	private int enclosingClassIndex;
	private char[] enclosingClassName;
	private int methodDescriptorIndex;
	private char[] methodDescriptor;
	private int methodNameIndex;
	private char[] methodName;
	private int methodNameAndTypeIndex;

	EnclosingMethodAttribute(byte[] classFileBytes, ConstantPool constantPool, int offset) throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		int index = u2At(classFileBytes, 6, offset);
		this.enclosingClassIndex = index;
		ConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(index);
		if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Class) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.enclosingClassName = constantPoolEntry.getClassInfoName();
		this.methodNameAndTypeIndex = u2At(classFileBytes, 8, offset);
		if (this.methodNameAndTypeIndex != 0) {
			constantPoolEntry = constantPool.decodeEntry(this.methodNameAndTypeIndex);
			if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_NameAndType) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.methodDescriptorIndex = constantPoolEntry.getNameAndTypeInfoDescriptorIndex();
			this.methodNameIndex = constantPoolEntry.getNameAndTypeInfoNameIndex();
			constantPoolEntry = constantPool.decodeEntry(this.methodDescriptorIndex);
			if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Utf8) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.methodDescriptor = constantPoolEntry.getUtf8Value();
			constantPoolEntry = constantPool.decodeEntry(this.methodNameIndex);
			if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Utf8) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.methodName = constantPoolEntry.getUtf8Value();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IEnclosingMethodAttribute#getEnclosingClass()
	 */
	public char[] getEnclosingClass() {
		return this.enclosingClassName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IEnclosingMethodAttribute#getMethodDeclaringClassDescriptorIndex()
	 */
	public int getEnclosingClassIndex() {
		return this.enclosingClassIndex;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IEnclosingMethodAttribute#getMethodDescriptor()
	 */
	public char[] getMethodDescriptor() {
		return this.methodDescriptor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IEnclosingMethodAttribute#getMethodDescriptorIndex()
	 */
	public int getMethodDescriptorIndex() {
		return this.methodDescriptorIndex;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IEnclosingMethodAttribute#getMethodName()
	 */
	public char[] getMethodName() {
		return this.methodName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IEnclosingMethodAttribute#getMethodNameIndex()
	 */
	public int getMethodNameIndex() {
		return this.methodNameIndex;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IEnclosingMethodAttribute#getMethodNameAndTypeIndex()
	 */
	public int getMethodNameAndTypeIndex() {
		return this.methodNameAndTypeIndex;
	}
}
