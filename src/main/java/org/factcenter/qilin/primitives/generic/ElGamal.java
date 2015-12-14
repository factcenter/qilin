package org.factcenter.qilin.primitives.generic;

import org.factcenter.qilin.primitives.CyclicGroup;
import org.factcenter.qilin.util.Pair;
import org.factcenter.qilin.primitives.Cipher;
import org.factcenter.qilin.primitives.Homomorphic;
import org.factcenter.qilin.util.IntegerUtils;

import java.math.BigInteger;
import java.util.Random;


/**
 * An implementation of the ElGamal cryptosystem over a generic
 * group (in which discrete log is hard).
 * @author talm
 *
 */
public class ElGamal {
	/**
	 * Represents the public key in the ElGamal cryptosystem. 
	 * Given a public-key object, it is possible to encrypt messages
	 * and perform homomorphic operations. Decryption requires
	 * the secret key.
	 * @author talm
	 *
	 * @param <G> The underlying group element type. This is used as both the plaintext
	 * and the ciphertext type. 
	 * @see SK
	 */
	static public class PK<G> implements Cipher.PK<Pair<G,G>,G,BigInteger>, Homomorphic<Pair<G,G>,G,BigInteger> {
		/**
		 * The underlying group over which operations are performed.
		 * The security of the scheme depends on the hardness of the discrete log
		 * problem in this group.
		 */
		CyclicGroup<G> grp;
		
		/**
		 * Public key.
		 */
		
		G pk;
		
		/**
		 * A generator of the group.
		 */
		transient G g;

		public PK(CyclicGroup<G> grp, G pk) {
			this.grp = grp;
			this.g = grp.getGenerator();
			this.pk = pk;
		}

		@Override
		public Pair<G, G> encrypt(G plaintext, BigInteger randomness) {
			G gr = grp.multiply(g, randomness);
			return new Pair<G,G>(gr, grp.add(grp.multiply(pk, randomness), plaintext));
		}

		@Override
		public Pair<G, G> multiply(Pair<G, G> cipher, BigInteger scalar) {
			return new Pair<G,G>(grp.multiply(cipher.a, scalar), grp.multiply(cipher.b, scalar));
		}

		@Override
		public Pair<G, G> add(Pair<G, G> cipher1, Pair<G, G> cipher2) {
			return new Pair<G,G>(grp.add(cipher1.a, cipher2.a),grp.add(cipher1.b, cipher2.b));
		}

		@Override
		public BigInteger rndadd(G msg1, BigInteger rnd1, G msg2, BigInteger rnd2) {
			return rnd1.add(rnd2);
		}

		@Override
		public BigInteger getRandom(Random rand) {
			return IntegerUtils.getRandomInteger(grp.orderUpperBound(), rand);
		}
		
		public G getPK() {
			return pk;
		}

		@Override
		public BigInteger rndmul(G msg, BigInteger rnd, BigInteger scalar) {
			return rnd.multiply(scalar).mod(grp.orderUpperBound());
		}

		@Override
		public Pair<G, G> negate(Pair<G, G> cipher) {
			return new Pair<G,G>(grp.negate(cipher.a), grp.negate(cipher.b));
		}

		@Override
		public BigInteger rndnegate(G msg, BigInteger rnd) {
			return grp.orderUpperBound().subtract(rnd);
		}

	}
	
	/**
	 * Represents the secret key in the ElGamal cryptosystem. 
	 * Given a secret-key object, it is decrypt messages (in addition
	 * to everything that can by done with the public-key object).
	 * 
	 * @author talm
	 *
	 * @param <G> The underlying group element type. This is used as both the plaintext
	 * and the ciphertext type. 
	 */
	static public class SK<G> extends PK<G> implements Cipher.SK<Pair<G,G>,G,BigInteger> {
		BigInteger sk;
		
		public SK(CyclicGroup<G> grp, BigInteger sk) {
			super(grp, grp.multiply(grp.getGenerator(), sk));
			this.sk = sk;
		}
		
		public BigInteger getSK() {
			return sk;
		}

		@Override
		public G decrypt(Pair<G, G> c) {
			if (sk == null)
				return null;
			
			G grs = grp.multiply(c.a, sk);
			return grp.add(c.b, grp.negate(grs));
		}
	}
}
