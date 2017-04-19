package dev.kkorolyov.sqlob;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import dev.kkorolyov.sqlob.annotation.Column;
import dev.kkorolyov.sqlob.annotation.Table;

/**
 * All stub classes used for unit tests.
 */
public class Stub {
	static Random rand = new Random();
	
	/**
	 * Contains all default simple types.
	 */
	public static class BasicStub {
		private short short0;
		private int int0;
		private long long0;
		private float float0;
		private double double0;
		private BigDecimal bigDecimal0;
		
		private boolean boolean0;
		
		private String string0;
		
		private Date date0;
		private Time time0;
		private Timestamp timestamp0;
		
		/** @return	new basic stub with random values */
		public static BasicStub random() {
			return new BasicStub((byte) rand.nextInt(Byte.MAX_VALUE + 1), rand.nextBoolean(), UUID.randomUUID().toString().replaceAll("-", ""), LocalDateTime.now());
		}
		
		private BasicStub(){}
		public BasicStub(byte num, boolean bool, String string, LocalDateTime time) {
			short0 = num;
			int0 = num;
			long0 = num;
			float0 = num;
			double0 = num;
			bigDecimal0 = new BigDecimal(num);
			
			boolean0 = bool;
			
			string0 = string;
			
			date0 = Date.valueOf(time.toLocalDate());
			time0 = Time.valueOf(time.toLocalTime());
			timestamp0 = Timestamp.valueOf(time);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((bigDecimal0 == null) ? 0 : bigDecimal0.hashCode());
			result = prime * result + (boolean0 ? 1231 : 1237);
			result = prime * result + ((date0 == null) ? 0 : date0.hashCode());
			long temp = Double.doubleToLongBits(double0);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + Float.floatToIntBits(float0);
			result = prime * result + int0;
			result = prime * result + (int) (long0 ^ (long0 >>> 32));
			result = prime * result + short0;
			result = prime * result + ((string0 == null) ? 0 : string0.hashCode());
			result = prime * result + ((time0 == null) ? 0 : time0.hashCode());
			result = prime * result	+ ((timestamp0 == null) ? 0 : timestamp0.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof BasicStub))
				return false;
			
