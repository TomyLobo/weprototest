package weprototest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.SortedMap;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;

public class Encoder {
	private final SortedMap<BlockVector, BaseBlock> blocks;
	private final OutputStream os;

	private Vector min = new BlockVector(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
	private Vector max = new BlockVector(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE);

	private int nibble;
	private boolean nibbleStored = false;

	public Encoder(SortedMap<BlockVector, BaseBlock> blocks, OutputStream os) {
		this.blocks = blocks;
		for (BlockVector pos : blocks.keySet()) {
			min = Vector.getMinimum(min, pos);
			max = Vector.getMaximum(max, pos);
		}
		this.os = os;
	}

	private void putUnsigned(int width, int value) throws IOException {
		assert width % 4 == 0;

		for (int i = width; i > 0; i -= 4) {
			if (!nibbleStored && i >= 8) {
				i -= 4;
				os.write(value >> (width - 8) & 0xff);
				value <<= 8;
			}
			else {
				putNibble(value >> (width - 4) & 0xf);
				value <<= 4;
			}
		}
	}

	void putSigned(int width, int value) throws IOException {
		assert width % 4 == 0;

		for (int i = width; i > 0; i -= 4) {
			if (!nibbleStored && i >= 8) {
				i -= 4;
				os.write(value >> (width - 8) & 0xff);
				value <<= 8;
			}
			else {
				putNibble(value >> (width - 4) & 0xf);
				value <<= 4;
			}
		}
	}

	private void putNibble(int value) throws IOException {
		if (nibbleStored) {
			nibbleStored = false;

			os.write((nibble << 4) | value);
		}
		else {
			nibbleStored = true;
			nibble = value;
			return;
		}
	}

	public void encode() throws IOException {
		final int maxX = max.getBlockX()+1;
		//final int maxY = max.getBlockY()+1;
		final int maxZ = max.getBlockZ()+1;

		final int minX = min.getBlockX();
		final int minY = min.getBlockY();
		final int minZ = min.getBlockZ();

		final int xSize = maxX - minX;
		final int zSize = maxZ - minZ;

		putSigned(32, minX);
		putSigned(32, minY);
		putSigned(32, minZ);
		putUnsigned(4, 0);

		if (xSize >= 4096 || zSize >= 4096) {
			putUnsigned(4, Packets.SIZE_1M);
			putUnsigned(20, xSize);
			putUnsigned(20, zSize);
			putUnsigned(4, 0);
		}
		else {
			putUnsigned(4, Packets.SIZE_4096);
			putUnsigned(12, xSize);
			putUnsigned(12, zSize);
			putUnsigned(4, 0);
		}

		int skip = 0;
		BaseBlock repeatBlock = null;
		int repeatCount = 0;
		int lastX = minX-1, lastY = minY, lastZ = minZ;

		for (Entry<BlockVector, BaseBlock> entry : blocks.entrySet()) {
			final BlockVector pos = entry.getKey();
			int x = pos.getBlockX();
			int y = pos.getBlockY();
			int z = pos.getBlockZ();

			skip = x-1 - lastX + xSize * (z - lastZ + zSize * (y - lastY));

			lastX = x;
			lastY = y;
			lastZ = z;

			if (skip != 0) {
				commitRepeats(repeatCount);
				repeatCount = 0;
			}

			BaseBlock block = entry.getValue();
			assert block != null;

			commitSkips(skip);
			skip = 0;

			if (block.equals(repeatBlock)) {
				++repeatCount;
				continue;
			}

			commitRepeats(repeatCount);
			repeatCount = 0;

			putUnsigned(4, Packets.BLOCK_256);
			putUnsigned(8, block.getType());
			putUnsigned(4, block.getData());
			repeatBlock = block;
		}
		assert skip == 0 || repeatCount == 0;
		commitRepeats(repeatCount);
		putUnsigned(4, Packets.END);
	}

	private void commitSkips(int skip) throws IOException {
		commit(skip, Packets.SKIP_16, Packets.SKIP_4096, Packets.SKIP_1M);
	}

	private void commitRepeats(int repeatCount) throws IOException {
		commit(repeatCount, Packets.REPEAT_16, Packets.REPEAT_4096, Packets.REPEAT_1M);
	}

	private void commit(int amount, final int packet16, final int packet4096, final int packet1m) throws IOException {
		while (amount != 0) {
			if (amount <= 16) {
				putUnsigned(4, packet16);
				putUnsigned(4, amount - 1);
				amount = 0;
			}
			else if (amount <= 4096) {
				putUnsigned(4, packet4096);
				putUnsigned(12, amount - 1);
				amount = 0;
			}
			else {
				putUnsigned(4, packet1m);
				if (amount <= 1048576) {
					putUnsigned(20, amount - 1);
					amount = 0;
				}
				else {
					putUnsigned(20, 1048576 - 1);
					amount -= 1048576;
				}
			}
		}
	}
}
