###############################################################################
# Copyright (c) 2012 SAP AG and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
# 
# Contributors:
#     SAP AG - initial API and implementation
###############################################################################

# the first entry has a valid syntax (there are two value components), but the second value component "other.namespace" is ignored
org.osgi.framework.system.capabilities = \
 osgi.ee; other.namespace; version:List<Version>="1.0"; osgi.ee="JavaSE",\
 osgi.ee; osgi.ee="JavaSE"; version:List<Version>="1.a.invalidversion",\
 osgi.ee; osgi.ee="OSGi/Minimum",\
 osgi.ee; version:List<Version>="2.0,2.1",\
 osgi.ee; osgi.ee="JavaSE"; version:List<Version>="1.0, 1.1"; version:Version="1.1"
osgi.java.profile.name = EECapabilitySyntaxTest
