/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-v10.html
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

import org.eclipse.tea.core.annotations.TaskChainMenuEntry;

/**
 * Service Interface that provides static fields annotated with the
 * TaskingSubMenuDecoration annotation.
 */
public interface TaskingMenuDecoration {

	/**
	 * Specifies that no grouping has been explicitly defined.
	 */
	public static final String NO_GROUPING = "none";

	/**
	 * Annotates a public static final {@link String} field in a
	 * {@link TaskingMenuDecoration} implementation. The type of the static
	 * field must be {@link String}, and the value is the path to the icon
	 * relative to the root of the defining bundle.
	 */
	@Documented
	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface TaskingMenuPathDecoration {
		/**
		 * @return path to the menu to decorate
		 */
		String[] menuPath();

		/**
		 * @return an optional group ID that allows grouping together and
		 *         ranking of menu items in the final menu.
		 */
		String groupingId() default NO_GROUPING;
	}

	/**
	 * Annotates a public static final {@link String} field in a
	 * {@link TaskingMenuDecoration} implementation. The type of the static
	 * field must be {@link String}, and the value is the id of the group
	 * defined by the field.
	 * <p>
	 * The group ID can be used by {@link TaskingMenuDecoration} and
	 * {@link TaskChainMenuEntry} annotations and controls the grouping and
	 * sorting of menu items within the menu tier that contains those elements.
	 * Thus a {@link TaskingMenuGroupingId} is assigned to a certain menu path,
	 * which is the menu whose items are controlled by this element.
	 * <p>
	 * It is not required to define such a group explicitly. It will be created
	 * implicitly once there is a {@link TaskChainMenuEntry} or
	 * {@link TaskingMenuPathDecoration} referencing the group's id. However
	 * this annotation allows to control the relative position in the menu.
	 * <p>
	 * There is an implicit group {@link TaskingMenuDecoration#NO_GROUPING} that
	 * is used when no group is specified for a {@link TaskChainMenuEntry} or a
	 * {@link TaskingMenuPathDecoration}.
	 */
	@Documented
	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface TaskingMenuGroupingId {
		/**
		 * @return path to the sub-menu that contains the group
		 */
		String[] menuPath() default {};

		/**
		 * @return sort this group after the group specified here. XOR with
		 *         {@link #beforeGroupingId()}. Only one may be set.
		 */
		String afterGroupingId() default ""; // intentionally ""

		/**
		 * @return sort this group before the group specified here. XOR with
		 *         {@link #beforeGroupingId()}. Only one may be set.
		 */
		String beforeGroupingId() default ""; // intentionally ""
	}

}
