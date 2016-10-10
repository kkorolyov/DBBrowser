package dev.kkorolyov.sqlob.connection;

/**
 * Denotes a mapping between a Java object class and a relational database SQL type.
 */
public class SqlobType {
	private Class<?> typeClass;
	private String typeName;
	private int typeCode;
	
	/**
	 * Constructs a new type.
	 * @param typeClass Java class
	 * @param typeName SQL name
	 * @param typeCode JDBC constant from {@link java.sql.Types}
	 */
	public SqlobType(Class<?> typeClass, String typeName, int typeCode) {
		this.typeClass = typeClass;
		this.typeName = typeName;
		this.typeCode = typeCode;
	}
	
	/** @return Java class */
	public Class<?> getTypeClass() {
		return typeClass;
	}
	/** @return SQL name */
	public String getTypeName() {
		return typeName;
	}
	/** @return JDBC constant from {@link java.sql.Types} */
	public int getTypeCode() {
		return typeCode;
	}
	
	@Override
	public int hashCode() {
		int result = 1,
				prime = 31;
		
		result = result * prime + typeClass.hashCode();
		result = result * prime + typeName.hashCode();
		result = result * prime + typeCode;
		
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (!(obj instanceof SqlobType))
			return false;
		
		SqlobType o = (SqlobType) obj;
		if (typeClass == null) {
			if (o.typeClass != null)
				return false;
		} else if (!typeClass.equals(o.typeClass))
			return false;
		if (typeName == null) {
			if (o.typeName != null)
				return false;
		} else if (!typeName.equals(o.typeName))
			return false;
		if (typeCode != o.typeCode)
			return false;
		
		return true;
	}
	
	@Override
	public String toString() {
		return "Java=" + typeClass.getName() + ", SQL=" + typeName + ", JDBC=" + typeCode;
	}
}
