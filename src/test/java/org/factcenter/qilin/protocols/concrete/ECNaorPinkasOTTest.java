package org.factcenter.qilin.protocols.concrete;


import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.LocalChannelFactory;
import org.factcenter.qilin.primitives.RandomOracle;
import org.factcenter.qilin.primitives.concrete.DigestOracle;
import org.factcenter.qilin.primitives.concrete.ECGroup;
import org.factcenter.qilin.primitives.concrete.ECGroupTest;
import org.factcenter.qilin.protocols.OT1of2.Chooser;
import org.factcenter.qilin.protocols.OT1of2.Sender;
import org.factcenter.qilin.protocols.OT1of2Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;


@RunWith(Parameterized.class)
public class ECNaorPinkasOTTest extends OT1of2Test {
	Random rand;
	RandomOracle H;
	ECGroup grp;

	ECNaorPinkasOT ot;
	ECNaorPinkasOT.Chooser chooser;
	ECNaorPinkasOT.Sender  sender;
	Channel[] channels;

	public ECNaorPinkasOTTest(ECGroup ecgrp, RandomOracle H) throws IOException {
		this.grp = ecgrp;
		this.H = H;

		LocalChannelFactory channelFactory = new LocalChannelFactory();
		channels = channelFactory.getChannelPair();
		ot = new ECNaorPinkasOT(H, grp);
		chooser = ot.newChooser();
		sender = ot.newSender();
	}

	@Parameters
	public static Collection<Object[]> getTestParams() {
		RandomOracle H = new DigestOracle();
		List<ECGroup> groups = ECGroupTest.getTestGroups();
		List<Object[]> params = new ArrayList<Object[]>(groups.size());
		for (ECGroup grp : groups) {
			Object[] param = {grp, H};
			params.add(param);
		}
		return params;
	}
	
	@Override
	protected Chooser getOTChooser() {
		assert chooser != null;
		return chooser;
	}

	@Override
	protected Sender getOTSender() {
		assert sender != null;
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
