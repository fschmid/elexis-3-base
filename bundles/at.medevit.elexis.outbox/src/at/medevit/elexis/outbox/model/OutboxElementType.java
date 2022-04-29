package at.medevit.elexis.outbox.model;

import org.apache.commons.lang3.StringUtils;

public enum OutboxElementType {
	FILE("FILE://"), DB(StringUtils.EMPTY), DOC("DOC://");

	private final String prefix;

	private OutboxElementType(String prefix) {
		this.prefix = prefix;
	}

	public String getPrefix() {
		return prefix;
	}

	public static OutboxElementType parseType(String uri) {
		if (uri != null) {
			if (uri.startsWith(FILE.getPrefix())) {
				return FILE;

			} else if (uri.startsWith(DOC.getPrefix())) {
				return DOC;
			} else if (uri.contains("::")) {
				return DB;
			}
		}
		return null;
	}
}
