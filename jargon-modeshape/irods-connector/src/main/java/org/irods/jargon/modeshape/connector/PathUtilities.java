/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.io.File;

import javax.jcr.NamespaceRegistry;

import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.modeshape.connector.nodetypes.NodeTypeAndId;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public static final String JCR_IRODS_IRODSOBJECT = "irods:irodsobject";
	public static final String JCR_IRODS_AVU_PROP = "irods:avu";
	public static final int DELIMITER_LENGTH = DELIMITER.length();
	public static final String JCR_IRODS_AVU = "irods:avu";
	public static final String JCR_AVU_SUFFIX = DELIMITER + JCR_IRODS_AVU;
	public static final int JCR_AVU_SUFFIX_LENGTH = JCR_AVU_SUFFIX.length();
	public static final int JCR_CONTENT_SUFFIX_LENGTH = JCR_CONTENT_SUFFIX
			.length();
	private static final String AVU_ID = "/avuId";

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

	public static final Logger log = LoggerFactory
			.getLogger(PathUtilities.class);

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

	/**
	 * From an id for a file, return the id that has the appropriate
	 * JCR_CONTENT_SUFFIX for a file
	 * 
	 * @param id
	 *            <code>String</code> with the modeshape id
	 * @return <code>String</code> with the id formatted with the
	 *         JCR_CONTENT_SUFFIX
	 */
	public static String formatChildIdForDocument(final String id) {
		if (id == null) {
			throw new IllegalArgumentException("null id");
		}

		return isRoot(id) ? JCR_CONTENT_SUFFIX : id + JCR_CONTENT_SUFFIX;
	}

	/**
	 * Utility method for determining if the node identifier is the identifier
	 * of the root node in this external source. Subclasses may override this
	 * method to change the format of the identifiers, but in that case should
	 * also override the {@link #fileFor(String)},
	 * {@link #isContentNode(String)}, and {@link #idFor(File)} methods.
	 * 
	 * @param id
	 *            the identifier; may not be null
	 * @return true if the identifier is for the root of this source, or false
	 *         otherwise
	 * @see #isContentNode(String)
	 * @see #fileFor(String)
	 * @see #idFor(File)
	 */
	public static boolean isRoot(final String id) {
		if (id == null) {
			throw new IllegalArgumentException("null id");
		}
		return DELIMITER.equals(id);
	}

	/**
	 * Utility method for determining the node identifier for the supplied file.
	 * Subclasses may override this method to change the format of the
	 * identifiers, but in that case should also override the
	 * {@link #fileFor(String)}, {@link #isContentNode(String)}, and
	 * {@link #isRoot(String)} methods.
	 * 
	 * @param file
	 *            the file; may not be null
	 * @return the node identifier; never null
	 * @see #isRoot(String)
	 * @see #isContentNode(String)
	 * @see #fileFor(String)
	 */
	public String idFor(final File file) {
		String path = file.getAbsolutePath();
		if (!path.startsWith(getDirectoryPathWithTrailingSlash())) {
			if (getDirectoryPath().equals(path)) {
				// This is the root
				return PathUtilities.DELIMITER;
			}

			throw new DocumentStoreException(path,
					"given path is not within the scope of this connector");
		}
		String id = path.substring(getDirectoryAbsolutePathLength());
		return id;
	}

	/**
	 * Get an object that has the node type and parsed id (without suffixes)
	 * 
	 * @param id
	 *            <code>String</code> ModeShape id
	 * @return {@link NodeTypeAndId}
	 */
	public NodeTypeAndId stripSuffixFromId(final String id) {
		log.info("stripSuffixFromId()");
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}

		String myId = id;

		if (myId.endsWith(DELIMITER)) {
			myId = id.substring(0, id.length() - DELIMITER.length());
		}

		IrodsNodeTypes nodeType = this.getNodeTypeForId(myId);

		switch (nodeType) {
		case CONTENT_NODE:
			myId = myId.substring(0, myId.length() - JCR_CONTENT_SUFFIX_LENGTH);
			break;
		case AVU_NODE:
			int idxOfAttr = myId.indexOf(AVU_ID);
			myId = myId.substring(0, idxOfAttr);
			break;
		default:
			// myId is already set
		}

		NodeTypeAndId nodeTypeAndId = new NodeTypeAndId();
		nodeTypeAndId.setIrodsNodeType(nodeType);
		nodeTypeAndId.setId(myId);
		log.info("nodeTypeAndId determined:{}", nodeTypeAndId);
		return nodeTypeAndId;
	}

}
