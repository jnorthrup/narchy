package jcog.io.lz;


import jcog.util.ArrayUtil;

public final class QuickLZ {


	public final static int QLZ_POINTERS_1 = 1;
	public final static int QLZ_POINTERS_3 = 16;
	private final static int HASH_VALUES = 4096;
	private final static int MINOFFSET = 2;
	private final static int UNCONDITIONAL_MATCHLEN = 6;
	private final static int UNCOMPRESSED_END = 4;
	private final static int CWORD_LEN = 4;
	private final static int DEFAULT_HEADERLEN = 9;

	static int headerLen(byte[] source) {
		return headerLong(source) ? 9 : 3;
	}

	static boolean headerLong(byte[] source) {
		return ((source[0] & 2) == 2);
	}

	static public long sizeDecompressed(byte[] source) {
		return headerLong(source) ?
			fast_read_long(source, 5, 4) : fast_read_long(source, 2, 1);
	}

	static public long sizeCompressed(byte[] source) {
		return fast_read_long(source, 1, (headerLong(source)) ? 4 : 1);
	}

	private static void write_header(byte[] dst, int level, boolean compressible, int size_compressed, int size_decompressed) {
		byte b = (byte) (2 | (compressible ? 1 : 0));
		b |= (byte) (level << 2);
		b |= (1 << 6);
		//b |= (0 << 4);
		dst[0] = b;
		fast_write(dst, 1, size_decompressed, 4);
		fast_write(dst, 5, size_compressed, 4);
	}

	public static byte[] compress(byte[] source) {
		return compress(source, 3);
	}