			BasicStub other = (BasicStub) obj;
			if (bigDecimal0 == null) {
				if (other.bigDecimal0 != null)
					return false;
			} else if (!bigDecimal0.equals(other.bigDecimal0))
				return false;
			if (boolean0 != other.boolean0)
				return false;
			if (date0 == null) {
				if (other.date0 != null)
					return false;
			} else if (!date0.equals(other.date0))
				return false;
			if (Double.doubleToLongBits(double0) != Double.doubleToLongBits(other.double0))
				return false;
			if (Float.floatToIntBits(float0) != Float.floatToIntBits(other.float0))
				return false;
			if (int0 != other.int0)
				return false;
			if (long0 != other.long0)
				return false;
			if (short0 != other.short0)
				return false;
			if (string0 == null) {
				if (other.string0 != null)
					return false;
			} else if (!string0.equals(other.string0))
				return false;
			if (time0 == null) {
				if (other.time0 != null)
					return false;
			} else if (!time0.equals(other.time0))
				return false;
			if (timestamp0 == null) {
				if (other.timestamp0 != null)
					return false;
			} else if (!timestamp0.equals(other.timestamp0))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "BasicStub [short0=" + short0 + ", int0="
					+ int0 + ", long0=" + long0 + ", float0=" + float0 + ", double0="
					+ double0 + ", bigDecimal0=" + bigDecimal0 + ", boolean0=" + boolean0
					+ ", string0=" + string0 + ", date0=" + date0 + ", time0="
					+ time0 + ", timestamp0=" + timestamp0 + "]";
		}
	}
	
	/**
	 * Like {@code BasicStub}, but with table and columns renamed using annotations.
	 */
	@Table("A")
	public static class AnnotatedStub {
		@Column("S")
		private short short0;
		@Column("I")
		private int int0;
		@Column("L")
		private long long0;
		@Column("F")
		private float float0;
		@Column("D")
		private double double0;
		@Column("BD")
		private BigDecimal bigDecimal0;
		
		@Column("Bo")
		private boolean boolean0;
		
		@Column("St")
		private String string0;
		
		@Column("Da")
		private Date date0;
		@Column("Ti")
		private Time time0;
		@Column("TS")
		private Timestamp timestamp0;
		
		/** @return	new basic stub with random values */
		public static AnnotatedStub random() {
			return new AnnotatedStub((byte) rand.nextInt(Byte.MAX_VALUE + 1), rand.nextBoolean(), UUID.randomUUID().toString().replaceAll("-", ""), LocalDateTime.now());
		}
		
		private AnnotatedStub(){}
		public AnnotatedStub(byte num, boolean bool, String string, LocalDateTime time) {
			short0 = num;
			int0 = num;
			long0 = num;
			float0 = num;
			double0 = num;
			bigDecimal0 = new BigDecimal(num);
			
			boolean0 = bool;
			
			string0 = string;
			
			date0 = Date.valueOf(time.toLocalDate());
			time0 = Time.valueOf(time.toLocalTime());
			timestamp0 = Timestamp.valueOf(time);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((bigDecimal0 == null) ? 0 : bigDecimal0.hashCode());
			result = prime * result + (boolean0 ? 1231 : 1237);
			result = prime * result + ((date0 == null) ? 0 : date0.hashCode());
			long temp = Double.doubleToLongBits(double0);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + Float.floatToIntBits(float0);
			result = prime * result + int0;
			result = prime * result + (int) (long0 ^ (long0 >>> 32));
			result = prime * result + short0;
			result = prime * result + ((string0 == null) ? 0 : string0.hashCode());
			result = prime * result + ((time0 == null) ? 0 : time0.hashCode());
			result = prime * result	+ ((timestamp0 == null) ? 0 : timestamp0.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof AnnotatedStub))
				return false;
			
			AnnotatedStub other = (AnnotatedStub) obj;
			if (bigDecimal0 == null) {
				if (other.bigDecimal0 != null)
					return false;
			} else if (!bigDecimal0.equals(other.bigDecimal0))
				return false;
			if (boolean0 != other.boolean0)
				return false;
			if (date0 == null) {
				if (other.date0 != null)
					return false;
			} else if (!date0.equals(other.date0))
				return false;
			if (Double.doubleToLongBits(double0) != Double.doubleToLongBits(other.double0))
				return false;
			if (Float.floatToIntBits(float0) != Float.floatToIntBits(other.float0))
				return false;
			if (int0 != other.int0)
				return false;
			if (long0 != other.long0)
				return false;
			if (short0 != other.short0)
				return false;
			if (string0 == null) {
				if (other.string0 != null)
					return false;
			} else if (!string0.equals(other.string0))
				return false;
			if (time0 == null) {
				if (other.time0 != null)
					return false;
			} else if (!time0.equals(other.time0))
				return false;
			if (timestamp0 == null) {
				if (other.timestamp0 != null)
					return false;
			} else if (!timestamp0.equals(other.timestamp0))
				return false;
			return true;
		}
	}
	
	/**
	 * Contains 1 {@code BasicStub}.
	 */
	public static class SmartStub {
		private BasicStub stub;
		
		public static SmartStub random() {
			return new SmartStub(BasicStub.random());
		}
		
		private SmartStub(){}
		public SmartStub(BasicStub stub) {
			this.stub = stub;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((stub == null) ? 0 : stub.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof SmartStub))
				return false;
			SmartStub other = (SmartStub) obj;
			if (stub == null) {
				if (other.stub != null)
					return false;
			} else if (!stub.equals(other.stub))
				return false;
			return true;
		}
	}
}
