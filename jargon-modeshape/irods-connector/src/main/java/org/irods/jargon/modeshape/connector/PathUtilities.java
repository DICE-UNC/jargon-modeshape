/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.io.File;

import javax.jcr.NamespaceRegistry;

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
	public static final String NT_FOLDER = "nt:folder";
	public static final String NT_FILE = "nt:file";
	public static final String NT_RESOURCE = "nt:resource";
	public static final String MIX_MIME_TYPE = "mix:mimeType";
	public static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
	public static final String JCR_DATA = "jcr:data";
	public static final String JCR_MIME_TYPE = "jcr:mimeType";
	public static final String JCR_ENCODING = "jcr:encoding";
	public static final String JCR_CREATED = "jcr:created";
	public static final String JCR_CREATED_BY = "jcr:createdBy";
	public static final String JCR_LAST_MODIFIED = "jcr:lastModified";
	public static final String JCR_LAST_MODIFIED_BY = "jcr:lastModified";
	public static final String JCR_CONTENT = "jcr:content";
	public static final String JCR_IRODS_AVU = "irods:avu";
	public static final String JCR_CONTENT_SUFFIX = DELIMITER + JCR_CONTENT;
	public static final int JCR_CONTENT_SUFFIX_LENGTH = JCR_CONTENT_SUFFIX
			.length();
	public static final String JCR_AVU_SUFFIX = DELIMITER + JCR_IRODS_AVU;
	public static final int JCR_AVU_SUFFIX_LENGTH = JCR_AVU_SUFFIX.length();

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

	/**
	 * Constructor takes the absolute path to the iRODS directory in the json
	 * config for the iRODS projection
	 * 
	 * @param directoryPath
	 *            <code>String</code> with the directory path
	 */
	public PathUtilities(final String directoryPath) {
		if (directoryPath == null || directoryPath.isEmpty()) {
			throw new IllegalArgumentException("null or empty directoryPath");
		}

		this.directoryPath = directoryPath;

		this.directoryPathWithTrailingSlash = directoryPath + "/";
		this.directoryAbsolutePathLength = directoryPathWithTrailingSlash
				.length() - DELIMITER.length();
	}

}
