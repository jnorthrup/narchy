package nars.ca;




public class CACell {
	final int x;
	final int y;
	final short state;

	public CACell() {
		x = 0;
		y = 0;
		state = (short) 0;
	}

	public CACell(int ix, int iy, short stt) {
		x = ix;
		y = iy;
		state = stt;
	}
}