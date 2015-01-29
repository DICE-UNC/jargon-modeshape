package org.irods.jargon.modeshape.connector;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * A {@link FilenameFilter} implementation that supports an inclusion and
 * exclusion pattern.
 */
public class InclusionExclusionFilenameFilter implements java.io.FilenameFilter {
	private String inclusionPattern = null;
	private String exclusionPattern = null;
	private Pattern inclusion;
	private Pattern exclusion;
	private Pattern extraPropertiesExclusion;

	public void setExclusionPattern(final String exclusionPattern) {
		this.exclusionPattern = exclusionPattern;
		if (exclusionPattern == null) {
			exclusion = null;
		} else {
			exclusion = Pattern.compile(exclusionPattern);
		}
	}

	public void setExtraPropertiesExclusionPattern(final String exclusionPattern) {
		if (exclusionPattern == null) {
			extraPropertiesExclusion = null;
		} else {
			extraPropertiesExclusion = Pattern.compile(exclusionPattern);
		}
	}

	public void setInclusionPattern(final String inclusionPattern) {
		this.inclusionPattern = inclusionPattern;
		if (inclusionPattern == null) {
			inclusion = null;
		} else {
			inclusion = Pattern.compile(inclusionPattern);
		}
	}

	public String getExclusionPattern() {
		return exclusionPattern;
	}

	public String getInclusionPattern() {
		return inclusionPattern;
	}

	@Override
	public boolean accept(final File file, final String name) {
		if (inclusionPattern == null) {
			// Include unless it matches an exclusion ...
			if (exclusionPattern != null && exclusion.matcher(name).matches()) {
				return false;
			}
			if (extraPropertiesExclusion != null
					&& extraPropertiesExclusion.matcher(name).matches()) {
				return false;
			}
			return true;
		}
		// Include ONLY if it matches the inclusion AND not matched by the
		// exclusions ...
		if (!inclusion.matcher(name).matches()) {
			return false;
		}
		if (exclusionPattern != null && exclusion.matcher(name).matches()) {
			return false;
		}
		if (extraPropertiesExclusion != null
				&& extraPropertiesExclusion.matcher(name).matches()) {
			return false;
		}
		return true;
	}
}