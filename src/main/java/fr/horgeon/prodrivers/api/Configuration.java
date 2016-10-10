package fr.horgeon.prodrivers.api;

import com.amazon.webservices.common.HMACEncoding;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystemException;
import java.security.SecureRandom;
import java.util.*;

public class Configuration {
	private JavaPlugin plugin;

	private FileConfiguration config;
	private FileConfiguration messages;

	private Map<String, String> keys;

	public Configuration( JavaPlugin plugin ) {
		this.plugin = plugin;
		this.config = this.plugin.getConfig();

		this.config.options().copyDefaults( true );

		this.plugin.saveConfig();

		loadMessages();
		loadKeys();
	}

	private void copy( InputStream in, File file ) {
		try {
			OutputStream out = new FileOutputStream( file );
			byte[] buf = new byte[ 1024 ];
			int len;
			while( ( len = in.read( buf ) ) > 0 ) {
				out.write( buf, 0, len );
			}
			out.close();
			in.close();
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}

	public void loadMessages() {
		try {
			File pluginmessagesfile = new File( this.plugin.getDataFolder(), "messages.yml" );

			if( !pluginmessagesfile.exists() ) {
				if( pluginmessagesfile.getParentFile().mkdirs() ) {
					copy( this.plugin.getResource( "messages.yml" ), pluginmessagesfile );
				} else {
					throw new FileSystemException( "Unable to create plugin's configuration directory." );
				}
			}

			this.messages = YamlConfiguration.loadConfiguration( pluginmessagesfile );
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}

	public void loadKeys() {
		if( this.keys == null ) {
			this.keys = new HashMap<>();
		} else {
			this.keys.clear();
		}

		unserializeKeys( this.config.getStringList( "keys" ) );
	}

	public void unserializeKeys( List<String> serializedKeys ) {
		if( serializedKeys == null )
			return;

		for( String key : serializedKeys ) {
			String pair[] = key.split( ":" );

			if( pair.length > 1 ) {
				this.keys.put( pair[ 0 ], pair[ 1 ] );
			}
		}
	}

	public String generateKey() throws Exception {
		SecureRandom rand = new SecureRandom();
		KeyGenerator keyGen = KeyGenerator.getInstance( "HmacSHA256" );
		keyGen.init( rand );
		keyGen.init( 512 );
		SecretKey key = keyGen.generateKey();
		String keystr = HMACEncoding.EncodeBase64( key.getEncoded() );
		keystr = keystr.replaceAll( ":", "=" );
		return keystr;
	}

	public String addKey() {
		try {
			String publicKey = generateKey();
			this.keys.put( publicKey, generateKey() );
			this.config.set( "keys", serializeKeys() );
			this.plugin.saveConfig();

			return publicKey;
		} catch( Exception e ) {
			System.err.println( "Could not generate key!" );
			e.printStackTrace();
		}

		return "";
	}

	public List<String> serializeKeys() {
		List<String> serializedKeys = new ArrayList<>();

		for( Map.Entry<String, String> entry : this.keys.entrySet() ) {
			serializedKeys.add( String.format( "%s:%s", entry.getKey(), entry.getValue() ) );
		}

		return serializedKeys;
	}

	public void reload() {
		this.plugin.reloadConfig();

		File pluginmessagesfile = new File( this.plugin.getDataFolder(), "messages.yml" );
		this.messages = YamlConfiguration.loadConfiguration( pluginmessagesfile );

		loadKeys();
	}

	public String getString( String key ) {
		return this.config.getString( key );
	}

	public Integer getInt( String key ) {
		return this.config.getInt( key );
	}

	public Map<String, String> getKeys() {
		return this.keys;
	}

	public String getMessage( String key ) {
		return this.messages.getString( key );
	}
}
