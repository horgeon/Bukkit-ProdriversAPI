package fr.horgeon.prodrivers.api;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Chat {
	private String prefix;

	public Chat( String name ) {
		this.prefix = ChatColor.BOLD + "[" + ChatColor.GOLD + name + ChatColor.WHITE + "] " + ChatColor.RESET;
	}

	public void send( CommandSender sender, String message, ChatColor color ) {
		sender.sendMessage( this.prefix + color + message );
	}

	public void send( CommandSender sender, String message ) {
		send( sender, message, ChatColor.WHITE );
	}

	public void success( CommandSender sender, String message ) {
		send( sender, message, ChatColor.GREEN );
	}

	public void error( CommandSender sender, String message ) {
		send( sender, message, ChatColor.RED );
	}
}
