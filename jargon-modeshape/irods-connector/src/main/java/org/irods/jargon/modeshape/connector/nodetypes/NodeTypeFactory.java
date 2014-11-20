package org.irods.jargon.modeshape.connector.nodetypes;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.exception.JargonException;
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
	 * @throws JargonException
	 */
	public abstract Document instanceForId(final String id, final int offset)
			throws UnknownNodeTypeException, JargonException;

}