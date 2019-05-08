package savemgo.nomad.lobby;

import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.NomadLobby;
import savemgo.nomad.packet.Packet;

public class GateLobby extends NomadLobby {

	public GateLobby() {

	}

	@Override
	public boolean onPacket(ChannelHandlerContext ctx, Packet ins) {
		// TODO Auto-generated method stub
		return false;
	}

}
