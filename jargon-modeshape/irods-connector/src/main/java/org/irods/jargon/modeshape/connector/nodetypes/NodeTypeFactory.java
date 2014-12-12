package org.irods.jargon.modeshape.connector.nodetypes;

import javax.jcr.RepositoryException;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.modeshape.connector.IrodsNodeTypes;
import org.irods.jargon.modeshape.connector.exceptions.UnknownNodeTypeException;

/**
 * Interface for a factory to return a Modeshape {@link Document} based on its
 * id as translated to a {@link IrodsNodeTypes} enum value
 * 
 * @author Mike Conway - DICE
 * 
 */
public interface NodeTypeFactory {

	/**
	 * Given an id, return the corresponding ModeShape {@link Document}
	 * 
	 * @param id
	 *            <code>String</code> with the document id
	 * @param offset
	 *            <code>int</code> with an optional offset for any paging of
	 *            child nodes. 0 if not used.
	 * @return {@link Document}
	 * @throws UnknownNodeTypeException
	 * @throws RepositoryException
	 */
	public abstract Document instanceForId(final String id, final int offset)
			throws UnknownNodeTypeException, RepositoryException;

	/**
	 * Given a document, return the associated creator. This is used for various
	 * functions such as storing a given Document
	 * 
	 * @param document
	 * @link Document} for which the associated creator will found
	 * @return {@link AbstarctNodeTypeCreator} that supports extended operations
	 *         on the given document
	 * @throws UnknownNodeTypeException
	 * @throws RepositoryException
	 */
	public abstract AbstractNodeTypeCreator instanceCreatorForDocument(
			final Document document) throws UnknownNodeTypeException,
			RepositoryException;
}