/**
 * 
 */
package org.irods.jargon.modeshape.connector;

/**
 * Enumerates supported node types and their path information
 * 
 * @author Mike Conway - DICE
 * 
 */
public enum IrodsNodeTypes {

	UNKNOWN("UNKNOWN"), ROOT_NODE(PathUtilities.DELIMITER), CONTENT_NODE(
			"nt:resource"), AVU_NODE("irods:avu");

	private String nodeTypeIdSuffix;

	IrodsNodeTypes(String nodeTypeIdSuffix) {
		this.nodeTypeIdSuffix = nodeTypeIdSuffix;
	}

	/**
	 * @return the nodeTypeIdSuffix
	 */
	public String getNodeTypeIdSuffix() {
		return nodeTypeIdSuffix;
	}

	/**
	 * Given the id, look at the suffix to determine the type
	 * 
	 * @param id
	 * @return
	 */
	public static IrodsNodeTypes determineNodeTypeFromId(final String id) {
		if (PathUtilities.DELIMITER.equals(id)) {
			return ROOT_NODE;
		} else if (id.endsWith(CONTENT_NODE.nodeTypeIdSuffix)) {
			return CONTENT_NODE;
		} else if (id.endsWith(AVU_NODE.nodeTypeIdSuffix)) {
			return AVU_NODE;
		}

		return UNKNOWN;
	}

	/**
	 * Return the correct node type suffix with the prepended delimiter
	 * 
	 * @param irodsNodeType
	 * @return
	 */
	public static String buildSuffixWithDelimiter(IrodsNodeTypes irodsNodeType) {
		if (irodsNodeType == null) {
			throw new IllegalArgumentException("null irodsNodeType");
		}
		StringBuilder sb = new StringBuilder();
		sb.append(PathUtilities.DELIMITER);
		sb.append(irodsNodeType.nodeTypeIdSuffix);
		return sb.toString();

	}

}
