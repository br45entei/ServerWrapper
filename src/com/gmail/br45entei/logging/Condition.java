package com.gmail.br45entei.logging;

/** @author Brian_Entei
 * @param <T> The object type to compare */
public class Condition<T> {
	protected T	condition1;
	protected T	condition2;
	
	/** @param condition1 The first value to check later
	 * @param condition2 The second value to check later */
	public Condition(T condition1, T condition2) {
		this.condition1 = condition1;
		this.condition2 = condition2;
	}
	
	/** @return Whether or not this conditions first value matches its second
	 *         value */
	public boolean isTrue() {
		return this.condition1.equals(this.condition2);
	}
	
	/** Utility class used to store a String value for later use
	 * 
	 * @author Brian_Entei */
	public static final class StringValue {
		private String value;
		
		/** The default constructor */
		public StringValue() {
			this("");
		}
		
		/** @param value The String value to set */
		public StringValue(String value) {
			this.value = value;
		}
		
		/** @return This StringValues value */
		public String getValue() {
			return this.value;
		}
		
		/** @param value The new String value to set
		 * @return This StringValue */
		public StringValue setValue(String value) {
			this.value = value;
			return this;
		}
	}
	
	/** Utility class used to store a boolean value for later use
	 * 
	 * @author Brian_Entei */
	public static final class BooleanValue {
		/** A special {@link BooleanValue} whose value is locked and cannot
		 * change; this ones value is set to true */
		public static final BooleanValue	TRUE			= new BooleanValue(true).lockValue();
		/** A special {@link BooleanValue} whose value is locked and cannot
		 * change; this ones value is set to false */
		public static final BooleanValue	FALSE			= new BooleanValue(false).lockValue();
		private boolean						isValueLocked	= false;
		private boolean						value;
		
		/** @param value The value to set */
		public BooleanValue(boolean value) {
			this.value = value;
		}
		
		/** @return This BooleanValues value */
		public boolean getValue() {
			return this.value;
		}
		
		/** @param value The new value to set
		 * @return This BooleanValue */
		public BooleanValue setValue(boolean value) {
			if(!this.isValueLocked) {
				this.value = value;
			}
			return this;
		}
		
		private BooleanValue lockValue() {
			this.isValueLocked = true;
			return this;
		}
		
	}
	
	/** Utility class used to store and compare two BooleanValues at a later
	 * time(so that if one of the BooleanValues value were to change this class'
	 * isTrue() method would return a different result because of it)
	 * 
	 * @author Brian_Entei */
	public static final class BooleanCondition extends Condition<BooleanValue> {
		
		/** @param condition1 The first value to check later
		 * @param condition2 The second value to check later */
		public BooleanCondition(BooleanValue condition1, BooleanValue condition2) {
			super(condition1, condition2);
		}
		
		@Override
		public boolean isTrue() {
			return this.condition1.getValue() == this.condition2.getValue();
		}
		
	}
	
}
