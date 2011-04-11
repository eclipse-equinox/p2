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

import java.util.Arrays;

public class ClassFileReader extends ClassFileStruct {
	/*
	 * This value should be used to read completely each part of a .class file.
	 */
	public static final int ALL = 0xFFFF;

	/*
	 * This value should be used to read only the constant pool entries of a .class file.
	 */
	public static final int CONSTANT_POOL = 0x0001;

	/*
	 * This value should be used to read the constant pool entries and
	 * the method infos of a .class file.
	 */
	public static final int METHOD_INFOS = 0x0002 + CONSTANT_POOL;

	/*
	 * This value should be used to read the constant pool entries and
	 * the field infos of a .class file.
	 */
	public static final int FIELD_INFOS = 0x0004 + CONSTANT_POOL;

	/*
	 * This value should be used to read the constant pool entries and
	 * the super interface names of a .class file.
	 */
	public static final int SUPER_INTERFACES = 0x0008 + CONSTANT_POOL;

	/*
	 * This value should be used to read the constant pool entries and
	 * the attributes of a .class file.
	 */
	public static final int CLASSFILE_ATTRIBUTES = 0x0010 + CONSTANT_POOL;

	/*
	 * This value should be used to read the method bodies.
	 * It has to be used with METHOD_INFOS.
	 */
	public static final int METHOD_BODIES = 0x0020;

	/*
	 * This value should be used to read the whole contents of the .class file except the
	 * method bodies.
	 */
	public static final int ALL_BUT_METHOD_BODIES = ALL & ~METHOD_BODIES;

	private static final FieldInfo[] NO_FIELD_INFOS = new FieldInfo[0];
	private static final char[][] NO_INTERFACES_NAMES = CharOperation.NO_CHAR_CHAR;
	private static final MethodInfo[] NO_METHOD_INFOS = new MethodInfo[0];
	private int accessFlags;
	private ClassFileAttribute[] attributes;
	private int attributesCount;
	private char[] className;
	private int classNameIndex;

	private ConstantPool constantPool;
	private FieldInfo[] fields;
	private int fieldsCount;
	private InnerClassesAttribute innerClassesAttribute;
	private int[] interfaceIndexes;
	private char[][] interfaceNames;
	private int interfacesCount;
	private int magicNumber;
	private int majorVersion;
	private MethodInfo[] methods;
	private int methodsCount;
	private int minorVersion;
	private SourceFileAttribute sourceFileAttribute;
	private char[] superclassName;
	private int superclassNameIndex;

