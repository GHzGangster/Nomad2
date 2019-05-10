package savemgo.nomad.crypto.ptsys;

import net.sourceforge.blowfishj.crypt.BlowfishECB;

public class PtsysState {

	public boolean ready;
	
	public BlowfishECB blowfish;

	public byte[] last = new byte[8];

}
