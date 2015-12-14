package org.factcenter.qilin.protocols.concrete;

import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.LocalChannelFactory;
import org.factcenter.qilin.primitives.concrete.*;
import org.factcenter.qilin.primitives.generic.TrapdoorPedersenCommitment;
import org.factcenter.qilin.protocols.TrapdoorTwoPartyGroupElementFlip.TrapdoorFirst;
import org.factcenter.qilin.protocols.TrapdoorTwoPartyGroupElementFlipTest;
import org.factcenter.qilin.protocols.TwoPartyGroupElementFlip.First;
import org.factcenter.qilin.protocols.TwoPartyGroupElementFlip.Second;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Random;

@RunWith(Parameterized.class)
public class ECPedersenCoinflipTest extends TrapdoorTwoPartyGroupElementFlipTest<BigInteger> {
	ECGroup ecgrp;
	BigInteger sk;
	ECPedersenCoinflip flipper;
	ECTrapdoorPedersen trapdoorPedersen;
	ECPedersen standardPedersen;
	Channel firstChannel;
	Channel secondChannel;
	
	public ECPedersenCoinflipTest(Random rand, ECGroup ecgrp) {
		super(rand, new Zn(ecgrp.orderUpperBound())); 
		this.ecgrp = ecgrp;
		sk = TrapdoorPedersenCommitment.generateKey(ecgrp, rand);
		trapdoorPedersen = new ECTrapdoorPedersen(ecgrp, sk);
		standardPedersen = new ECPedersen(ecgrp, trapdoorPedersen.getH());
		
		LocalChannelFactory channelFactory = new LocalChannelFactory();
		Channel[] channels = channelFactory.getChannelPair();
		firstChannel = channels[0];
		secondChannel = channels[1];
		
		flipper = new ECPedersenCoinflip(ecgrp);
	}

	@Parameters
	public static Collection<Object[]> getTestParams() {
		return ECGroupTest.getTestParams();
	}
	
	@Override
	protected TrapdoorFirst<BigInteger> getTrapdoorFirst() {
		return flipper.newTrapdoorFirst(trapdoorPedersen);
	}

	@Override
	protected First<BigInteger> getFirst() {
		return flipper.newFirst(standardPedersen);
	}

	@Override
	protected Second<BigInteger> getSecond() {
		return flipper.newSecond(standardPedersen);
	}

	@Override
	protected Channel getFirsttoSecondChannel() {
		return firstChannel;
	}

	@Override
	protected Channel getSecondtoFirstChannel() {
		return secondChannel;
	}
}
