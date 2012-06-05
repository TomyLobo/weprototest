package weprototest;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Test;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;

public class ProtocolTest {
	@Test
	public void testProtocol() throws Exception {
		SortedMap<BlockVector, BaseBlock> source = new TreeMap<BlockVector, BaseBlock>();

		for (int i = 0; i < 100*100*100*10; ++i) {
			//final BaseBlock block = new BaseBlock((int)(Math.random()*255), (int)(Math.random()*15));
			final BaseBlock block = new BaseBlock(232, 13);
			source.put(new Vector(Math.random()*100-50,Math.random()*100-50,Math.random()*100-50).toBlockPoint(), block);
		}
		//source.put(new BlockVector(1,0,0), new BaseBlock(232, 13));
		//source.put(new BlockVector(0,0,0), new BaseBlock(232, 13));

		byte[] encoded = Protocol.encode(source);
		System.out.println((double)encoded.length / source.size()+" bytes per block");
		Map<BlockVector, BaseBlock> decoded = Protocol.decode(encoded);

		assertEquals(source.size(), decoded.size());
		final TreeMap<BlockVector, BaseBlock> treeMapSource = new TreeMap<BlockVector, BaseBlock>(source);
		final TreeMap<BlockVector, BaseBlock> treeMapDecoded = new TreeMap<BlockVector, BaseBlock>(decoded);
		assertEquals(treeMapSource.keySet(), treeMapDecoded.keySet());
		assertEquals(treeMapSource, treeMapDecoded);
	}
}
