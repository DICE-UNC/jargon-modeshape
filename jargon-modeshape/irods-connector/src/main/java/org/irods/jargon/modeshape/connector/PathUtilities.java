/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.io.File;

import javax.jcr.NamespaceRegistry;

import org.irods.jargon.core.pub.io.IRODSFile;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;

/**
 * Utilities for understanding and formatting document ids between ModeShape and
 * IRODS
 * 
 * @author Mike Conway - DICE
 * 
 */
public class PathUtilities {

	public static final String DELIMITER = "/";
	public static final String MIX_MIME_TYPE = "mix:mimeType";
	public static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
	public static final String JCR_DATA = "jcr:data";
	public static final String JCR_MIME_TYPE = "jcr:mimeType";
	public static final String JCR_ENCODING = "jcr:encoding";
	public static final String JCR_CREATED = "jcr:created";
	public static final String JCR_CREATED_BY = "jcr:createdBy";
	public static final String JCR_LAST_MODIFIED = "jcr:lastModified";
	public static final String JCR_LAST_MODIFIED_BY = "jcr:lastModified";
	public static final String NT_FOLDER = "nt:folder";
	public static final String NT_FILE = "nt:file";
	public static final String JCR_CONTENT = "jcr:content";
	public static final String JCR_CONTENT_SUFFIX = DELIMITER + JCR_CONTENT;
	public static final int DELIMITER_LENGTH = DELIMITER.length();

	/**
	 * The string path for a {@link File} object that represents the top-level
	 * directory accessed by this connector. This is set via reflection and is
	 * required for this connector.
	 */
	private final String directoryPath;

	/**
	 * A string that is created in the
	 * {@link #initialize(NamespaceRegistry, NodeTypeManager)} method that
	 * represents the absolute path to the {@link #directory}. This path is
	 * removed from an absolute path of a file to obtain the ID of the node.
	 */
	private final String directoryPathWithTrailingSlash;

	/**
	 * length of the absolute path of the directory - the trailing slash
	 */
	private final int directoryAbsolutePathLength;

	private final InclusionExclusionFilenameFilter inclusionExclusionFilenameFilter;

	/**
	 * Constructor takes the absolute path to the iRODS directory in the json
	 * config for the iRODS projection
	 * 
	 * @param directoryPath
	 *            <code>String</code> with the directory path
	 * @param inclusionExclusionFilenameFilter
	 *            {@link InclusinoExclusionFilenameFilter}
	 */
	public PathUtilities(
			final String directoryPath,
			final InclusionExclusionFilenameFilter inclusionExclusionFilenameFilter) {
		if (directoryPath == null || directoryPath.isEmpty()) {
			throw new IllegalArgumentException("null or empty directoryPath");
		}

		if (inclusionExclusionFilenameFilter == null) {
			throw new IllegalArgumentException(
					"null or empty inclusionExclusionFilenameFilter");
		}

		this.directoryPath = directoryPath;
		this.inclusionExclusionFilenameFilter = inclusionExclusionFilenameFilter;

		this.directoryPathWithTrailingSlash = directoryPath + "/";
		this.directoryAbsolutePathLength = directoryPathWithTrailingSlash
				.length() - DELIMITER.length();
	}

	/**
	 * Given an id, determine the JCR node type
	 * 
	 * @param id
	 *            <code>String</code> with the id
	 * @return {@link IrodsNodeTypes} enum value
	 */
	public IrodsNodeTypes getNodeTypeForId(final String id) {
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("unknown node type");
		}

		return IrodsNodeTypes.determineNodeTypeFromId(id);
	}

	/**
	 * Given an id, make sure it starts with a delimiter, and if it ends with
	 * the delimiter, strip it
	 * 
	 * @param id
	 *            <code>String</code> with a jcr id for a node
	 * @return <code>String</code> with a delim at the start and any trailing
	 *         delim stripped out
	 */
	public static String ensureIdStartsWithDelimiterAndStripOutTrailingDelimeter(
			final String id) {

		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}

		if (!id.startsWith(DELIMITER)) {
			throw new IllegalArgumentException("id must start with delimiter");
		}
		if (id.endsWith(DELIMITER)) {
			return id.substring(0, id.length() - DELIMITER.length());
		} else {
			return id;
		}
	}

	/**
	 * @return the directoryPath
	 */
	public String getDirectoryPath() {
		return directoryPath;
	}

	/**
	 * @return the directoryPathWithTrailingSlash
	 */
	public String getDirectoryPathWithTrailingSlash() {
		return directoryPathWithTrailingSlash;
	}

	/**
	 * @return the directoryAbsolutePathLength
	 */
	public int getDirectoryAbsolutePathLength() {
		return directoryAbsolutePathLength;
	}

	/**
	 * @return the inclusionExclusionFilenameFilter
	 */
	public InclusionExclusionFilenameFilter getInclusionExclusionFilenameFilter() {
		return inclusionExclusionFilenameFilter;
	}

	/**
	 * See if the file does not exist or is excluded by any filter. Return
	 * <code>true</code> if this file should be excluded
	 * 
	 * @param file
	 *            {@link IRODSFile} representing the underlying iRODS data
	 * @return <code>boolean</code> if this file should be excluded
	 */
	public boolean isExcluded(final IRODSFile file) {

		if (!file.exists()) {
			return true;
		}

		if (this.getInclusionExclusionFilenameFilter().accept(
				file.getParentFile(), file.getName())) {
			return false;
		} else {

			return true;
		}
	}

	/**
	 * Given an id for a node as given by Modeshape, strip a trailing delim if
	 * present, otherwise return as is
	 * 
	 * @param id
	 *            <code>String</code> with an id that may contain a trailing
	 *            delimiter
	 * @return <code>String</code> with the id stripped of any trailing
	 *         delimiter
	 */
	public static String stripTrailingDelimFromIdIfPresent(final String id) {
		if (id == null) {
			throw new IllegalArgumentException("Null id");
		}

		if (id.endsWith(DELIMITER)) {
			return id.substring(0, id.length() - DELIMITER.length());
		} else {
			return id;
		}
	}

}
