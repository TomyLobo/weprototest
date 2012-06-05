package weprototest;

public final class Packets {
	public static final int END = 0x0;
	public static final int POSITION = 0x1;
	public static final int RELATIVE_POSITION = 0x2;
	public static final int SIZE_4096 = 0x3;
	public static final int SIZE_1M = 0x4;
	public static final int BLOCK_256 = 0x5;
	public static final int BLOCK_4096 = 0x6;
	public static final int REPEAT_16 = 0x7;
	public static final int REPEAT_4096 = 0x8;
	public static final int REPEAT_1M = 0x9;
	public static final int SKIP_16 = 0xA;
	public static final int SKIP_4096 = 0xB;
	public static final int SKIP_1M = 0xC;

	private Packets() { }
}
