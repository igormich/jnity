package jnity.properties;

import properties.Property3d;

public interface EditorProperty extends Property3d{
	
	public default boolean isTransient() {
		return true;
	}
}