	/**
	 * level=(1|3)
	 */
	public static byte[] compress(byte[] source, int level) {
		byte[] destination = new byte[source.length + 400];

		int src = 0;
		int dst = DEFAULT_HEADERLEN + CWORD_LEN;
		long cword_val = 0x80000000L;
		int cword_ptr = DEFAULT_HEADERLEN;
		int[] cachetable = new int[HASH_VALUES];
		byte[] hash_counter = new byte[HASH_VALUES];
		int fetch = 0;
		int last_matchstart = (source.length - UNCONDITIONAL_MATCHLEN - UNCOMPRESSED_END - 1);
		int lits = 0;

		if (level != 1 && level != 3)
			throw new RuntimeException("Java version only supports level 1 and 3");

		int[][] hashtable = new int[HASH_VALUES][level == 1 ? QLZ_POINTERS_1 : QLZ_POINTERS_3];

		if (source.length == 0)
			return ArrayUtil.EMPTY_BYTE_ARRAY;

		if (src <= last_matchstart)
			fetch = fast_read_int(source, src, 3);

		while (src <= last_matchstart) {
			if ((cword_val & 1) == 1) {
				if (src > 3 * (source.length >> 2) && dst > src - (src >> 5)) {
					byte[] d2 = new byte[source.length + DEFAULT_HEADERLEN];
					write_header(d2, level, false, source.length, source.length + DEFAULT_HEADERLEN);
					System.arraycopy(source, 0, d2, DEFAULT_HEADERLEN, source.length);
					return d2;
				}

				fast_write(destination, cword_ptr, (cword_val >>> 1) | 0x80000000L, 4);
				cword_ptr = dst;
				dst += CWORD_LEN;
				cword_val = 0x80000000L;
			}

			if (level == 1) {
				int hash = ((fetch >>> 12) ^ fetch) & (HASH_VALUES - 1);
				int o = hashtable[hash][0];
				int cache = cachetable[hash] ^ fetch;

				cachetable[hash] = fetch;
				hashtable[hash][0] = src;

				if (cache == 0 && hash_counter[hash] != 0 && (src - o > MINOFFSET || (src == o + 1 && lits >= 3 && src > 3 && source[src] == source[src - 3] && source[src] == source[src - 2] && source[src] == source[src - 1] && source[src] == source[src + 1] && source[src] == source[src + 2]))) {
					cword_val = ((cword_val >>> 1) | 0x80000000L);
					if (source[o + 3] != source[src + 3]) {
						int f = 3 - 2 | (hash << 4);
						destination[dst + 0] = (byte) (f >>> 0 * 8);
						destination[dst + 1] = (byte) (f >>> 1 * 8);
						src += 3;
						dst += 2;
					} else {
						int old_src = src;
						int remaining = (Math.min((source.length - UNCOMPRESSED_END - src + 1 - 1), 255));

						src += 4;
						if (source[o + src - old_src] == source[src]) {
							src++;
							if (source[o + src - old_src] == source[src]) {
								src++;
								while (source[o + (src - old_src)] == source[src] && (src - old_src) < remaining)
									src++;
							}
						}

						int matchlen = src - old_src;

						hash <<= 4;
						if (matchlen < 18) {
							int f = hash | (matchlen - 2);

							destination[dst + 0] = (byte) (f >>> 0 * 8);
							destination[dst + 1] = (byte) (f >>> 1 * 8);
							dst += 2;
						} else {
                            fast_write(destination, dst, hash | (matchlen << 16), 3);
							dst += 3;
						}
					}
					lits = 0;
					fetch = fast_read_int(source, src, 3);
				} else {
					lits++;
					hash_counter[hash] = 1;
					destination[dst] = source[src];
					cword_val = (cword_val >>> 1);
					src++;
					dst++;
					fetch = ((fetch >>> 8) & 0xffff) | ((source[src + 2] & 0xff) << 16);
				}
			} else {
				fetch = fast_read_int(source, src, 3);

				int o, offset2;
				int matchlen, k, m/*, best_k = 0*/;
				byte c;
				int remaining = (Math.min((source.length - UNCOMPRESSED_END - src + 1 - 1), 255));
				int hash = ((fetch >>> 12) ^ fetch) & (HASH_VALUES - 1);

				c = hash_counter[hash];
				matchlen = 0;
				offset2 = 0;

                int[] hth = hashtable[hash];
				for (k = 0; k < QLZ_POINTERS_3 && (c > k || c < 0); k++) {
					o = hth[k];
					if ((byte) fetch == source[o] && (byte) (fetch >>> 8) == source[o + 1] && (byte) (fetch >>> 16) == source[o + 2] && o < src - MINOFFSET) {
						m = 3;
						while (source[o + m] == source[src + m] && m < remaining)
							m++;
						if ((m > matchlen) || (m == matchlen && o > offset2)) {
							offset2 = o;
							matchlen = m;
						}
					}
				}
				o = offset2;
				hth[c & (QLZ_POINTERS_3 - 1)] = src;
				c++;
				hash_counter[hash] = c;

				if (matchlen >= 3 && src - o < 131071) {
					int offset = src - o;
					for (int u = 1; u < matchlen; u++) {
						fetch = fast_read_int(source, src + u, 3);
						hash = ((fetch >>> 12) ^ fetch) & (HASH_VALUES - 1);
						c = hash_counter[hash]++;
						hashtable[hash][c & (QLZ_POINTERS_3 - 1)] = src + u;
					}

					src += matchlen;
					cword_val = ((cword_val >>> 1) | 0x80000000L);

					if (matchlen == 3 && offset <= 63) {
						fast_write(destination, dst, offset << 2, 1);
						dst++;
					} else if (matchlen == 3 && offset <= 16383) {
						fast_write(destination, dst, (offset << 2) | 1, 2);
						dst += 2;
					} else if (matchlen <= 18 && offset <= 1023) {
						fast_write(destination, dst, ((matchlen - 3) << 2) | (offset << 6) | 2, 2);
						dst += 2;
					} else if (matchlen <= 33) {
						fast_write(destination, dst, ((matchlen - 2) << 2) | (offset << 7) | 3, 3);
						dst += 3;
					} else {
						fast_write(destination, dst, ((matchlen - 3) << 7) | (offset << 15) | 3, 4);
						dst += 4;
					}
				} else {
					destination[dst] = source[src];
					cword_val = (cword_val >>> 1);
					src++;
					dst++;
				}
			}
		}

		while (src <= source.length - 1) {
			if ((cword_val & 1) == 1) {
				fast_write(destination, cword_ptr, (cword_val >>> 1) | 0x80000000L, 4);
				cword_ptr = dst;
				dst += CWORD_LEN;
				cword_val = 0x80000000L;
			}

			destination[dst] = source[src];
			src++;
			dst++;
			cword_val = (cword_val >>> 1);
		}
		while ((cword_val & 1) != 1) {
			cword_val = (cword_val >>> 1);
		}
		fast_write(destination, cword_ptr, (cword_val >>> 1) | 0x80000000L, CWORD_LEN);
		write_header(destination, level, true, source.length, dst);

		byte[] d2 = new byte[dst];
		System.arraycopy(destination, 0, d2, 0, dst);
		return d2;
	}

	static long fast_read_long(byte[] a, int i, int numbytes) {
		long l = 0;
		for (int j = 0; j < numbytes; j++)
			l |= ((a[i++] & 0xffL) << j * 8);
		return l;
	}
    static int fast_read_int(byte[] a, int i, int numbytes) {
        int l = 0;
        for (int j = 0; j < numbytes; j++)
            l |= ((a[i++] & 0xffL) << j * 8);
        return l;
    }

