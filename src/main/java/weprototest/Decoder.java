package weprototest;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BaseBlock;

public class Decoder {
	private InputStream is;

	int nibble;
	boolean nibbleStored = false;

	public Decoder(InputStream is) {
		this.is = is;
	}

	int getNibble() throws IOException {
		if (nibbleStored) {
			nibbleStored = false;
			return nibble;
		}
		else {
			nibbleStored = true;
			int read = is.read();
			nibble = read & 0xf;
			return read >> 4 & 0xf;
		}
	}

	int getUnsigned(int width) throws IOException {
		assert width % 4 == 0;

		int value = 0;
		for (; width > 0; width -= 4) {
			if (!nibbleStored && width >= 8) {
				width -= 4;
				value = value << 8 | is.read();
			}
			else {
				value = value << 4 | getNibble();
			}
		}
		return value;
	}
	int getSigned(int width) throws IOException {
		assert width % 4 == 0;

		int value = 0;
		for (; width > 0; width -= 4) {
			value = value << 4 | getNibble();
		}
		return signExtend(width, value);
	}

	private int signExtend(int width, int value) {
		return (value << (32-width)) >> (32-width);
	}

	int x = 0, y = 0, z = 0;
	int wrapX = Integer.MAX_VALUE;
	int wrapZ = Integer.MAX_VALUE;
	int xSize = 0;
	int zSize = 0;

	private void advance(int i) {
		x += i;
		while (x >= wrapX) {
			x -= xSize;
			++z;
		}
		while (z >= wrapZ) {
			z -= zSize;
			++y;
		}
	}

	public Map<BlockVector, BaseBlock> decode() throws IOException {
		HashMap<BlockVector, BaseBlock> blocks = new HashMap<BlockVector, BaseBlock>();

		int packetId = Packets.POSITION;

		int type = -1, data = -1;
		while (true) {
			switch (packetId) {
			case Packets.END:
				return blocks;

			case Packets.POSITION:
				x = getSigned(32);
				y = getSigned(32);
				z = getSigned(32);
				getUnsigned(4);
				break;

			case Packets.RELATIVE_POSITION:
				x += getSigned(12);
				y += getSigned(12);
				z += getSigned(12);
				break;

			case Packets.SIZE_4096:
				xSize = getUnsigned(12);
				zSize = getUnsigned(12);
				wrapX = x+xSize;
				wrapZ = z+zSize;
				advance(getUnsigned(4));
				break;

			case Packets.SIZE_1M:
				xSize = getUnsigned(20);
				zSize = getUnsigned(20);
				wrapX = x+xSize;
				wrapZ = z+zSize;
				advance(getUnsigned(4));
				break;

			case Packets.BLOCK_256:
				type = getUnsigned(8);
				data = getUnsigned(4);
				blocks.put(new BlockVector(x, y, z), new BaseBlock(type, data));
				advance(1);
				break;

			case Packets.BLOCK_4096:
				type = getUnsigned(12);
				data = getUnsigned(8);
				blocks.put(new BlockVector(x, y, z), new BaseBlock(type, data));
				advance(1);
				break;

			case Packets.REPEAT_16:
				for (int i = 0, amount = getUnsigned(4) + 1; i < amount; ++i) {
					blocks.put(new BlockVector(x, y, z), new BaseBlock(type, data));
					advance(1);
				}
				break;

			case Packets.REPEAT_4096:
				for (int i = 0, amount = getUnsigned(12) + 1; i < amount; ++i) {
					blocks.put(new BlockVector(x, y, z), new BaseBlock(type, data));
					advance(1);
				}
				break;

			case Packets.REPEAT_1M:
				for (int i = 0, amount = getUnsigned(20) + 1; i < amount; ++i) {
					blocks.put(new BlockVector(x, y, z), new BaseBlock(type, data));
					advance(1);
				}
				break;

			case Packets.SKIP_16:
				advance(getUnsigned(4) + 1);
				break;

			case Packets.SKIP_4096:
				advance(getUnsigned(12) + 1);
				break;

			case Packets.SKIP_1M:
				advance(getUnsigned(20) + 1);
				break;

			default:
				throw new IllegalArgumentException("WorldEdit: Invalid block data packet id "+packetId);
			}

			assert nibbleStored;
			packetId = getUnsigned(4);
		}
	}
}
