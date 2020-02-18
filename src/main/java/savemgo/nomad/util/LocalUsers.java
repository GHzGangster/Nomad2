package savemgo.nomad.util;

import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import savemgo.nomad.local.LocalUser;

public class LocalUsers {

	private static final AttributeKey<LocalUser> USER = AttributeKey.valueOf("user");

	private static final ConcurrentHashMap<Channel, LocalUser> USERS = new ConcurrentHashMap<>();

	public static void add(Channel channel, LocalUser user) {
		USERS.put(channel, user);
	}

	public static void remove(Channel channel) {
		USERS.remove(channel);
	}

	public static LocalUser getUser(Channel channel) {
		return USERS.get(channel);
	}

	public static LocalUser get(ChannelHandlerContext ctx) {
		return ctx.channel().attr(USER).get();
	}

	public static void set(ChannelHandlerContext ctx, LocalUser user) {
		var attr = ctx.channel().attr(USER);
		attr.set(user);
	}

}
