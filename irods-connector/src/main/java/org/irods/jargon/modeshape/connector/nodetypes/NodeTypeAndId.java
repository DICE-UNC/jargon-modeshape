/**
 * 
 */
package org.irods.jargon.modeshape.connector.nodetypes;

import org.irods.jargon.modeshape.connector.IrodsNodeTypes;

/**
 * Value object holding node type and id with node type stripped out
 * 
 * @author Mike Conway - DICE
 * 
 */
public class NodeTypeAndId {

	private String id;
	private IrodsNodeTypes irodsNodeType;

	/**
	 * 
	 */
	public NodeTypeAndId() {
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(final String id) {
		this.id = id;
	}

	/**
	 * @return the irodsNodeType
	 */
	public IrodsNodeTypes getIrodsNodeType() {
		return irodsNodeType;
	}

	/**
	 * @param irodsNodeType
	 *            the irodsNodeType to set
	 */
	public void setIrodsNodeType(final IrodsNodeTypes irodsNodeType) {
		this.irodsNodeType = irodsNodeType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NodeTypeAndId [");
		if (id != null) {
			builder.append("id=");
			builder.append(id);
			builder.append(", ");
		}
		if (irodsNodeType != null) {
			builder.append("irodsNodeType=");
			builder.append(irodsNodeType);
		}
		builder.append("]");
		return builder.toString();
	}

}
