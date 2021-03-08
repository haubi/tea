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
package org.eclipse.tea.library.build.chain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.Service;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.internal.Activator;
import org.eclipse.tea.library.build.services.TeaBuildElementFactory;
import org.eclipse.tea.library.build.services.TeaBuildVisitor;
import org.eclipse.tea.library.build.services.TeaDependencyWireFactory;
import org.eclipse.tea.library.build.services.TeaElementFailurePolicy;
import org.eclipse.tea.library.build.services.TeaElementFailurePolicy.FailurePolicy;
import org.eclipse.tea.library.build.services.TeaElementVisitPolicy;
import org.eclipse.tea.library.build.services.TeaElementVisitPolicy.VisitPolicy;

/**
 * Encapsulates "building" of a set of projects, where building consists of:
 * <ul>
 * <li>Creating to-be-built elements based on a given set of projects from the
 * workspace.
 * <li>Creating references between these elements and calculating an order to
 * build these elements.
 * <li>Calling all registered visitors that want to contribute to "building" an
 * element.
 * </ul>
 * <p>
 * "Build" in this context could be for instance:
 * <ul>
 * <li>Compiling a project
 * <li>Calling a generator that generates code prior to compiling a project
 * <li>...
 * </ul>
 */
@SuppressWarnings("restriction")
public class TeaBuildChain {

	private final Map<String, TeaBuildElement> namedElements = new TreeMap<>();
	private final Map<Integer, List<TeaBuildElement>> groupedElements = new TreeMap<>();
	private final List<TeaBuildVisitor> visitors;
	private final IEclipseContext context;

	private static final String PROJECTS_KEY = "TeaBuildChain.projectsToBuild";

	/**
	 * Public for Injection, use {@link #make(IEclipseContext, Collection)} to
	 * create instances manually.
	 */
	@Inject
	public TeaBuildChain(IEclipseContext context, TaskingLog log,
			@Named(PROJECTS_KEY) Collection<IProject> projectsToBuild,
			@Service List<TeaBuildElementFactory> elementFactories,
			@Service List<TeaDependencyWireFactory> dependencyFactories, @Service List<TeaBuildVisitor> visitors) {
		this.visitors = visitors;
		this.context = context;

		// Step 1: create all elements, so that dependency calculation can
		// access them
		elementFactories.forEach(f -> ContextInjectionFactory.invoke(f, Execute.class, context, null));

		for (IProject prj : projectsToBuild) {
			List<TeaBuildElement> elements = elementFactories.stream().map(f -> f.createElements(this, prj))
					.filter(Objects::nonNull).flatMap(l -> l.stream()).filter(Objects::nonNull)
					.collect(Collectors.toList());

			if (elements.isEmpty()) {
				// nobody knows how to handle this... create a dummy element to
				// allow dependencies.
				elements.add(new TeaUnhandledElement(prj.getName()));
			}

			elements.forEach(e -> namedElements.put(e.getName(), e));
		}

		// Step 2: calculate dependencies of each element
		dependencyFactories.forEach(f -> ContextInjectionFactory.invoke(f, Execute.class, context, null));
		dependencyFactories.forEach(f -> f.createWires(this));

		// Step 3: sort elements by build order into groups. lazily triggers
		// calculation of order groups (getBuildOrder()).
		namedElements.values().forEach(e -> groupedElements.computeIfAbsent(e.getBuildOrder(), ArrayList::new).add(e));
	}

	/**
	 * Creates a {@link TeaBuildChain} that is capable of building the given
	 * {@link IProject}s.
	 *
	 * @param ctx
	 *            the {@link IEclipseContext} used by TEA (by the TEA Task
	 *            calling this method).
	 * @param projectsToBuild
	 *            the projects to build. May be anything from a hand-selected
	 *            list of projects to all projects in the workspace.
	 * @return a {@link TeaBuildChain} that contains all elements required to
	 *         build the given projects.
	 */
	public static TeaBuildChain make(IEclipseContext ctx, Collection<IProject> projectsToBuild) {
		IEclipseContext context = ctx.createChild("TeaBuildChain");
		context.set(PROJECTS_KEY, projectsToBuild);
		TeaBuildChain chain = ContextInjectionFactory.make(TeaBuildChain.class, context);
		context.set(TeaBuildChain.class, chain);
		return chain;
	}

	public List<String> getBuildOrder() {
		return groupedElements.values().stream()
				.flatMap(teaBuildElement -> teaBuildElement.stream().filter(e -> e instanceof TeaBuildProjectElement)
						.map(e -> ((TeaBuildProjectElement) e).getProject().getName()))
				.collect(Collectors.toList());
	}

