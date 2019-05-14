package savemgo.nomad.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.mapper.JoinRow;
import org.jdbi.v3.core.mapper.JoinRowMapper;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;

import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.crypto.ptsys.Ptsys;
import savemgo.nomad.database.DB;
import savemgo.nomad.database.record.Character;
import savemgo.nomad.database.record.User;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.packet.ResultError;
import savemgo.nomad.session.NomadUser;
import savemgo.nomad.util.Buffers;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Sessions;

public class Users {

	private static final Logger logger = LogManager.getLogger();

	private static final Packet SETSESSION_OK = new Packet(0x3004, 0);

	public static void setSession(ChannelHandlerContext ctx, Packet in, boolean isAccountLobby) {
		ResultError error = null;
		try (var handle = DB.open()) {
			var bi = in.getPayload();

			// Get id
			int id = bi.readInt();

			// Get session id
			// TODO: Use 16 char session ids in gidauth, and don't trim here
			Ptsys.encryptBlowfish(Packets.KEY_KIT, bi, 0x4, bi, 0x4, 0x10);
			var sessionId = Buffers.readString(bi, 0x10);
			sessionId = sessionId.substring(0, 8);
			logger.debug("Session id: {}", sessionId);

			// Check session id
			User user = null;
			Character chara = null;
			if (isAccountLobby) {
				user = handle.createQuery(
						"SELECT id, username, role, banned_until, is_cfw, slots FROM users WHERE id=:id AND session=:sessionId")
						.bind("id", id).bind("sessionId", sessionId).mapToBean(User.class).findOne().orElse(null);
			} else {
				handle.registerRowMapper(BeanMapper.factory(User.class, "u"));
				handle.registerRowMapper(BeanMapper.factory(Character.class, "c"));
				handle.registerRowMapper(JoinRowMapper.forTypes(User.class, Character.class));
				
				var row = handle.createQuery("SELECT u.id u_id, u.username u_username, u.role u_role, u.banned_until u_banned_until, "
						+ "u.is_cfw u_iscfw, u.slots u_slots, "
						+ "c.id c_id, c.user c_user, c.name c_name, c.old_name c_old_name, c.rank c_rank, c.comment c_comment, "
						+ "c.gameplay_options c_gameplay_options, c.active c_active, c.creation_time c_creation_time, c.lobby c_lobby "
						+ "FROM users u JOIN mgo2_characters c ON c.user=u.id WHERE c.id=:id AND u.session=:sessionId")
						.bind("id", id).bind("sessionId", sessionId).mapTo(JoinRow.class).findOne().orElse(null);
				if (row != null) {				
					user = row.get(User.class);
					chara = row.get(Character.class);
				}
			}

			if (user == null) {
				logger.error("Invalid session: {}", sessionId);
				error = ResultError.INVALID_SESSION;
				return;
			}

			// TODO: Check banned_until

			// TODO: Get user as well, and pass it to onLobbyJoin

			// Start session on server
			onLobbyJoin(ctx, user, chara);

			ctx.write(SETSESSION_OK);
		} catch (Exception e) {
			logger.error("setSession: Exception occurred.", e);
			error = ResultError.GENERAL;
		} finally {
			Packets.writeError(ctx, 0x3004, error);
		}
	}

	public static void onLobbyJoin(ChannelHandlerContext ctx, User user, Character chara) {
		var nUser = new NomadUser();
		nUser.setId(user.getId());
		nUser.setUsername(user.getUsername());
		nUser.setRole(user.getRole());
		nUser.setBannedUntil(user.getBannedUntil());
		nUser.setIsCfw(user.getIsCfw());
		if (chara != null) {
			nUser.setChara(chara.getId());
		} else {
			nUser.setChara(null);
		}

		Sessions.add(ctx.channel(), nUser);
	}

	public static void onLobbyLeave(ChannelHandlerContext ctx) {

	}

}