	static void fast_write(byte[] a, int i, long value, int numbytes) {
		for (int j = 0; j < numbytes; j++)
			a[i++] = (byte) (value >>> (j * 8));
	}

	static public byte[] decompress(byte[] source) {
		int size = (int) sizeDecompressed(source);
		int src = headerLen(source);
		int dst = 0;
		long cword_val = 1;
		byte[] destination = new byte[size];

        int[] hashtable =
            //new int[hashtable_size];
            null;

		//byte[] hash_counter = new byte[4096];
		int last_matchstart = size - UNCONDITIONAL_MATCHLEN - UNCOMPRESSED_END - 1;
		int last_hashed = -1;
		int fetch = 0;

		int level = (source[0] >>> 2) & 0x3;

		if (level != 1 && level != 3)
			throw new RuntimeException("Java version only supports level 1 and 3");

		if ((source[0] & 1) != 1) {
			byte[] d2 = new byte[size];
			System.arraycopy(source, headerLen(source), d2, 0, size);
			return d2;
		}

		for (; ; ) {
			if (cword_val == 1) {
				cword_val = fast_read_long(source, src, 4);
				src += 4;
				if (dst <= last_matchstart) {
					fetch = fast_read_int(source, src, level == 1 ? 3 : 4);
				}
			}

			if ((cword_val & 1) == 1) {
				int matchlen;
				int offset2;

				cword_val = cword_val >>> 1;

				if (level == 1) {
                    offset2 = hashtable[(fetch >>> 4) & 0xfff];

					if ((fetch & 0xf) != 0) {
						matchlen = (fetch & 0xf) + 2;
						src += 2;
					} else {
						matchlen = source[src + 2] & 0xff;
						src += 3;
					}
				} else {
					int offset;

					if ((fetch & 3) == 0) {
						offset = (fetch & 0xff) >>> 2;
						matchlen = 3;
						src++;
					} else if ((fetch & 2) == 0) {
						offset = (fetch & 0xffff) >>> 2;
						matchlen = 3;
						src += 2;
					} else if ((fetch & 1) == 0) {
						offset = (fetch & 0xffff) >>> 6;
						matchlen = ((fetch >>> 2) & 15) + 3;
						src += 2;
					} else if ((fetch & 127) != 3) {
						offset = (fetch >>> 7) & 0x1ffff;
						matchlen = ((fetch >>> 2) & 0x1f) + 2;
						src += 3;
					} else {
						offset = (fetch >>> 15);
						matchlen = ((fetch >>> 7) & 255) + 3;
						src += 4;
					}
					offset2 = dst - offset;
				}


				for (int i = 0; i < matchlen; i++)
					destination[dst + i] = destination[offset2 + i];

				dst += matchlen;

				int toFetch;
				if (level == 1) {
					fetch = fast_read_int(destination, last_hashed + 1, 3);
                    if (last_hashed < dst - matchlen) {
                        if (hashtable == null)
                            hashtable = new int[HASH_VALUES]; //lazy alloc
                        do {
                            last_hashed++;
                            hashtable[((fetch >>> 12) ^ fetch) & (HASH_VALUES - 1)] = last_hashed;
                            //hash_counter[hash] = 1;
                            fetch = fetch >>> 8 & 0xffff | (destination[last_hashed + 3] & 0xff) << 16;
                        } while (last_hashed < dst - matchlen);
                    }
					toFetch = 3;
				} else {
					toFetch = 4;
				}
                fetch = fast_read_int(source, src, toFetch);
				last_hashed = dst - 1;
			} else {
				if (dst <= last_matchstart) {
					destination[dst++] = source[src++];
					cword_val = cword_val >>> 1;

					if (level == 1 && (last_hashed < dst-3)) {
                        if (hashtable == null)
                            hashtable = new int[HASH_VALUES]; //lazy alloc

						do {
							int fetch2 = fast_read_int(destination, ++last_hashed, 3);
                            hashtable[((fetch2 >>> 12) ^ fetch2) & (HASH_VALUES - 1)] = last_hashed;
							//hash_counter[hash] = 1;
						} while (last_hashed < dst - 3);
					}

					fetch = fetch >> 8 & 0xffff | (source[src + 2] & 0xff) << 16;

					if (level!=1)
					    fetch = fetch | (source[src + 3] & 0xff) << 24;

				} else {
					while (dst <= size - 1) {
						if (cword_val == 1) {
							src += CWORD_LEN;
							cword_val = 0x80000000L;
						}

						destination[dst++] = source[src++];
						cword_val = cword_val >>> 1;
					}
					return destination;
				}
			}
		}
	}
}