	/*
	 * Constructor for ClassFileReader.
	 *
	 * @param classFileBytes the raw bytes of the .class file
	 * @param decodingFlags the decoding flags
	 *
	 * @see IClassFileReader#ALL
	 * @see IClassFileReader#CLASSFILE_ATTRIBUTES
	 * @see IClassFileReader#CONSTANT_POOL
	 * @see IClassFileReader#FIELD_INFOS
	 */
	public ClassFileReader(byte[] classFileBytes, int decodingFlags) throws ClassFormatException {

		// This method looks ugly but is actually quite simple, the constantPool is constructed
		// in 3 passes.  All non-primitive constant pool members that usually refer to other members
		// by index are tweaked to have their value in inst vars, this minor cost at read-time makes
		// all subsequent uses of the constant pool element faster.
		int constantPoolCount;
		int[] constantPoolOffsets;
		try {
			this.magicNumber = (int) u4At(classFileBytes, 0, 0);
			if (this.magicNumber != 0xCAFEBABE) {
				throw new ClassFormatException(ClassFormatException.INVALID_MAGIC_NUMBER);
			}

			int readOffset = 10;
			this.minorVersion = u2At(classFileBytes, 4, 0);
			this.majorVersion = u2At(classFileBytes, 6, 0);

			if ((decodingFlags & CONSTANT_POOL) == 0) {
				// no need to go further
				return;
			}

			constantPoolCount = u2At(classFileBytes, 8, 0);
			// Pass #1 - Fill in all primitive constants
			constantPoolOffsets = new int[constantPoolCount];
			for (int i = 1; i < constantPoolCount; i++) {
				int tag = u1At(classFileBytes, readOffset, 0);
				switch (tag) {
					case ConstantPoolConstant.CONSTANT_Utf8 :
						constantPoolOffsets[i] = readOffset;
						readOffset += u2At(classFileBytes, readOffset + 1, 0);
						readOffset += ConstantPoolConstant.CONSTANT_Utf8_SIZE;
						break;
					case ConstantPoolConstant.CONSTANT_Integer :
						constantPoolOffsets[i] = readOffset;
						readOffset += ConstantPoolConstant.CONSTANT_Integer_SIZE;
						break;
					case ConstantPoolConstant.CONSTANT_Float :
						constantPoolOffsets[i] = readOffset;
						readOffset += ConstantPoolConstant.CONSTANT_Float_SIZE;
						break;
					case ConstantPoolConstant.CONSTANT_Long :
						constantPoolOffsets[i] = readOffset;
						readOffset += ConstantPoolConstant.CONSTANT_Long_SIZE;
						i++;
						break;
					case ConstantPoolConstant.CONSTANT_Double :
						constantPoolOffsets[i] = readOffset;
						readOffset += ConstantPoolConstant.CONSTANT_Double_SIZE;
						i++;
						break;
					case ConstantPoolConstant.CONSTANT_Class :
						constantPoolOffsets[i] = readOffset;
						readOffset += ConstantPoolConstant.CONSTANT_Class_SIZE;
						break;
					case ConstantPoolConstant.CONSTANT_String :
						constantPoolOffsets[i] = readOffset;
						readOffset += ConstantPoolConstant.CONSTANT_String_SIZE;
						break;
					case ConstantPoolConstant.CONSTANT_Fieldref :
						constantPoolOffsets[i] = readOffset;
						readOffset += ConstantPoolConstant.CONSTANT_Fieldref_SIZE;
						break;
					case ConstantPoolConstant.CONSTANT_Methodref :
						constantPoolOffsets[i] = readOffset;
						readOffset += ConstantPoolConstant.CONSTANT_Methodref_SIZE;
						break;
					case ConstantPoolConstant.CONSTANT_InterfaceMethodref :
						constantPoolOffsets[i] = readOffset;
						readOffset += ConstantPoolConstant.CONSTANT_InterfaceMethodref_SIZE;
						break;
					case ConstantPoolConstant.CONSTANT_NameAndType :
						constantPoolOffsets[i] = readOffset;
						readOffset += ConstantPoolConstant.CONSTANT_NameAndType_SIZE;
						break;
					default :
						throw new ClassFormatException(ClassFormatException.INVALID_TAG_CONSTANT);
				}
			}

			this.constantPool = new ConstantPool(classFileBytes, constantPoolOffsets);
			// Read and validate access flags
			this.accessFlags = u2At(classFileBytes, readOffset, 0);
			readOffset += 2;

			// Read the classname, use exception handlers to catch bad format
			this.classNameIndex = u2At(classFileBytes, readOffset, 0);
			this.className = getConstantClassNameAt(classFileBytes, constantPoolOffsets, this.classNameIndex);
			readOffset += 2;

			// Read the superclass name, can be zero for java.lang.Object
			this.superclassNameIndex = u2At(classFileBytes, readOffset, 0);
			readOffset += 2;
			// if superclassNameIndex is equals to 0 there is no need to set a value for the
			// field this.superclassName. null is fine.
			if (this.superclassNameIndex != 0) {
				this.superclassName = getConstantClassNameAt(classFileBytes, constantPoolOffsets, this.superclassNameIndex);
			}

			// Read the interfaces, use exception handlers to catch bad format
			this.interfacesCount = u2At(classFileBytes, readOffset, 0);
			readOffset += 2;
			this.interfaceNames = NO_INTERFACES_NAMES;
			this.interfaceIndexes = Utility.EMPTY_INT_ARRAY;
			if (this.interfacesCount != 0) {
				if ((decodingFlags & SUPER_INTERFACES) != CONSTANT_POOL) {
					this.interfaceNames = new char[this.interfacesCount][];
					this.interfaceIndexes = new int[this.interfacesCount];
					for (int i = 0; i < this.interfacesCount; i++) {
						this.interfaceIndexes[i] = u2At(classFileBytes, readOffset, 0);
						this.interfaceNames[i] = getConstantClassNameAt(classFileBytes, constantPoolOffsets, this.interfaceIndexes[i]);
						readOffset += 2;
					}
				} else {
					readOffset += (2 * this.interfacesCount);
				}
			}
			// Read the this.fields, use exception handlers to catch bad format
			this.fieldsCount = u2At(classFileBytes, readOffset, 0);
			readOffset += 2;
			this.fields = NO_FIELD_INFOS;
			if (this.fieldsCount != 0) {
				if ((decodingFlags & FIELD_INFOS) != CONSTANT_POOL) {
					FieldInfo field;
					this.fields = new FieldInfo[this.fieldsCount];
					for (int i = 0; i < this.fieldsCount; i++) {
						field = new FieldInfo(classFileBytes, this.constantPool, readOffset);
						this.fields[i] = field;
						readOffset += field.sizeInBytes();
					}
				} else {
					for (int i = 0; i < this.fieldsCount; i++) {
						int attributeCountForField = u2At(classFileBytes, 6, readOffset);
						readOffset += 8;
						if (attributeCountForField != 0) {
							for (int j = 0; j < attributeCountForField; j++) {
								int attributeLength = (int) u4At(classFileBytes, 2, readOffset);
								readOffset += (6 + attributeLength);
							}
						}
					}
				}
			}
			// Read the this.methods
			this.methodsCount = u2At(classFileBytes, readOffset, 0);
			readOffset += 2;
			this.methods = NO_METHOD_INFOS;
			if (this.methodsCount != 0) {
				if ((decodingFlags & METHOD_INFOS) != CONSTANT_POOL) {
					this.methods = new MethodInfo[this.methodsCount];
					MethodInfo method;
					for (int i = 0; i < this.methodsCount; i++) {
						method = new MethodInfo(classFileBytes, this.constantPool, readOffset, decodingFlags);
						this.methods[i] = method;
						readOffset += method.sizeInBytes();
					}
				} else {
					for (int i = 0; i < this.methodsCount; i++) {
						int attributeCountForMethod = u2At(classFileBytes, 6, readOffset);
						readOffset += 8;
						if (attributeCountForMethod != 0) {
							for (int j = 0; j < attributeCountForMethod; j++) {
								int attributeLength = (int) u4At(classFileBytes, 2, readOffset);
								readOffset += (6 + attributeLength);
							}
						}
					}
				}
			}

			// Read the attributes
			this.attributesCount = u2At(classFileBytes, readOffset, 0);
			readOffset += 2;

			int attributesIndex = 0;
			this.attributes = ClassFileAttribute.NO_ATTRIBUTES;
			if (this.attributesCount != 0) {
				if ((decodingFlags & CLASSFILE_ATTRIBUTES) != CONSTANT_POOL) {
					this.attributes = new ClassFileAttribute[this.attributesCount];
					for (int i = 0; i < this.attributesCount; i++) {
						int utf8Offset = constantPoolOffsets[u2At(classFileBytes, readOffset, 0)];
						char[] attributeName = utf8At(classFileBytes, utf8Offset + 3, 0, u2At(classFileBytes, utf8Offset + 1, 0));
						if (Arrays.equals(attributeName, AttributeNamesConstants.INNER_CLASSES)) {
							this.innerClassesAttribute = new InnerClassesAttribute(classFileBytes, this.constantPool, readOffset);
							this.attributes[attributesIndex++] = this.innerClassesAttribute;
						} else if (Arrays.equals(attributeName, AttributeNamesConstants.SOURCE)) {
							this.sourceFileAttribute = new SourceFileAttribute(classFileBytes, this.constantPool, readOffset);
							this.attributes[attributesIndex++] = this.sourceFileAttribute;
						} else if (Arrays.equals(attributeName, AttributeNamesConstants.ENCLOSING_METHOD)) {
							this.attributes[attributesIndex++] = new EnclosingMethodAttribute(classFileBytes, this.constantPool, readOffset);
						} else if (Arrays.equals(attributeName, AttributeNamesConstants.SIGNATURE)) {
							this.attributes[attributesIndex++] = new SignatureAttribute(classFileBytes, this.constantPool, readOffset);
						} else if (Arrays.equals(attributeName, AttributeNamesConstants.RUNTIME_VISIBLE_ANNOTATIONS)) {
							this.attributes[attributesIndex++] = new RuntimeVisibleAnnotationsAttribute(classFileBytes, this.constantPool, readOffset);
						} else if (Arrays.equals(attributeName, AttributeNamesConstants.RUNTIME_INVISIBLE_ANNOTATIONS)) {
							this.attributes[attributesIndex++] = new RuntimeInvisibleAnnotationsAttribute(classFileBytes, this.constantPool, readOffset);
						} else {
							this.attributes[attributesIndex++] = new ClassFileAttribute(classFileBytes, this.constantPool, readOffset);
						}
						readOffset += (6 + u4At(classFileBytes, readOffset + 2, 0));
					}
				} else {
					for (int i = 0; i < this.attributesCount; i++) {
						readOffset += (6 + u4At(classFileBytes, readOffset + 2, 0));
					}
				}
			}
			if (readOffset != classFileBytes.length) {
				throw new ClassFormatException(ClassFormatException.TOO_MANY_BYTES);
			}
		} catch (ClassFormatException e) {
			throw e;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ClassFormatException(ClassFormatException.ERROR_TRUNCATED_INPUT);
		} catch (Exception e) {
			// need to know what kind of exception can be thrown
			e.printStackTrace();
			throw new ClassFormatException(ClassFormatException.ERROR_TRUNCATED_INPUT);
		}
	}

