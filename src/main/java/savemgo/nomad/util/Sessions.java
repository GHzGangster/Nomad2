package savemgo.nomad.util;

import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import savemgo.nomad.session.NomadUser;

public class Sessions {

	private static final AttributeKey<NomadUser> USER = AttributeKey.valueOf("user");

	private static final ConcurrentHashMap<Channel, NomadUser> users = new ConcurrentHashMap<>();

	public static void add(Channel channel, NomadUser user) {
		users.put(channel, user);
	}

	public static void remove(Channel channel) {
		users.remove(channel);
	}

	public static NomadUser getUser(Channel channel) {
		return users.get(channel);
	}

	public static NomadUser getUser(ChannelHandlerContext ctx) {
		return ctx.channel().attr(USER).get();
	}

	public static void setUser(ChannelHandlerContext ctx, NomadUser user) {
		var attr = ctx.channel().attr(USER);
		attr.set(user);
	}

}
