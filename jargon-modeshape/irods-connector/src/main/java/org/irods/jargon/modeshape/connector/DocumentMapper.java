/**
 * 
 */
package org.irods.jargon.modeshape.connector;

/**
 * @author Mike Conway - DICE (www.irods.org)
 * 
 */
public class DocumentMapper {

	/**
	 * Default constuctor
	 * 
	 * @param connectorContext
	 *            {@link ConnectorContext} with access and environmental
	 *            information
	 */
	public DocumentMapper(ConnectorContext connectorContext) {
		if (connectorContext == null) {
			throw new IllegalArgumentException("null connectorContext");
		}
	}

}