	/*
	 * @see IClassFileReader#getAccessFlags()
	 */
	public int getAccessFlags() {
		return this.accessFlags;
	}

	/*
	 * @see IClassFileReader#getAttributeCount()
	 */
	public int getAttributeCount() {
		return this.attributesCount;
	}

	/*
	 * @see IClassFileReader#getAttributes()
	 */
	public ClassFileAttribute[] getAttributes() {
		return this.attributes;
	}

	/*
	 * @see IClassFileReader#getClassIndex()
	 */
	public int getClassIndex() {
		return this.classNameIndex;
	}

	/*
	 * @see IClassFileReader#getClassName()
	 */
	public char[] getClassName() {
		return this.className;
	}

	private char[] getConstantClassNameAt(byte[] classFileBytes, int[] constantPoolOffsets, int constantPoolIndex) {
		int utf8Offset = constantPoolOffsets[u2At(classFileBytes, constantPoolOffsets[constantPoolIndex] + 1, 0)];
		return utf8At(classFileBytes, utf8Offset + 3, 0, u2At(classFileBytes, utf8Offset + 1, 0));
	}

	/*
	 * @see IClassFileReader#getConstantPool()
	 */
	public ConstantPool getConstantPool() {
		return this.constantPool;
	}

	/*
	 * @see IClassFileReader#getFieldInfos()
	 */
	public FieldInfo[] getFieldInfos() {
		return this.fields;
	}

