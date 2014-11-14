/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.spi.federation.DocumentChanges;
import org.modeshape.jcr.spi.federation.WritableConnector;
import org.modeshape.jcr.value.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Conway - DICE ref
 *         https://github.com/ModeShape/modeshape/tree/master
 *         /connectors/modeshape
 *         -connector-git/src/main/java/org/modeshape/connector/git
 */
public class IrodsWriteableConnector extends WritableConnector {

	/**
	 * The string path for a {@link File} object that represents the top-level
	 * directory accessed by this connector. This is set via reflection and is
	 * required for this connector.
	 */
	private String directoryPath;

	public static final Logger log = LoggerFactory
			.getLogger(IrodsWriteableConnector.class);

	/**
	 * A boolean flag that specifies whether this connector should add the
	 * 'mix:mimeType' mixin to the 'nt:resource' nodes to include the
	 * 'jcr:mimeType' property. If set to <code>true</code>, the MIME type is
	 * computed immediately when the 'nt:resource' node is accessed, which might
	 * be expensive for larger files. This is <code>false</code> by default.
	 */
	private boolean addMimeTypeMixin = false;

	/**
	 * The regular expression that, if matched by a file or folder, indicates
	 * that the file or folder should be included.
	 */
	private String inclusionPattern;

	/**
	 * The regular expression that, if matched by a file or folder, indicates
	 * that the file or folder should be ignored.
	 */
	private String exclusionPattern;

	/**
	 * Configuration flag that can cause this connector to operare in read-only
	 * mode if desired
	 */
	private boolean readOnly;

	/**
	 * The {@link FilenameFilter} implementation that is instantiated in the
	 * {@link #initialize(NamespaceRegistry, NodeTypeManager)} method.
	 */
	private InclusionExclusionFilenameFilter filenameFilter;

	/**
	 * Created during init phase, this will handle all the node type detection
	 * and path munging for ids
	 */
	private PathUtilities pathUtilities;

	@Override
	public Document getDocumentById(String id) {

		log.info("getDocumentById()");

		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}

		log.info("id:{}", id);

		IrodsNodeTypes irodsNodeType = pathUtilities.getNodeTypeForId(id);
		log.info("node type:{}", irodsNodeType);

		return null;

	}

	@Override
	public String getDocumentId(String arg0) {
		return null;
	}

	@Override
	public Collection<String> getDocumentPathsById(String arg0) {
		return null;
	}

	@Override
	public boolean hasDocument(String arg0) {
		return false;
	}

	@Override
	public String newDocumentId(String arg0, Name arg1, Name arg2) {
		return null;
	}

	@Override
	public boolean removeDocument(String arg0) {
		return false;
	}

	@Override
	public void storeDocument(Document arg0) {

	}

	@Override
	public void updateDocument(DocumentChanges arg0) {

	}

	/**
	 * @return the addMimeTypeMixin
	 */
	public boolean isAddMimeTypeMixin() {
		return addMimeTypeMixin;
	}

	/**
	 * @param addMimeTypeMixin
	 *            the addMimeTypeMixin to set
	 */
	public void setAddMimeTypeMixin(boolean addMimeTypeMixin) {
		this.addMimeTypeMixin = addMimeTypeMixin;
	}

	/**
	 * @return the inclusionPattern
	 */
	public String getInclusionPattern() {
		return inclusionPattern;
	}

	/**
	 * @param inclusionPattern
	 *            the inclusionPattern to set
	 */
	public void setInclusionPattern(String inclusionPattern) {
		this.inclusionPattern = inclusionPattern;
	}

	/**
	 * @return the exclusionPattern
	 */
	public String getExclusionPattern() {
		return exclusionPattern;
	}

	/**
	 * @param exclusionPattern
	 *            the exclusionPattern to set
	 */
	public void setExclusionPattern(String exclusionPattern) {
		this.exclusionPattern = exclusionPattern;
	}

	/**
	 * @return the readOnly
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * @param readOnly
	 *            the readOnly to set
	 */
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.spi.federation.Connector#initialize(javax.jcr.
	 * NamespaceRegistry, org.modeshape.jcr.api.nodetype.NodeTypeManager)
	 */
	@Override
	public void initialize(NamespaceRegistry registry,
			NodeTypeManager nodeTypeManager) throws RepositoryException,
			IOException {
		super.initialize(registry, nodeTypeManager);

		checkFieldNotNull(directoryPath, "directoryPath");

		// Initialize the filename filter ...
		filenameFilter = new InclusionExclusionFilenameFilter();
		if (exclusionPattern != null)
			filenameFilter.setExclusionPattern(exclusionPattern);
		if (inclusionPattern != null)
			filenameFilter.setInclusionPattern(inclusionPattern);

		this.pathUtilities = new PathUtilities(directoryPath, filenameFilter);

		log.info("initialized");
	}

	/**
	 * @return the directoryPath
	 */
	public String getDirectoryPath() {
		return directoryPath;
	}

	/**
	 * @param directoryPath
	 *            the directoryPath to set
	 */
	public void setDirectoryPath(String directoryPath) {
		this.directoryPath = directoryPath;
	}

}
