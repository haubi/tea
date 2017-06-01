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
package org.eclipse.tea.core.internal;

import java.util.GregorianCalendar;

/**
 * Helps in formatting time.
 */
public class TimeHelper {

	private static long lastMillis;
	private static String lastTimeStamp;

	/**
	 * @return the formatted {@link String} for the current time. Only returns a
	 *         new value every 1000 ms to avoid expensive calculations on every
	 *         call.
	 */
	public static String getFormattedCurrentTime() {
		final long millis = System.currentTimeMillis();
		if (millis - lastMillis >= 1000) {
			lastMillis = millis;
			GregorianCalendar cal = new GregorianCalendar();
			lastTimeStamp = String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS", cal);
		}
		return lastTimeStamp;
	}

	/**
	 * Returns a human readable string of the given duration. The result will
	 * only contain <tt>minutes</tt> and <tt>seconds</tt>.
	 *
	 * <pre>
	 * Sample output:  02 minutes 03 seconds
	 * </pre>
	 *
	 * @param duration
	 *            The duration in milliseconds
	 * @return The string representation
	 */
	public static String formatDuration(long duration) {
		long durationInSeconds = duration / 1000;
		if (duration % 1000 >= 500) {
			++durationInSeconds;
		}
		long seconds = durationInSeconds % 60;
		long minutes = durationInSeconds / 60;
		String minute = minutes == 1 ? "minute" : "minutes";
		String second = seconds == 1 ? "second" : "seconds";
		String formatted = null;
		if (minutes > 0) {
			formatted = String.format("%1$d %2$s %3$02d %4$s", minutes, minute, seconds, second);
		} else {
			formatted = String.format("%1$d %2$s", seconds, second);
		}
		return formatted;
	}

	/**
	 * Returns a human readable string of the given duration. The result will
	 * contain (up to) <tt>hours</tt> , <tt>minutes</tt>, <tt>seconds</tt>,
	 * <tt>milliseconds</tt>.
	 *
	 * <pre>
	 * Sample output:  4 seconds 450 milliseconds
	 *                 3 hours 15 minutes 1 second 750 milliseconds
	 * </pre>
	 *
	 * @param duration
	 *            The duration in milliseconds
	 * @return The string representation.
	 */
	public static String formatDetailedDuration(long duration) {
		final StringBuilder builder = new StringBuilder();

		long milliseconds = duration % 1000;
		long allSeconds = duration / 1000;

		long seconds = allSeconds % 60;
		long allMinutes = allSeconds / 60;

		long minutes = allMinutes % 60;
		long hours = allMinutes / 60;

		if (hours > 0) {
			builder.append(hours);
			builder.append(hours == 1 ? " hour " : " hours ");
		}
		if (minutes > 0) {
			builder.append(minutes);
			builder.append(minutes == 1 ? " minute " : " minutes ");
		}
		if (seconds > 0) {
			builder.append(seconds);
			builder.append(seconds == 1 ? " second " : " seconds ");
		}
		if (milliseconds > 0) {
			builder.append(milliseconds);
			builder.append(milliseconds == 1 ? " millisecond" : " milliseconds");
		}
		return builder.toString().trim();
	}

}
