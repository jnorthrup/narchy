package mcaixictw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class McAIXIUtilTest {


	@Test
	public final void randRange() {

		for (int i = 0; i < 1000; i++) {
			int range = 1 + (int) Math.round(Math.random() * 7);
			int r = McAIXIUtil.randRange(range);
			assertTrue(r < range && r >= 0);
		}
	}

	@Test
	public final void testDecode() {
		BooleanArrayList list = new BooleanArrayList();
		list.add(true);
		list.add(true);
		list.add(false);
		list.add(true);
		assertTrue(McAIXIUtil.decode(list) == 13);
	}

	@Test
	public final void testEncode() {
		BooleanArrayList list = McAIXIUtil.encode(13, 4);
		assertTrue(list.get(0));
		assertTrue(list.get(1));
		assertTrue(!list.get(2));
		assertTrue(list.get(3));
	}

}
