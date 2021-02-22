/*
 * Copyright 2010, Silvio Heuberger @ IFS www.ifs.hsr.ch
 *
 * This code is release under the Apache License 2.0.
 * You should have received a copy of the license
 * in the LICENSE file. If you have not, see
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.geohash;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.geom.Geom;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

public final class GeoHash implements Comparable<GeoHash>, Serializable {
	private static final int MAX_BIT_PRECISION = 64;
	private static final int MAX_CHARACTER_PRECISION = 12;
	
	private static final long serialVersionUID = -8553214249630252175L;
	private static final int[] BITS = { 16, 8, 4, 2, 1 };
	private static final int BASE32_BITS = 5;
	public static final long FIRST_BIT_FLAGGED = 0x8000000000000000L;
	private static final char[] base32 = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'b', 'c', 'd', 'e', 'f',
			'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

	private final static Map<Character, Integer> decodeMap = new HashMap<>();

	static {
		int sz = base32.length;
		for (int i = 0; i < sz; i++) {
			decodeMap.put(base32[i], i);
		}
	}

	protected long bits = 0;
	private Point point;

	private Envelope envelope;

	protected byte significantBits = 0;

	public GeoHash() {
	}

	/**
	 * This method uses the given number of characters as the desired precision
	 * value. The hash can only be 64bits long, thus a maximum precision of 12
	 * characters can be achieved.
	 */
	public static String encode(double latitude, double longitude, int numberOfCharacters) {
		if (numberOfCharacters > MAX_CHARACTER_PRECISION) {
			throw new IllegalArgumentException("A geohash can only be " + MAX_CHARACTER_PRECISION + " character long.");
		}
		int desiredPrecision = (numberOfCharacters * 5 <= 60) ? numberOfCharacters * 5 : 60;
		GeoHash hash = new GeoHash(latitude, longitude, desiredPrecision);
		return hash.toBase32();
	}

	/**
	 * create a new {@link GeoHash} with the given number of bits accuracy. This
	 * at the same time defines this hash's bounding box.
	 */
	public static GeoHash withBitPrecision(double latitude, double longitude, int numberOfBits) {
		if (numberOfBits > MAX_BIT_PRECISION) {
			throw new IllegalArgumentException("A Geohash can only be " + MAX_BIT_PRECISION + " bits long!");
		}
		if (Math.abs(latitude) > 90.0 || Math.abs(longitude) > 180.0) {
			throw new IllegalArgumentException("Can't have lat/lon values out of (-90,90)/(-180/180)");
		}
		return new GeoHash(latitude, longitude, numberOfBits);
	}

	public static Set<String> geoHashesPolygon(Polygon polygon, int hashLength){
		Envelope envelope = polygon.getEnvelopeInternal();
		Set<String> set = new HashSet<>();
		double southLat = envelope.getMinY();
		double northLat = envelope.getMaxY();
		double westLon = envelope.getMinX();
		double eastLon = envelope.getMaxX();
		String rowHash = encode(southLat,westLon, hashLength);
		Envelope rowBox = decode(rowHash).envelope;
		while(rowBox.getMinY() < northLat){
			String columnHash = rowHash;
			Envelope columnBox = rowBox;
			while (isWest(columnBox.getMinX(), eastLon)){
				Geometry geoHashEvp = Geom.polygon(columnBox.getMinX(),columnBox.getMinY(),columnBox.getMaxX(),columnBox.getMinY(),columnBox.getMaxX(),columnBox.getMaxY(),columnBox.getMinX(),columnBox.getMaxY(),columnBox.getMinX(),columnBox.getMinY());
				if(polygon.intersects(geoHashEvp)){
					set.add(columnHash);
				}
				// move column east
				GeoHash column = decode(columnHash).getEasternNeighbour();
				columnHash = column.toBase32();
				columnBox = column.envelope;
			}
			// move row north
			GeoHash row = decode(rowHash).getNorthernNeighbour();
			rowHash = row.toBase32();
			rowBox = row.envelope;
		}
		return set;
	}

	public static GeoHash decodeBinary(String binaryString) {
		GeoHash geohash = new GeoHash();
		for (int i = 0; i < binaryString.length(); i++) {
			if (binaryString.charAt(i) == '1') {
				geohash.addOnBitToEnd();
			} else if (binaryString.charAt(i) == '0') {
				geohash.addOffBitToEnd();
			} else {
				throw new IllegalArgumentException(binaryString + " is not a valid geohash as a binary string");
			}
		}
		geohash.bits <<= (MAX_BIT_PRECISION - geohash.significantBits);
		long[] latitudeBits = geohash.getRightAlignedLatitudeBits();
		long[] longitudeBits = geohash.getRightAlignedLongitudeBits();
		return geohash.recombineLatLonBitsToHash(latitudeBits, longitudeBits);
	}

	/**
	 * build a new {@link GeoHash} from a base32-encoded {@link String}.<br>
	 * This will also set up the hashes bounding box and other values, so it can
	 * also be used with functions like within().
	 */
	public static GeoHash decode(String geohash) {
		double[] latitudeRange = { -90.0, 90.0 };
		double[] longitudeRange = { -180.0, 180.0 };

		boolean isEvenBit = true;
		GeoHash hash = new GeoHash();

		for (int i = 0; i < geohash.length(); i++) {
			int cd = decodeMap.get(geohash.charAt(i));
			for (int j = 0; j < BASE32_BITS; j++) {
				int mask = BITS[j];
				if (isEvenBit) {
					divideRangeDecode(hash, longitudeRange, (cd & mask) != 0);
				} else {
					divideRangeDecode(hash, latitudeRange, (cd & mask) != 0);
				}
				isEvenBit = !isEvenBit;
			}
		}

		double latitude = (latitudeRange[0] + latitudeRange[1]) / 2;
		double longitude = (longitudeRange[0] + longitudeRange[1]) / 2;

		hash.point = Geom.point(longitude, latitude);
		setEnvelope(hash, latitudeRange, longitudeRange);
		hash.bits <<= (MAX_BIT_PRECISION - hash.significantBits);
		return hash;
	}

	public static Polygon decodePolygon(String geoHash){
		Envelope evp = decode(geoHash).envelope;
		Polygon pg = Geom.polygon(evp.getMinX(),evp.getMinY(),evp.getMaxX(),evp.getMinY(),evp.getMaxX(),evp.getMaxX(),evp.getMinX(),evp.getMaxY(),evp.getMinX(),evp.getMinY());
		return pg;
	}

	public static GeoHash decodeLong(long hashVal, int significantBits) {
		double[] latitudeRange = { -90.0, 90.0 };
		double[] longitudeRange = { -180.0, 180.0 };

		boolean isEvenBit = true;
		GeoHash hash = new GeoHash();

		String binaryString = Long.toBinaryString(hashVal);
		while (binaryString.length() < MAX_BIT_PRECISION) {
			binaryString = "0" + binaryString;
		}
		for (int j = 0; j < significantBits; j++) {
			if (isEvenBit) {
				divideRangeDecode(hash, longitudeRange, binaryString.charAt(j) != '0');
			} else {
				divideRangeDecode(hash, latitudeRange, binaryString.charAt(j) != '0');
			}
			isEvenBit = !isEvenBit;
		}

		double latitude = (latitudeRange[0] + latitudeRange[1]) / 2;
		double longitude = (longitudeRange[0] + longitudeRange[1]) / 2;

		hash.point = Geom.point(longitude, latitude);
		setEnvelope(hash, latitudeRange, longitudeRange);
		hash.bits <<= (MAX_BIT_PRECISION - hash.significantBits);
		return hash;
	}

	private static void setEnvelope(GeoHash hash, double[] latitudeRange, double[] longitudeRange) {
		hash.envelope = Geom.envelope(longitudeRange[0], latitudeRange[0], longitudeRange[1], latitudeRange[1]);
	}

	public static GeoHash decodeOrd(long ord, int significantBits) {
		int insignificantBits = MAX_BIT_PRECISION - significantBits;
		return decodeLong(ord << insignificantBits, significantBits);
	}

	/**
	 * Counts the number of geohashes contained between the two (ie how many
	 * times next() is called to increment from one to two) This value depends
	 * on the number of significant bits.
	 * 
	 * @param one
	 * @param two
	 * @return number of steps
	 */
	public static long stepsBetween(GeoHash one, GeoHash two) {
		if (one.significantBits() != two.significantBits()) {
			throw new IllegalArgumentException(
					"It is only valid to compare the number of steps between two hashes if they have the same number of significant bits");
		}
		return two.ord() - one.ord();
	}

	private static void divideRangeDecode(GeoHash hash, double[] range, boolean b) {
		double mid = (range[0] + range[1]) / 2;
		if (b) {
			hash.addOnBitToEnd();
			range[0] = mid;
		} else {
			hash.addOffBitToEnd();
			range[1] = mid;
		}
	}

	private static boolean isWest(double lon1, double lon2){
		double ll1 = lon1 + 180;
		double ll2 = lon2 + 180;
		if(ll1 < ll2 && (ll2 - ll1)<180){
			return true;
		}else{
			return (ll1>ll2 && (ll2 + 360 - ll1)<180);
		}
	}

	private static String padLeft(String s, int n, String pad) {
		return String.format("%" + n + "s", s).replace(" ", pad);
	}

	private GeoHash(double latitude, double longitude, int desiredPrecision) {
		point = Geom.point(longitude, latitude);
		desiredPrecision = Math.min(desiredPrecision, MAX_BIT_PRECISION);

		boolean isEvenBit = true;
		double[] latitudeRange = { -90, 90 };
		double[] longitudeRange = { -180, 180 };

		while (significantBits < desiredPrecision) {
			if (isEvenBit) {
				divideRangeEncode(longitude, longitudeRange);
			} else {
				divideRangeEncode(latitude, latitudeRange);
			}
			isEvenBit = !isEvenBit;
		}

		setEnvelope(this, latitudeRange, longitudeRange);
		bits <<= (MAX_BIT_PRECISION - desiredPrecision);
	}

	public GeoHash next(int step) {
		return decodeOrd(ord() + step, significantBits);
	}

	public GeoHash next() {
		return next(1);
	}

	public GeoHash prev() {
		return next(-1);
	}

	public long ord() {
		int insignificantBits = MAX_BIT_PRECISION - significantBits;
		return bits >>> insignificantBits;
	}

	/**
	 * Returns the number of characters that represent this hash.
	 *
	 * @throws IllegalStateException
	 *             when the hash cannot be encoded in base32, i.e. when the
	 *             precision is not a multiple of 5.
	 */
	public int getCharacterPrecision() {
		if (significantBits % 5 != 0) {
			throw new IllegalStateException(
					"precision of GeoHash is not divisble by 5: " + this);
		}
		return significantBits / 5;
	}

	private void divideRangeEncode(double value, double[] range) {
		double mid = (range[0] + range[1]) / 2;
		if (value >= mid) {
			addOnBitToEnd();
			range[0] = mid;
		} else {
			addOffBitToEnd();
			range[1] = mid;
		}
	}

	/**
	 * returns the 8 adjacent hashes for this one. They are in the following
	 * order:<br>
	 * N, NE, E, SE, S, SW, W, NW
	 */
	public GeoHash[] getAdjacent() {
		GeoHash northern = getNorthernNeighbour();
		GeoHash eastern = getEasternNeighbour();
		GeoHash southern = getSouthernNeighbour();
		GeoHash western = getWesternNeighbour();
		return new GeoHash[] { northern, northern.getEasternNeighbour(), eastern, southern.getEasternNeighbour(),
				southern,
				southern.getWesternNeighbour(), western, northern.getWesternNeighbour() };
	}

	/**
	 * how many significant bits are there in this {@link GeoHash}?
	 */
	public int significantBits() {
		return significantBits;
	}

	public long longValue() {
		return bits;
	}

	/**
	 * get the base32 string for this {@link GeoHash}.<br>
	 * this method only makes sense, if this hash has a multiple of 5
	 * significant bits.
	 * 
	 * @throws IllegalStateException
	 *             when the number of significant bits is not a multiple of 5.
	 */
	public String toBase32() {
		if (significantBits % 5 != 0) {
			throw new IllegalStateException("Cannot convert a geohash to base32 if the precision is not a multiple of 5.");
		}
		StringBuilder buf = new StringBuilder();

		long firstFiveBitsMask = 0xf800000000000000L;
		long bitsCopy = bits;
		int partialChunks = (int) Math.ceil(((double) significantBits / 5));

		for (int i = 0; i < partialChunks; i++) {
			int pointer = (int) ((bitsCopy & firstFiveBitsMask) >>> 59);
			buf.append(base32[pointer]);
			bitsCopy <<= 5;
		}
		return buf.toString();
	}

	/**
	 * returns true iff this is within the given geohash bounding box.
	 */
	public boolean within(GeoHash envelope) {
		return (bits & envelope.mask()) == envelope.bits;
	}

	/**
	 * find out if the given point lies within this hashes bounding box.<br>
	 * <i>Note: this operation checks the bounding boxes coordinates, i.e. does
	 * not use the {@link GeoHash}s special abilities.s</i>
	 */
	public boolean contains(Point point) {
		return envelope.contains(point.getCoordinate());
	}

	/**
	 * returns the {@link Point} that was originally used to set up this.<br>
	 * If it was built from a base32-{@link String}, this is the center point of
	 * the bounding box.
	 */
	public Point getOriginatingPoint() {
		return point;
	}

	/**
	 * return the center of this {@link GeoHash}s bounding box. this is rarely
	 * the same point that was used to build the hash.
	 */
	public Point getEnvelopeCenter() {
		return Geom.point(envelope.centre());
	}

	public Envelope getEnvelope() {
		return envelope;
	}

	public boolean enclosesCircleAroundPoint(Point point, double radius) {
		return false;
	}

	protected GeoHash recombineLatLonBitsToHash(long[] latBits, long[] lonBits) {
		GeoHash hash = new GeoHash();
		boolean isEvenBit = false;
		latBits[0] <<= (MAX_BIT_PRECISION - latBits[1]);
		lonBits[0] <<= (MAX_BIT_PRECISION - lonBits[1]);
		double[] latitudeRange = { -90.0, 90.0 };
		double[] longitudeRange = { -180.0, 180.0 };

		for (int i = 0; i < latBits[1] + lonBits[1]; i++) {
			if (isEvenBit) {
				divideRangeDecode(hash, latitudeRange, (latBits[0] & FIRST_BIT_FLAGGED) == FIRST_BIT_FLAGGED);
				latBits[0] <<= 1;
			} else {
				divideRangeDecode(hash, longitudeRange, (lonBits[0] & FIRST_BIT_FLAGGED) == FIRST_BIT_FLAGGED);
				lonBits[0] <<= 1;
			}
			isEvenBit = !isEvenBit;
		}
		hash.bits <<= (MAX_BIT_PRECISION - hash.significantBits);
		setEnvelope(hash, latitudeRange, longitudeRange);
		hash.point = Geom.point(hash.envelope.centre());
		return hash;
	}

	public GeoHash getNorthernNeighbour() {
		long[] latitudeBits = getRightAlignedLatitudeBits();
		long[] longitudeBits = getRightAlignedLongitudeBits();
		latitudeBits[0] += 1;
		latitudeBits[0] = maskLastNBits(latitudeBits[0], latitudeBits[1]);
		return recombineLatLonBitsToHash(latitudeBits, longitudeBits);
	}

	public GeoHash getSouthernNeighbour() {
		long[] latitudeBits = getRightAlignedLatitudeBits();
		long[] longitudeBits = getRightAlignedLongitudeBits();
		latitudeBits[0] -= 1;
		latitudeBits[0] = maskLastNBits(latitudeBits[0], latitudeBits[1]);
		return recombineLatLonBitsToHash(latitudeBits, longitudeBits);
	}

	public GeoHash getEasternNeighbour() {
		long[] latitudeBits = getRightAlignedLatitudeBits();
		long[] longitudeBits = getRightAlignedLongitudeBits();
		longitudeBits[0] += 1;
		longitudeBits[0] = maskLastNBits(longitudeBits[0], longitudeBits[1]);
		return recombineLatLonBitsToHash(latitudeBits, longitudeBits);
	}

	public GeoHash getWesternNeighbour() {
		long[] latitudeBits = getRightAlignedLatitudeBits();
		long[] longitudeBits = getRightAlignedLongitudeBits();
		longitudeBits[0] -= 1;
		longitudeBits[0] = maskLastNBits(longitudeBits[0], longitudeBits[1]);
		return recombineLatLonBitsToHash(latitudeBits, longitudeBits);
	}

	protected long[] getRightAlignedLatitudeBits() {
		long copyOfBits = bits << 1;
		long value = extractEverySecondBit(copyOfBits, getNumberOfLatLonBits()[0]);
		return new long[] { value, getNumberOfLatLonBits()[0] };
	}

	protected long[] getRightAlignedLongitudeBits() {
		long copyOfBits = bits;
		long value = extractEverySecondBit(copyOfBits, getNumberOfLatLonBits()[1]);
		return new long[] { value, getNumberOfLatLonBits()[1] };
	}

	private long extractEverySecondBit(long copyOfBits, int numberOfBits) {
		long value = 0;
 		for (int i = 0; i < numberOfBits; i++) {
			if ((copyOfBits & FIRST_BIT_FLAGGED) == FIRST_BIT_FLAGGED) {
				value |= 0x1;
			}
			value <<= 1;
			copyOfBits <<= 2;
		}
		value >>>= 1;
		return value;
	}

	protected int[] getNumberOfLatLonBits() {
		if (significantBits % 2 == 0) {
			return new int[] { significantBits / 2, significantBits / 2 };
		} else {
			return new int[] { significantBits / 2, significantBits / 2 + 1 };
		}
	}

	protected final void addOnBitToEnd() {
		significantBits++;
		bits <<= 1;
		bits = bits | 0x1;
	}

	protected final void addOffBitToEnd() {
		significantBits++;
		bits <<= 1;
	}

	public String toBinaryString() {
		StringBuilder bui = new StringBuilder();
		long bitsCopy = bits;
		for (int i = 0; i < significantBits; i++) {
			if ((bitsCopy & FIRST_BIT_FLAGGED) == FIRST_BIT_FLAGGED) {
				bui.append('1');
			} else {
				bui.append('0');
			}
			bitsCopy <<= 1;
		}
		return bui.toString();
	}

	/**
	 * return a long mask for this hashes significant bits.
	 */
	private long mask() {
		if (significantBits == 0) {
			return 0;
		} else {
			long value = FIRST_BIT_FLAGGED;
			value >>= (significantBits - 1);
			return value;
		}
	}

	private long maskLastNBits(long value, long n) {
		long mask = 0xffffffffffffffffL;
		mask >>>= (MAX_BIT_PRECISION - n);
		return value & mask;
	}

	@Override
	public String toString() {
		if (significantBits % 5 == 0) {
			return String.format("%s -> %s -> %s", padLeft(Long.toBinaryString(bits), 64, "0"), envelope, toBase32());
		} else {
			return String.format("%s -> %s, bits: %d", padLeft(Long.toBinaryString(bits), 64, "0"), envelope, significantBits);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof GeoHash) {
			GeoHash other = (GeoHash) obj;
			if (other.significantBits == significantBits && other.bits == bits) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		int f = 17;
		f = 31 * f + (int) (bits ^ (bits >>> 32));
		f = 31 * f + significantBits;
		return f;
	}

	@Override
	public int compareTo(GeoHash o) {
		int bitsCmp = Long.compare(bits ^ FIRST_BIT_FLAGGED, o.bits ^ FIRST_BIT_FLAGGED);
		if (bitsCmp != 0) {
			return bitsCmp;
		} else {
			return Integer.compare(significantBits, o.significantBits);
		}
	}
}
