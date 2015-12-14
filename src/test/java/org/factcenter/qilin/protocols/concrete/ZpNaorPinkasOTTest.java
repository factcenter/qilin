package org.factcenter.qilin.protocols.concrete;

import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.LocalChannelFactory;
import org.factcenter.qilin.primitives.RandomOracle;
import org.factcenter.qilin.primitives.concrete.DigestOracle;
import org.factcenter.qilin.primitives.concrete.Zpsafe;
import org.factcenter.qilin.protocols.OT1of2.Chooser;
import org.factcenter.qilin.protocols.OT1of2.Sender;
import org.factcenter.qilin.protocols.OT1of2Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;


@RunWith(Parameterized.class)
public class ZpNaorPinkasOTTest extends OT1of2Test {
	public final static int[] BITS = {8, 64, 256}; 

	Random rand;
	RandomOracle H;
	LocalChannelFactory channelFactory;
	Zpsafe grp;

	ZpNaorPinkasOT.Chooser chooser;
	ZpNaorPinkasOT.Sender  sender;
	Channel[] channels;
	
	
	public ZpNaorPinkasOTTest(Random rand, Zpsafe grp, RandomOracle H) {
		this.rand = rand;
		this.grp = grp;
		this.H = H;
		channelFactory = new LocalChannelFactory();
		channels = channelFactory.getChannelPair();
		ZpNaorPinkasOT ot = new ZpNaorPinkasOT(H, grp);
		chooser = ot.newChooser();
		sender = ot.newSender();
	}

	@Parameters
	public static Collection<Object[]>  getTestParameters() {
		Random rand = new Random(1);
		RandomOracle H = new DigestOracle();
		List<Object[]> params = new ArrayList<Object[]>(BITS.length);
		for (int bits : BITS) {
			Zpsafe grp = new Zpsafe(Zpsafe.randomSafePrime(bits, 50, rand));
			Object[] param = {rand, grp, H};
			params.add(param);
		}
		return params;
	}

	@Override
	protected Chooser getOTChooser() {
		return chooser;
	}

	@Override
	protected Sender getOTSender() {
		return sender;
	}

	@Override
	protected Channel getChoosertoSenderChannel() {
		return channels[0];
	}

	@Override
	protected Channel getSendertoChooserChannel() {
		return channels[1];
	}

}
