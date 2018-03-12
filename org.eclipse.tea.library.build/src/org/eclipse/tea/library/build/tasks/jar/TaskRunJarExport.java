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
import org.eclipse.tea.library.build.model.BundleBuild;
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

	private static TemporaryJarCache cache;

	private static final String PLUGINS_DIRECTORY = "plugins";

	public TaskRunJarExport() {
		this(false);
	}

	public TaskRunJarExport(boolean composite) {
		this.composite = composite;
	}

	/**
	 * Create a cache to improve JAR creation performance if a single JAR file
	 * is requested multiple times.
	 * <p>
	 * Callers MUST make sure to call {@link #cleanCache(TaskingLog)} as well.
	 *
	 * @param dir
	 *            a temporary directory to use to cache JAR file creation
	 *            results.
	 */
	public static void initCache(File dir) {
		cache = new TemporaryJarCache(dir);
	}

	/**
	 * Clears a cache previously initialized with {@link #initCache(File)}
	 *
	 * @param log
	 *            used to log cache statistics.
	 */
	public static void cleanCache(TaskingLog log) {
		if (cache != null) {
			cache.clear(log);
		}
		cache = null;
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

						execJarCached(jarManager, distDirectory, pb);
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

	protected static void execJarCached(JarManager jarManager, File distDirectory, BundleBuild<?> bb) throws Exception {
		if (cache != null) {
			cache.execJarCommands(jarManager, bb, distDirectory);
		} else {
			jarManager.execJarCommands(bb, distDirectory);
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

	static class TemporaryJarCache {

		private final File dir;

		private long missCnt = 0;
		private long hitCnt = 0;
		private long skipCnt = 0;

		TemporaryJarCache(File dir) {
			this.dir = dir;

			if (!dir.isDirectory()) {
				FileUtils.mkdirs(dir);
			}
		}

		private File getCached(JarManager jm, BundleBuild<?> build) throws Exception {
			String jarFileName = build.getJarFileName(jm.getBundleVersion(build.getData()));
			File cached = new File(dir, jarFileName);

			if (cached.exists()) {
				hitCnt++;
				return cached;
			}

			missCnt++;
			return jm.execJarCommands(build, dir);
		}

		File execJarCommands(JarManager jm, BundleBuild<?> build, File destDir) throws Exception {
			File cached = getCached(jm, build);
			File destFile = new File(destDir, cached.getName());
			if (!destFile.exists() || cached.length() != destFile.length()) {
				FileUtils.hardLinkOrCopy(cached, destFile);
			} else {
				skipCnt++;
			}
			return destFile;
		}

		void clear(TaskingLog log) {
			log.info("clearing JAR cache: miss=" + missCnt + ", hit=" + hitCnt + ", skip=" + skipCnt);
			FileUtils.deleteDirectory(dir);
		}

	}

}
