package weprototest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.SortedMap;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BaseBlock;

public class Protocol {
	public static Map<BlockVector, BaseBlock> decode(byte[] input) {
		ByteArrayInputStream is = new ByteArrayInputStream(input);
		try {
			return decode(is);
		} catch (IOException e) {
			// ByteArrayInputStream doesn't throw any IOExceptions, so this should never be triggered.
			e.printStackTrace();
			assert false;
			throw new RuntimeException("Apparently, we got an IOException from a ByteArrayInputStream...");
		}
	}

	public static Map<BlockVector, BaseBlock> decode(InputStream is) throws IOException {
		return new Decoder(is).decode();
	}

	public static byte[] encode(SortedMap<BlockVector, BaseBlock> blocks) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		try {
			encode(blocks, os);
		} catch (IOException e) {
			// ByteArrayOutputStream doesn't throw any IOExceptions, so this should never be triggered.
			e.printStackTrace();
			assert false;
			throw new RuntimeException("Apparently, we got an IOException from a ByteArrayOutputStream...");
		}

		return os.toByteArray();
	}

	public static void encode(SortedMap<BlockVector, BaseBlock> blocks, OutputStream os) throws IOException {
		new Encoder(blocks, os).encode();
	}
}
