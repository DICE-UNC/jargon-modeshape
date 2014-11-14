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
	 * @return {@link Document}
	 * @throws JargonException
	 */
	public abstract Document instanceForId(String id)
			throws UnknownNodeTypeException, JargonException;

}