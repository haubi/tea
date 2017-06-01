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
package org.eclipse.tea.core.services;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Service that can be used by tasks to report their progress. By default, each
 * task is assumed to have only a work amount of <code>1</code>. If a task
 * requires more, it can implement a method annotated with
 * {@link TaskProgressProvider}. It will be called to initialize the amount of
 * work.
 */
public interface TaskProgressTracker {

	/**
	 * Annotates a method on a task that is capable of calculating the required
	 * amount of work. The method must return an {@link Integer} (or int)
	 * result.
	 * <p>
	 * Be aware that methods having this annotation are called before any other
	 * tasks are run. This means that this method may NEVER depend on any of the
	 * previous tasks.
	 */
	@Documented
	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface TaskProgressProvider {

	}

	/**
	 * Report work done. This should never exceed the amount of work reported by
	 * the method annotated with {@link TaskProgressProvider}.
	 */
	public void worked(int amount);

	/**
	 * @return whether the task should be cancelled.
	 */
	public boolean isCanceled();

	/**
	 * @param name
	 *            the new name of the task to display during progress reporting.
	 *            Tasks can override toString instead, which is used during
	 *            startup of the task to set the name.
	 */
	public void setTaskName(String name);

}
