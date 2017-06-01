#!/usr/bin/env bash

##########################################################################
#  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
#
#  Contributors:
#      SSI Schaefer IT Solutions GmbH
##########################################################################

#--------------------------------------------------#
# Script to run TEA headlessly.                    #
#  Usage: $0 <workspace> <taskchain> [<vmargs>...] #
#--------------------------------------------------#

target="$(cd $(dirname "$0"); pwd)"
workspace="$1"; shift
taskchain="$1"; shift

eclipse=${target}/eclipse
properties=${target}/headless.properties

# SSI Schaefer SDC Update specific
#   waits until a running automatic update has finished. does not do any
#   harm if SDC auto-update is not present at all
while test -e "${target}/.sdc.update.lock"; do
	echo "Waiting for update lock..."; sleep 10
done
# END SSI Schaefer SDC Update specific

vmargs="-Dequinox.statechange.timeout=10000 -Declipse.p2.mirrors=false -Dfile.encoding=UTF-8 $@"
[[ -z "${workspace}" ]] && { echo "workspace not given. usage: $0 <workspace> <taskchain> <vmargs...>"; exit 1; }
[[ -z "${taskchain}" ]] && { echo "taskchain not given. usage: $0 <workspace> <taskchain> <vmargs...>"; exit 1; }

echo "TEA-Launcher: Using '${eclipse}' with '${properties}'"
echo "TEA-Launcher: Running '${taskchain}' on '${workspace}'"

"${eclipse}" --launcher.suppressErrors --launcher.appendVmargs -nosplash -product org.eclipse.tea.core.ui.HeadlessTaskingEngine -application org.eclipse.tea.core.ui.TaskingEngineExtendedApplication -data "${workspace}" -properties "${properties}" -taskchain ${taskchain} -vmargs $vmargs
