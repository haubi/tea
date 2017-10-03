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
package org.eclipse.tea.library.build.chain.jdk;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.chain.TeaBuildElement;
import org.eclipse.tea.library.build.model.PluginData;
import org.eclipse.tea.library.build.util.FileUtils;

public class TeaJdkLibBuildElement extends TeaBuildElement {

	private IProject project;
	private String libName;
	private String target;

	public TeaJdkLibBuildElement(IProject project, String libName, String target) {
		this.project = project;
		this.libName = libName;
		this.target = target;
	}
	
	public IProject getProject() {
		return project;
	}
	
	void execute(TaskingLog log) throws Exception {
		PluginData data = new PluginData(project);
		
		// don't copy if it is already there.
		File targetFile = new File(data.getBundleDir(), target);
		if(targetFile.exists()) {
			log.info("using existing " + targetFile);
		}
		
		// find current JDK
		String[] envs = data.getRequiredExecutionEnvironment();
		if(envs.length < 1) {
			log.warn("no execution environments for " + project);
			return;
		}
		
		IExecutionEnvironmentsManager eeManager= JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment bestEE= null;
		for(String env : envs) {
			bestEE = eeManager.getEnvironment(env);
			if(bestEE != null) {
				break;
			}
		}
		
		IVMInstall vm = bestEE.getDefaultVM();
		if(vm == null) {
			log.warn("no default VM for " + bestEE.getId());
			IVMInstall[] compatibleVMs = bestEE.getCompatibleVMs();
			if(compatibleVMs == null || compatibleVMs.length < 1) {
				log.warn("no compatile VM for " + bestEE.getId());
				return;
			}
			vm = compatibleVMs[0]; // pick any.
		}
		
		// lookup library
		File f = new File(vm.getInstallLocation(), libName);
		if(!f.exists()) {
			throw new IllegalStateException("VM installation in " + vm.getInstallLocation() + " does not have " + libName);
		}
		
		// copy to target path
		FileUtils.copyFile(f, targetFile);
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
	}
	
	@Override
	public String getName() {
		return "JDK Library " + libName + " for " + project.getName();
	}

}
