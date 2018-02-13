/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.library.build.p2;

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.internal.Activator;

@SuppressWarnings("restriction")
public class TargetPlatformHelper {

	public static Job setTargetPlatform(TaskingLog log, String tp, IProject bep, boolean user) {
		if (tp == null || tp.isEmpty()) {
			throw new IllegalStateException("no target platform set, skipping");
		}

		final ITargetPlatformService service = getTargetPlatformService();
		ITargetDefinition definition = null;
		try {
			URI tpDef = null;
			if (tp.startsWith("/")) {
				File f = new File(tp);
				if (f.isFile()) {
					tpDef = f.toURI();
				}
			} else if (bep != null) {
				IResource member = bep.findMember(tp);
				if (member.exists() && member.getType() == IResource.FILE) {
					tpDef = member.getLocationURI();
				}
			}

			log.debug("looking up target platform, trying file: " + tpDef);

			if (tpDef == null) {
				// the following code is more or less the same as used by SDC to
				// find installed
				// local targets
				File installLoc = P2InstallationHelper.getInstallLocation(log, tp);
				log.debug("creating target from IU install location: " + installLoc);

				definition = createTargetFromLocation(service, installLoc);
			} else {
				log.debug("loading target from: " + tpDef);
				ITargetHandle target = service.getTarget(tpDef);
				definition = target.getTargetDefinition();
			}
		} catch (Exception e) {
			throw new IllegalStateException("cannot resolve target definition", e);
		}

		if (definition == null) {
			throw new IllegalStateException("cannot resolve target definition");
		}

		log.debug("loading target definition");
		final LoadTargetDefinitionJob job = new LoadTargetDefinitionJob(definition);
		job.setUser(user);
		job.addJobChangeListener(new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult() != Status.OK_STATUS) {
					Activator.log(IStatus.ERROR, "failed to set the target platform: " + event.getResult(), null);
				}
			}
		});
		job.schedule();
		return job;
	}

	private static ITargetDefinition createTargetFromLocation(final ITargetPlatformService service, File installLoc)
			throws CoreException {
		ITargetDefinition definition;
		definition = service.newTarget();
		definition.setName("Anonymous Target for Build");
		ITargetLocation loc = service.newDirectoryLocation(installLoc.getAbsolutePath());
		definition.setTargetLocations(new ITargetLocation[] { loc });
		service.saveTargetDefinition(definition);
		return definition;
	}

	public static ITargetDefinition getCurrentTargetDefinition() throws CoreException {
		ITargetPlatformService service = getTargetPlatformService();
		return service.getWorkspaceTargetHandle().getTargetDefinition();
	}

	public static ITargetPlatformService getTargetPlatformService() {
		return (ITargetPlatformService) ServiceHelper.getService(Activator.getContext(),
				ITargetPlatformService.class.getName());
	}

}
