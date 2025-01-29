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

public class AnnotationComponentValue extends ClassFileStruct {
	/**
	 * Tag value for a constant of type <code>byte</code>
	 * @since 3.1
	 */
	public static final int BYTE_TAG = 'B';
	/**
	 * Tag value for a constant of type <code>char</code>
	 * @since 3.1
	 */
	public static final int CHAR_TAG = 'C';
	/**
	 * Tag value for a constant of type <code>double</code>
	 * @since 3.1
	 */
	public static final int DOUBLE_TAG = 'D';
	/**
	 * Tag value for a constant of type <code>float</code>
	 * @since 3.1
	 */
	public static final int FLOAT_TAG = 'F';
	/**
	 * Tag value for a constant of type <code>int</code>
	 * @since 3.1
	 */
	public static final int INTEGER_TAG = 'I';
	/**
	 * Tag value for a constant of type <code>long</code>
	 * @since 3.1
	 */
	public static final int LONG_TAG = 'J';
	/**
	 * Tag value for a constant of type <code>short</code>
	 * @since 3.1
	 */
	public static final int SHORT_TAG = 'S';
	/**
	 * Tag value for a constant of type <code>boolean</code>
	 * @since 3.1
	 */
	public static final int BOOLEAN_TAG = 'Z';
	/**
	 * Tag value for a constant of type <code>java.lang.String</code>
	 * @since 3.1
	 */
	public static final int STRING_TAG = 's';
	/**
	 * Tag value for a value that represents an enum constant
	 * @since 3.1
	 */
	public static final int ENUM_TAG = 'e';
	/**
	 * Tag value for a value that represents a class
	 * @since 3.1
	 */
	public static final int CLASS_TAG = 'c';
	/**
	 * Tag value for a value that represents an annotation
	 * @since 3.1
	 */
	public static final int ANNOTATION_TAG = '@';
	/**
	 * Tag value for a value that represents an array
	 * @since 3.1
	 */
	public static final int ARRAY_TAG = '[';

	private static final AnnotationComponentValue[] NO_VALUES = new AnnotationComponentValue[0];

	private AnnotationComponentValue[] annotationComponentValues;
	private Annotation annotationValue;
	private ConstantPoolEntry classInfo;
	private int classFileInfoIndex;
	private ConstantPoolEntry constantValue;
	private int constantValueIndex;
	private int enumConstantTypeNameIndex;
	private int enumConstantNameIndex;
	private char[] enumConstantTypeName;
	private char[] enumConstantName;

	private int readOffset;
	private final int tag;
	private int valuesNumber;

	public AnnotationComponentValue(byte[] classFileBytes, ConstantPool constantPool, int offset) throws ClassFormatException {
		this.classFileInfoIndex = -1;
		this.constantValueIndex = -1;
		this.enumConstantTypeNameIndex = -1;
		this.enumConstantNameIndex = -1;
		final int t = u1At(classFileBytes, 0, offset);
		this.tag = t;
		this.readOffset = 1;
		switch (t) {
			case 'B' :
			case 'C' :
			case 'D' :
			case 'F' :
			case 'I' :
			case 'J' :
			case 'S' :
			case 'Z' :
			case 's' :
				final int constantIndex = u2At(classFileBytes, this.readOffset, offset);
				this.constantValueIndex = constantIndex;
				if (constantIndex != 0) {
					ConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(constantIndex);
					switch (constantPoolEntry.getKind()) {
						case ConstantPoolConstant.CONSTANT_Long :
						case ConstantPoolConstant.CONSTANT_Float :
						case ConstantPoolConstant.CONSTANT_Double :
						case ConstantPoolConstant.CONSTANT_Integer :
						case ConstantPoolConstant.CONSTANT_Utf8 :
							break;
						default :
							throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					this.constantValue = constantPoolEntry;
				}
				this.readOffset += 2;
				break;
			case 'e' :
				int index = u2At(classFileBytes, this.readOffset, offset);
				this.enumConstantTypeNameIndex = index;
				if (index != 0) {
					ConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Utf8) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					this.enumConstantTypeName = constantPoolEntry.getUtf8Value();
				}
				this.readOffset += 2;
				index = u2At(classFileBytes, this.readOffset, offset);
				this.enumConstantNameIndex = index;
				if (index != 0) {
					ConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Utf8) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					this.enumConstantName = constantPoolEntry.getUtf8Value();
				}
				this.readOffset += 2;
				break;
			case 'c' :
				final int classFileIndex = u2At(classFileBytes, this.readOffset, offset);
				this.classFileInfoIndex = classFileIndex;
				if (classFileIndex != 0) {
					ConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(classFileIndex);
					if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Utf8) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					this.classInfo = constantPoolEntry;
				}
				this.readOffset += 2;
				break;
			case '@' :
				Annotation annotation = new Annotation(classFileBytes, constantPool, this.readOffset + offset);
				this.annotationValue = annotation;
				this.readOffset += annotation.sizeInBytes();
				break;
			case '[' :
				final int numberOfValues = u2At(classFileBytes, this.readOffset, offset);
				this.valuesNumber = numberOfValues;
				this.readOffset += 2;
				if (numberOfValues != 0) {
					this.annotationComponentValues = new AnnotationComponentValue[numberOfValues];
					for (int i = 0; i < numberOfValues; i++) {
						AnnotationComponentValue value = new AnnotationComponentValue(classFileBytes, constantPool, offset + this.readOffset);
						this.annotationComponentValues[i] = value;
						this.readOffset += value.sizeInBytes();
					}
				} else {
					this.annotationComponentValues = NO_VALUES;
				}
				break;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationComponentValue#getAnnotationComponentValues()
	 */
	public AnnotationComponentValue[] getAnnotationComponentValues() {
		return this.annotationComponentValues;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationComponentValue#getAnnotationValue()
	 */
	public Annotation getAnnotationValue() {
		return this.annotationValue;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationComponentValue#getClassInfo()
	 */
	public ConstantPoolEntry getClassInfo() {
		return this.classInfo;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationComponentValue#getClassInfoIndex()
	 */
	public int getClassInfoIndex() {
		return this.classFileInfoIndex;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationComponentValue#getConstantValue()
	 */
	public ConstantPoolEntry getConstantValue() {
		return this.constantValue;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationComponentValue#getConstantValueIndex()
	 */
	public int getConstantValueIndex() {
		return this.constantValueIndex;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationComponentValue#getEnumConstantName()
	 */
	public char[] getEnumConstantName() {
		return this.enumConstantName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationComponentValue#getEnumConstantNameIndex()
	 */
	public int getEnumConstantNameIndex() {
		return this.enumConstantNameIndex;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationComponentValue#getEnumConstantTypeName()
	 */
	public char[] getEnumConstantTypeName() {
		return this.enumConstantTypeName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationComponentValue#getEnumConstantTypeNameIndex()
	 */
	public int getEnumConstantTypeNameIndex() {
		return this.enumConstantTypeNameIndex;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationComponentValue#getTag()
	 */
	public int getTag() {
		return this.tag;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.IAnnotationComponentValue#getValuesNumber()
	 */
	public int getValuesNumber() {
		return this.valuesNumber;
	}

	int sizeInBytes() {
		return this.readOffset;
	}
}
