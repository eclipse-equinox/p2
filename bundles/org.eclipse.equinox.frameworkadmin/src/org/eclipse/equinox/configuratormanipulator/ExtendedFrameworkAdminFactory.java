package org.eclipse.equinox.configuratormanipulator;
///*******************************************************************************
// * Copyright (c) 2007 IBM Corporation and others.
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the Eclipse Public License v1.0
// * which accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// * 
// * Contributors:
// *     IBM Corporation - initial API and implementation
// *******************************************************************************/
//package org.eclipse.equinox.configurator;
//
//import org.eclipse.equinox.frameworkadmin.FrameworkAdmin;
//import org.eclipse.equinox.frameworkadmin.FrameworkAdminFactory;
//
//// This class is used for only method 1 to support ConfiguratorManipulator from a Java program 
//
//public abstract class ExtendedFrameworkAdminFactory extends FrameworkAdminFactory {
//
//	abstract protected FrameworkAdmin createFrameworkAdmin(String configuratorManipulatorFactoryName) throws InstantiationException, IllegalAccessException, ClassNotFoundException;
//
//	public static FrameworkAdmin getInstance(String className, String configuratorManipulatorFactoryName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
//		ExtendedFrameworkAdminFactory factory = (ExtendedFrameworkAdminFactory) Class.forName(className).newInstance();
//		return (FrameworkAdmin) factory.createFrameworkAdmin(configuratorManipulatorFactoryName);
//	}
//}
