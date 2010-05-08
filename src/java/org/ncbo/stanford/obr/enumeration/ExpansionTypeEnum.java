package org.ncbo.stanford.obr.enumeration;

public enum ExpansionTypeEnum {
	 
	IS_A_CLOSURE(1),
	MAPPING(2),
	DISTANCE(3),
	 
	;
	private int type;

	private ExpansionTypeEnum(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	} 
	 
}
