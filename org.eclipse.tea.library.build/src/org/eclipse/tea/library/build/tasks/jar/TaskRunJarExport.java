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
package org.eclipse.tea.library.build.tasks.jar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.chain.plugin.TeaBuildPluginElement;
import org.eclipse.tea.library.build.config.BuildDirectories;
import org.eclipse.tea.library.build.jar.JarManager;
import org.eclipse.tea.library.build.jar.ZipConfig;
import org.eclipse.tea.library.build.model.PluginBuild;
import org.eclipse.tea.library.build.model.WorkspaceBuild;
import org.eclipse.tea.library.build.util.FileUtils;
import org.eclipse.tea.library.build.util.TeaBuildUtil;

/**
 * Generates JAR files from the plug-ins.
 */
public class TaskRunJarExport {

	/** the family name of the checkout jobs */
	private final String JobFamily = this.getClass().getName();

	private final boolean composite;

	private static final String PLUGINS_DIRECTORY = "plugins";

	public TaskRunJarExport() {
		this(false);
	}

	public TaskRunJarExport(boolean composite) {
		this.composite = composite;
	}

	/**
	 * Returns the plugins for which we have to create JARs. Intended to be
	 * overridden by subclasses.
	 */
	protected Collection<PluginBuild> getPlugins(WorkspaceBuild wb) {
		return wb.getSourcePlugIns();
	}

	@Execute
	public void run(TaskingLog log, WorkspaceBuild wb, JarManager jarManager, BuildDirectories dirs,
			TaskProgressTracker tracker, ZipConfig config) throws Exception {

		final File distDirectory = new File(dirs.getOutputDirectory(), PLUGINS_DIRECTORY);

		// reset destination directory
		if (!composite) {
			FileUtils.deleteDirectory(distDirectory);
		}
		FileUtils.mkdirs(distDirectory);

		log.info("jar destination directory: " + distDirectory);

		// all jobs that have been started
		ArrayList<Job> startedJobs = new ArrayList<>();

		// loop over all source plugins
		List<PluginBuild> plugins = new ArrayList<>(getPlugins(wb));

		log.info("Number of plugins: " + plugins.size());

		for (int i = 0; i < plugins.size();) {

			if (tracker.isCanceled()) {
				break;
			}

			// parallel job count
			if (getNumberOfJobs(JobFamily) >= config.zipParallelThreads) {
				Thread.sleep(10);
				continue;
			}

			final PluginBuild pb = plugins.get(i);

			/*
			 * Job that exports one project.
			 */
			Job job = new Job("Export " + pb.getPluginName()) {

				@Override
				public boolean belongsTo(Object family) {
					if (family.equals(JobFamily)) {
						return true;
					}
					return false;
				}

				@Override
				public IStatus run(IProgressMonitor monitor) {

					try {
						log.info("execJarCommands: " + pb.getPluginName());

						IStatus status = TeaBuildUtil.getStatus(new TeaBuildPluginElement(pb));
						if (status.getSeverity() > IStatus.WARNING) {
							throw new RuntimeException(pb.getPluginName() + " has errors");
						}

						jarManager.execJarCommands(pb, distDirectory);

					} catch (Exception e) {
						log.info(pb.getPluginName() + " " + e.toString());
						return new Status(Status.ERROR, getClass().getName(), "unexpected exception during jar export",
								e);
					}
					return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.schedule();
			startedJobs.add(job);
			tracker.worked(1);
			++i;
		}

		// wait until all checkout jobs have finished
		Job.getJobManager().join(JobFamily, null);

		if (tracker.isCanceled()) {
			throw new RuntimeException("cancelled");
		}

		// check if all jobs have been successful
		for (Job job : startedJobs) {
			if (!job.getResult().isOK()) {
				Throwable cause = job.getResult().getException();
				throw new RuntimeException("Job failed:" + job.getName(), cause);
			}
		}
	}

	/**
	 * @return the number of running checkout jobs that belong to the given
	 *         family
	 */
	private static int getNumberOfJobs(String jobFamily) {
		IJobManager manager = Job.getJobManager();
		Job[] jobs = manager.find(jobFamily);
		return jobs.length;
	}

	@Override
	public String toString() {
		return "Export All Jars";
	}

	public static String getPluginJarDirectory() {
		return PLUGINS_DIRECTORY;
	}

}
