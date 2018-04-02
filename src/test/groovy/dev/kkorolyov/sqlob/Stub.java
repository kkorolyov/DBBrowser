package dev.kkorolyov.sqlob;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static dev.kkorolyov.simplespecs.SpecUtilities.randByte;
import static dev.kkorolyov.simplespecs.SpecUtilities.randString;

/**
 * All stub classes used for unit tests.
 */
public class Stub {
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

		/** @return new basic stub with random values */
		public static BasicStub random() {
			return new BasicStub(randByte(), ThreadLocalRandom.current().nextBoolean(), randString(), LocalDateTime.now());
		}

		private BasicStub() {}
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
			timestamp0.setNanos(0);
		}

		public short getShort0() {
			return short0;
		}
		public int getInt0() {
			return int0;
		}
		public long getLong0() {
			return long0;
		}
		public float getFloat0() {
			return float0;
		}
		public double getDouble0() {
			return double0;
		}
		public BigDecimal getBigDecimal0() {
			return bigDecimal0;
		}
		public boolean isBoolean0() {
			return boolean0;
		}
		public String getString0() {
			return string0;
		}
		public Date getDate0() {
			return date0;
		}
		public Time getTime0() {
			return time0;
		}
		public Timestamp getTimestamp0() {
			return timestamp0;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			BasicStub other = (BasicStub) o;

			return short0 == other.short0 &&
					int0 == other.int0 &&
					long0 == other.long0 &&
					Float.compare(other.float0, float0) == 0 &&
					Double.compare(other.double0, double0) == 0 &&
					boolean0 == other.boolean0 &&
					Objects.equals(bigDecimal0, other.bigDecimal0) &&
					Objects.equals(string0, other.string0) &&
					Objects.equals(date0, other.date0) &&
					Objects.equals(time0, other.time0) &&
					Objects.equals(timestamp0, other.timestamp0);
		}
		@Override
		public int hashCode() {
			return Objects.hash(short0, int0, long0, float0, double0, bigDecimal0, boolean0, string0, date0, time0, timestamp0);
		}

		@Override
		public String toString() {
			return "BasicStub{" +
					"short0=" + short0 +
					", int0=" + int0 +
					", long0=" + long0 +
					", float0=" + float0 +
					", double0=" + double0 +
					", bigDecimal0=" + bigDecimal0 +
					", boolean0=" + boolean0 +
					", string0='" + string0 + '\'' +
					", date0=" + date0 +
					", time0=" + time0 +
					", timestamp0=" + timestamp0 +
					'}';
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

		private SmartStub() {}
		public SmartStub(BasicStub stub) {
			this.stub = stub;
		}

		public BasicStub getStub() {
			return stub;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			SmartStub other = (SmartStub) o;

			return Objects.equals(stub, other.stub);
		}
		@Override
		public int hashCode() {
			return Objects.hash(stub);
		}

		@Override
		public String toString() {
			return "SmartStub{" +
					"stub=" + stub +
					'}';
		}
	}
}