	/**
	 * Retrieve the {@link TeaBuildElement} for the given name. This can be used
	 * to lookup specific elements e.g. for dependency wiring.
	 *
	 * @see TeaDependencyWireFactory
	 *
	 * @param name
	 *            the name of the element.
	 * @return the corresponding {@link TeaBuildElement} or <code>null</code> if
	 *         it does not exist.
	 */
	public TeaBuildElement getElementFor(String name) {
		return namedElements.get(name);
	}

	/**
	 * @return all {@link TeaBuildElement}s that are currently participating in
	 *         the {@link TeaBuildChain}.
	 */
	public Collection<TeaBuildElement> getAllElements() {
		return namedElements.values();
	}

	/**
	 * @return the amount of work, equaling the amount of participating
	 *         {@link TeaBuildElement}s.
	 */
	public int getTotalProgress() {
		return groupedElements.size();
	}

	/**
	 * Execute the {@link TeaBuildChain}, building all {@link TeaBuildElement}s
	 * participating.
	 *
	 * @param tracker
	 *            a {@link TaskProgressTracker} to update progress on. Also used
	 *            for cancellation check. Total progress can be queried using
	 *            {@link #getTotalProgress()} for tracker setup.
	 * @param failureThreshold
	 *            the amount of failed build elements before stopping any
	 *            further processing. The failure threshold might be exceeded
	 *            due to batch processing of elements that have no dependencies
	 *            to each other.
	 * @return {@link IStatus} describing the overall build status.
	 */
	public IStatus execute(TaskProgressTracker tracker, long failureThreshold) {
		MultiStatus ms = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, "Build", null);
		context.set(IStatus.class, ms);

		for (TeaBuildVisitor visitor : visitors) {
			ContextInjectionFactory.invoke(visitor, Execute.class, context, null);
		}

		long failures = 0;

		// elements are in order already
		for (Map.Entry<Integer, List<TeaBuildElement>> entry : groupedElements.entrySet()) {
			if (tracker.isCanceled()) {
				throw new OperationCanceledException();
			}

			tracker.setTaskName("Processing Group " + entry.getKey());
			for (TeaBuildVisitor visitor : visitors) {
				for (TeaBuildElement e : entry.getValue()) {
					if (getVisitPolicyFor(e) == VisitPolicy.ABORT_IF_PREVIOUS_ERROR) {
						if (ms.getSeverity() > IStatus.WARNING) {
							ms.add(new Status(IStatus.CANCEL, Activator.PLUGIN_ID,
									"Abort prior to executing " + e.getName() + " due to previous error"));
							return ms;
						}
					}
				}

				Map<TeaBuildElement, IStatus> results = Optional.of(visitor.visit(entry.getValue()))
						.orElse(Collections.emptyMap());

				for (Entry<TeaBuildElement, IStatus> result : results.entrySet()) {
					IStatus status = result.getValue();
					if (status.getSeverity() > IStatus.WARNING) {
						TeaBuildElement element = result.getKey();
						switch (getFailurePolicyFor(element)) {
						case ABORT_IMMEDIATE:
							ms.add(status);
							return ms;
						case IGNORE:
							ms.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
									"Ignored failure in element " + element.getName()));
							break;
						case USE_THRESHOLD:
							ms.add(status);
							if (failures++ >= failureThreshold) {
								return ms;
							}
						}
					}
				}
			}
			tracker.worked(1);
		}

		return ms;
	}

	/**
	 * Determines how to handle a failure to build the given
	 * {@link TeaBuildElement}.
	 *
	 * @param element
	 *            the element to query
	 * @return a {@link FailurePolicy} describing the action to take on failure.
	 */
	private FailurePolicy getFailurePolicyFor(TeaBuildElement element) {
		TeaElementFailurePolicy a = element.getClass().getAnnotation(TeaElementFailurePolicy.class);
		if (a == null) {
			return FailurePolicy.USE_THRESHOLD;
		}
		return a.value();
	}

	/**
	 * Determines how to handle a failure prior to building the given
	 * {@link TeaBuildElement}.
	 *
	 * @param element
	 *            the element to query
	 * @return a {@link VisitPolicy} describing the action to take on failure.
	 */
	private VisitPolicy getVisitPolicyFor(TeaBuildElement element) {
		TeaElementVisitPolicy a = element.getClass().getAnnotation(TeaElementVisitPolicy.class);
		if (a == null) {
			return VisitPolicy.USE_THRESHOLD;
		}
		return a.value();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append(getClass().getSimpleName()).append(":\n");
		groupedElements.forEach(
				(g, e) -> e.forEach(x -> builder.append("\t").append(g).append(": ").append(x.getName()).append("\n")));

		return builder.toString();
	}

}
