package savemgo.nomad.local.util;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import savemgo.nomad.local.LocalUser;

public class LocalUsers {

	private static final AttributeKey<LocalUser> USER = AttributeKey.valueOf("user");

	public static void add(Channel channel, LocalUser user) {
		channel.attr(USER).set(user);
	}

	public static void remove(Channel channel) {
		channel.attr(USER).set(null);
	}

	public static LocalUser get(Channel channel) {
		return channel.attr(USER).get();
	}

}
