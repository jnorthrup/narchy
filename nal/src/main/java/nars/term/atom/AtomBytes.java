package nars.term.atom;

import nars.Op;
import nars.Idempotent;
import nars.io.IO;
import nars.term.Compound;

import java.io.DataInput;
import java.io.IOException;

import static nars.Op.ATOM;

public class AtomBytes extends AbstractAtomic implements Idempotent {

	public static AtomBytes atomBytes(String raw) {
		return atomBytes(raw.getBytes());
	}

	public static AtomBytes atomBytes(Compound c) {
		return atomBytes(IO.termToBytes(c));
	}

	public static AtomBytes atomBytes(DataInput i) throws IOException {
		var bb = new byte[i.readUnsignedShort()];
		i.readFully(bb);
		return AtomBytes.atomBytes(bb);
	}

	public static AtomBytes atomBytes(byte[] raw) {
		return new AtomBytes(raw);
	}

//	public static AtomCompressed compressed(byte[] raw) {
//		byte[] compressed = QuickLZ.compress(raw, 1);
//
//		{
//			byte[] oo = new byte[raw.length * 2];
//			ByteArrayDataOutput o = new ByteArrayDataOutput(oo);
//			LZ4.compress(raw, 0, raw.length, o, new LZ4.LZ4Table());
//			System.out.println(o.getPosition() + " " + Arrays.toString(oo));
//		}
//		{
//			byte[] oo = new byte[raw.length * 2];
//			ByteArrayDataOutput o = new ByteArrayDataOutput(oo);
//			LZ4.compressHC(raw, 0, raw.length, o, new LZ4.LZ4HCTable());
//			System.out.println(o.getPosition() + " " + Arrays.toString(oo));
//		}
//
//
//		//TODO use prefixReserve to use only one array
//		return new AtomCompressed(compressed);
//	}

	private AtomBytes(byte[] compressed) {
		super(IO.opAndEncoding((byte)0 /* ATOM */, (byte) 2), compressed);
	}

	@Override
	public String toString() {
		var b = bytes;
		//byte[] b = QuickLZ.decompress(bytes, 3)
		return "\"" + new String(b, 3, b.length-3) + "\"";
	}

	@Override
	public Op op() {
		return ATOM;
	}
}
