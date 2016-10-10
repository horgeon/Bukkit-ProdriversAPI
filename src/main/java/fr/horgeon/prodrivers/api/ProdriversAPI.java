package fr.horgeon.prodrivers.api;

import fr.horgeon.apiserver.HTTPHandler;
import fr.horgeon.apiserver.HTTPServer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.logging.Logger;

public class ProdriversAPI extends JavaPlugin implements Listener {
	private Chat chat;
	private Configuration config;
	private HTTPServer server;

	private final Logger logger = Logger.getLogger( "Minecraft" );

	@Override
	public void onDisable() {
		stopServer();

		PluginDescriptionFile plugindescription = this.getDescription();
		this.logger.info( plugindescription.getName() + " has been disabled!" );
	}

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents( this, this );

		PluginDescriptionFile plugindescription = this.getDescription();
		this.chat = new Chat( plugindescription.getName() );
		this.config = new Configuration( this );

		getCommand( "prodriversapi" ).setExecutor( this );

		this.logger.info( plugindescription.getName() + " has been enabled!" );

		initServer();
	}

	public void registerHandler( String endpoint, HTTPHandler handler ) {
		if( this.server != null )
			this.server.registerHandler( endpoint, handler );
	}

	public void unregisterHandler( String endpoint ) {
		if( this.server != null )
			this.server.unregisterHandler( endpoint );
	}

	private void initServer() {
		if( this.server != null )
			return;

		if( !createServer() )
			return;

		try {
			loadKeys();

		} catch( Exception e ) {
			this.logger.severe( "Couldn't load keys!" );
			e.printStackTrace();
			return;
		}

		BukkitScheduler scheduler = getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask( this, new Runnable() {
			@Override
			public void run() {
				startServer();
			}
		}, 1L );
	}

	private boolean createServer() {
		try {
			if( this.server == null ) {
				this.server = new HTTPServer( this.config.getInt( "port" ) );
			}
		} catch( Exception e ) {
			this.logger.severe( "The HTTP server couldn't be created!" );
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private void startServer() {
		try {
			if( this.server != null ) {
				this.server.start();
				this.logger.info( String.format( "HTTP Server now listening on port %d.", this.server.port ) );
			}
		} catch( Exception e ) {
			this.logger.severe( "The HTTP server couldn't be started!" );
			e.printStackTrace();
		}
	}

	private void stopServer() {
		try {
			if( this.server != null ) {
				this.server.stop();

				this.logger.info( "HTTP Server stopped." );
			}
		} catch( Exception e ) {
			this.logger.severe( "The HTTP server couldn't be stopped! It is highly recommended to restart the server." );
			e.printStackTrace();
		}
	}

	private void restartServer() {
		try {
			if( this.server != null ) {
				if( this.server.isStarted() )
					this.server.restart();
				else
					this.server.start();
				this.logger.info( String.format( "HTTP Server restarted, now listening on port %d.", this.server.port ) );
			}
		} catch( Exception e ) {
			this.logger.severe( "The HTTP server couldn't be restarted! It is highly recommended to restart the server." );
			e.printStackTrace();
		}
	}

	private void loadKeys() {
		this.server.setKeys( this.config.getKeys() );
	}

	private void startCommand( CommandSender sender ) {
		if( sender.hasPermission( "prodriversapi.start" ) ) {
			if( this.server != null ) {
				if( this.server.isStarted() ) {
					chat.error( sender, this.config.getMessage( "serveralreadystarted" ) );
				} else {
					try {
						startServer();

						chat.success( sender, this.config.getMessage( "serverstarted" ) );
					} catch( Exception e ) {
						chat.error( sender, this.config.getMessage( "errorocurred" ) );
						chat.error( sender, e.getLocalizedMessage() );
					}
				}
			} else {
				try {
					initServer();

					chat.success( sender, this.config.getMessage( "serverstarted" ) );
				} catch( Exception e ) {
					chat.error( sender, this.config.getMessage( "errorocurred" ) );
					chat.error( sender, e.getLocalizedMessage() );
				}
			}
		} else {
			chat.error( sender, this.config.getMessage( "nopermission" ) );
		}
	}

	private void stopCommand( CommandSender sender ) {
		if( sender.hasPermission( "prodriversapi.stop" ) ) {
			if( ( this.server == null ) || ( !this.server.isStarted() ) ) {
				chat.error( sender, this.config.getMessage( "serveralreadystopped" ) );
			} else {
				try {
					stopServer();

					chat.success( sender, this.config.getMessage( "serverstopped" ) );
				} catch( Exception e ) {
					chat.error( sender, this.config.getMessage( "errorocurred" ) );
					chat.error( sender, e.getLocalizedMessage() );
				}

				chat.success( sender, this.config.getMessage( "serverstopped" ) );
			}
		} else {
			chat.error( sender, this.config.getMessage( "nopermission" ) );
		}
	}

	private void restartCommand( CommandSender sender ) {
		if( sender.hasPermission( "prodriversapi.restart" ) ) {
			if( this.server != null ) {
				try {
					restartServer();

					chat.success( sender, this.config.getMessage( "serverrestarted" ) );
				} catch( Exception e ) {
					chat.error( sender, this.config.getMessage( "errorocurred" ) );
					chat.error( sender, e.getLocalizedMessage() );
				}
			}
		} else {
			chat.error( sender, this.config.getMessage( "nopermission" ) );
		}
	}

	private void reloadCommand( CommandSender sender ) {
		if( sender.hasPermission( "prodriversapi.reload" ) ) {
			this.config.reload();

			chat.success( sender, this.config.getMessage( "configurationreloaded" ) );
		} else {
			chat.error( sender, this.config.getMessage( "nopermission" ) );
		}
	}

	private void addKeyCommand( CommandSender sender ) {
		if( sender.hasPermission( "prodriversapi.addkey" ) ) {
			String publicKey = this.config.addKey();

			if( !publicKey.equalsIgnoreCase( "" ) )
				chat.success( sender, String.format( this.config.getMessage( "keyadded" ), publicKey ) );
			else
				chat.error( sender, this.config.getMessage( "keynotadded" ) );
		} else {
			chat.error( sender, this.config.getMessage( "nopermission" ) );
		}
	}

	public boolean onCommand( CommandSender sender, Command command, String label, String[] args ) {
		if( label.equalsIgnoreCase( "prodriversapi" ) ) {
			if( args.length > 0 ) {
				switch( args[ 0 ] ) {
					case "start":
						startCommand( sender );
						break;

					case "stop":
						stopCommand( sender );
						break;

					case "restart":
						restartCommand( sender );
						break;

					case "reload":
						reloadCommand( sender );
						break;

					case "addkey":
						addKeyCommand( sender );
						break;
				}
			} else {
				chat.send( sender, "Usage:" );
				chat.send( sender, "/prodriversapi <start/stop/restart/reload/addkey>" );
			}

			return true;
		}

		return false;
	}

}
