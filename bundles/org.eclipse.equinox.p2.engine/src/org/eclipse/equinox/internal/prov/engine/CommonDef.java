/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.prov.engine;

/*
 *  This interface contains global constant definitions.
 *  
 *  Use this interface to define constants that are likely to be used
 *  widely in different contexts with share a common intended meaning.
 *
 */
public interface CommonDef {

	public static final String EmptyString = ""; //$NON-NLS-1$
	public static final String SpaceString = " "; //$NON-NLS-1$
	public static final String Underscore = "_"; //$NON-NLS-1$
	public static final String Dot = "."; //$NON-NLS-1$
	public static final String DotDot = ".."; //$NON-NLS-1$

	public static final String EncodedSpaceString = "%20"; //$NON-NLS-1$ 

	public static final String UncPrefix = "\\\\"; //$NON-NLS-1$
	public static final char ColonChar = ':';

	/*
	 * Strings used as the type for the native and eclipse touchpoints,
	 * including the type in the touchpoints extension point.
	 */
	public static final String NativeTouchpoint = "native"; //$NON-NLS-1$
	public static final String EclipseTouchpoint = "eclipse"; //$NON-NLS-1$   

	public static final int MaxPathLength_Win32 = 256;
	public static final int MaxPathLength_Linux = 1024;
	//    
	//    /*
	//     * Different protocols
	//     */
	//    public interface Protocols {
	//        public static final String File = "file"; //$NON-NLS-1$
	//        public static final String Http = "http"; //$NON-NLS-1$
	//        public static final String Https = "https"; //$NON-NLS-1$
	//        public static final String Ftp = "ftp"; //$NON-NLS-1$
	//        public static final String Socks = "SOCKS"; //$NON-NLS-1$
	//    }
	//
	//    /*
	//     * File name extensions.
	//     */
	//    public interface Extensions {
	//        public static final String Xml = ".xml"; //$NON-NLS-1$
	//        public static final String Zip = ".zip"; //$NON-NLS-1$
	//        public static final String Jar = ".jar"; //$NON-NLS-1$
	//        public static final String Properties = ".properties"; //$NON-NLS-1$
	//    }
}
