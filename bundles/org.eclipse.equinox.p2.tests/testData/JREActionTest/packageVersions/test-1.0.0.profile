###############################################################################
# Copyright (c) 2011 SAP AG and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
# 
# Contributors:
#     SAP AG - initial API and implementation
###############################################################################
org.osgi.framework.system.packages = \
 my.package,\
 my.package;version="1.0.0"
org.osgi.framework.bootdelegation = \
 javax.*,\
 org.ietf.jgss,\
 org.omg.*,\
 org.w3c.*,\
 org.xml.*,\
 sun.*,\
 com.sun.*
org.osgi.framework.executionenvironment = \
 OSGi/Minimum-1.0,\
 OSGi/Minimum-1.1,\
 JRE-1.1,\
 J2SE-1.2,\
 J2SE-1.3,\
 J2SE-1.4,\
 J2SE-1.5,\
 JavaSE-1.6
osgi.java.profile.name = JavaSE-1.6
