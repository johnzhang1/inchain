package org.inchain.filter;

import static java.lang.Math.E;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;

import java.util.Arrays;

public class BloomFilter {
	
    private static final int MAX_FILTER_SIZE = 10000000;
    private static final int MAX_HASH_FUNCS = 50;
    
	private byte[] data;
    private long hashFuncs;
    private long nTweak;
    
    private int elements;
    private double falsePositiveRate;
    private long randomNonce;

    public BloomFilter(int elements, double falsePositiveRate, long randomNonce) {
    	this.elements = elements;
    	this.falsePositiveRate = falsePositiveRate;
    	this.randomNonce = randomNonce;
    	init();
    }
    
    public void init() {
        int size = (int)(-1  / (pow(log(2), 2)) * elements * log(falsePositiveRate));
        size = max(1, min(size, (int) MAX_FILTER_SIZE * 8) / 8);
        data = new byte[size];
        hashFuncs = (int)(data.length * 8 / (double)elements * log(2));
        hashFuncs = max(1, min(hashFuncs, MAX_HASH_FUNCS));
        this.nTweak = randomNonce;
    }
    
    public double getFalsePositiveRate(int elements) {
        return pow(1 - pow(E, -1.0 * (hashFuncs * elements) / (data.length * 8)), hashFuncs);
    }

    @Override
    public String toString() {
        return "Bloom Filter of size " + data.length + " with " + hashFuncs + " hash functions.";
    }

    private static int rotateLeft32(int x, int r) {
        return (x << r) | (x >>> (32 - r));
    }

    public static int murmurHash3(byte[] data, long nTweak, int hashNum, byte[] object) {
        int h1 = (int)(hashNum * 0xFBA4C795L + nTweak);
        final int c1 = 0xcc9e2d51;
        final int c2 = 0x1b873593;

        int numBlocks = (object.length / 4) * 4;
        // body
        for(int i = 0; i < numBlocks; i += 4) {
            int k1 = (object[i] & 0xFF) |
                  ((object[i+1] & 0xFF) << 8) |
                  ((object[i+2] & 0xFF) << 16) |
                  ((object[i+3] & 0xFF) << 24);
            
            k1 *= c1;
            k1 = rotateLeft32(k1, 15);
            k1 *= c2;

            h1 ^= k1;
            h1 = rotateLeft32(h1, 13);
            h1 = h1*5+0xe6546b64;
        }
        
        int k1 = 0;
        switch(object.length & 3)
        {
            case 3:
                k1 ^= (object[numBlocks + 2] & 0xff) << 16;
                // Fall through.
            case 2:
                k1 ^= (object[numBlocks + 1] & 0xff) << 8;
                // Fall through.
            case 1:
                k1 ^= (object[numBlocks] & 0xff);
                k1 *= c1; k1 = rotateLeft32(k1, 15); k1 *= c2; h1 ^= k1;
                // Fall through.
            default:
                // Do nothing.
                break;
        }

        // finalization
        h1 ^= object.length;
        h1 ^= h1 >>> 16;
        h1 *= 0x85ebca6b;
        h1 ^= h1 >>> 13;
        h1 *= 0xc2b2ae35;
        h1 ^= h1 >>> 16;
        
        return (int)((h1&0xFFFFFFFFL) % (data.length * 8));
    }
    
    public synchronized boolean contains(byte[] object) {
        for (int i = 0; i < hashFuncs; i++) {
            if (!checkBitLE(data, murmurHash3(data, nTweak, i, object)))
                return false;
        }
        return true;
    }
    
    /** Insert the given arbitrary data into the filter */
    public synchronized void insert(byte[] object) {
        for (int i = 0; i < hashFuncs; i++)
            setBitLE(data, murmurHash3(data, nTweak, i, object));
    }

    public synchronized boolean matchesAll() {
        for (byte b : data)
            if (b != (byte) 0xff)
                return false;
        return true;
    }

    @Override
    public synchronized boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BloomFilter other = (BloomFilter) o;
        return hashFuncs == other.hashFuncs && nTweak == other.nTweak && Arrays.equals(data, other.data);
    }
    
    private static final int[] bitMask = {0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80};
    
    public static boolean checkBitLE(byte[] data, int index) {
        return (data[index >>> 3] & bitMask[7 & index]) != 0;
    }

    public static void setBitLE(byte[] data, int index) {
        data[index >>> 3] |= bitMask[7 & index];
    }
    
//    public byte[] getData() {
//		return data;
//	}
//	public long getHashFuncs() {
//		return hashFuncs;
//	}
//    public static void main(String[] args) {
//		BloomFilter filter = new BloomFilter(1000000, 0.0001, 15645646);
//		
//		byte[] d1 = Hex.decode("cf9dd4e42b5cddb1bf49efbb6cf3525e3e81b6fd8c18c107e121ff576cf8b3a3");
//		System.out.println(filter.contains(d1));
//		
//		filter.insert(d1);
//		
//		System.out.println(filter.contains(d1));
//		System.out.println(filter.getData().length);
//		System.out.println(filter.getHashFuncs());
//	}
}