	/*
	 * @see IClassFileReader#getFieldsCount()
	 */
	public int getFieldsCount() {
		return this.fieldsCount;
	}

	/*
	 * @see IClassFileReader#getInnerClassesAttribute()
	 */
	public InnerClassesAttribute getInnerClassesAttribute() {
		return this.innerClassesAttribute;
	}

	/*
	 * @see IClassFileReader#getInterfaceIndexes()
	 */
	public int[] getInterfaceIndexes() {
		return this.interfaceIndexes;
	}

	/*
	 * @see IClassFileReader#getInterfaceNames()
	 */
	public char[][] getInterfaceNames() {
		return this.interfaceNames;
	}

	/*
	 * @see IClassFileReader#getMagic()
	 */
	public int getMagic() {
		return this.magicNumber;
	}

	/*
	 * @see IClassFileReader#getMajorVersion()
	 */
	public int getMajorVersion() {
		return this.majorVersion;
	}

	/*
	 * @see IClassFileReader#getMethodInfos()
	 */
	public MethodInfo[] getMethodInfos() {
		return this.methods;
	}

	/*
	 * @see IClassFileReader#getMethodsCount()
	 */
	public int getMethodsCount() {
		return this.methodsCount;
	}

	/*
	 * @see IClassFileReader#getMinorVersion()
	 */
	public int getMinorVersion() {
		return this.minorVersion;
	}

	/*
	 * @see IClassFileReader#getSourceFileAttribute()
	 */
	public SourceFileAttribute getSourceFileAttribute() {
		return this.sourceFileAttribute;
	}

	/*
	 * @see IClassFileReader#getSuperclassIndex()
	 */
	public int getSuperclassIndex() {
		return this.superclassNameIndex;
	}

	/*
	 * @see IClassFileReader#getSuperclassName()
	 */
	public char[] getSuperclassName() {
		return this.superclassName;
	}

	/*
	 * @see IClassFileReader#isClass()
	 */
	public boolean isClass() {
		return !isInterface();
	}

	/*
	 * @see IClassFileReader#isInterface()
	 */
	public boolean isInterface() {
		return (getAccessFlags() & IModifierConstants.ACC_INTERFACE) != 0;
	}
}
