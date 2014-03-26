package BP.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import BP.users.GameUser;
import BP.users.GameUserImage;
import BP.domain.GameData;
import BP.events.objects.GameCreated;
import BP.events.objects.GameStarted;
import BP.events.objects.GameEnded;
import BP.events.objects.UserKilled;
import BP.game.Game;


public class GameManager implements GameManagerInterface {
	
	/**
	 * Constructor 
	 */
	public GameManager() {
	}

	//User Management
	public String RegisterUser(String code_name, GameUserImage thumbnail, 
			ArrayList<GameUserImage> faceImages, String apn, String platformID) {
		ArrayList<String> usrImageUUIDs = new ArrayList<String>();
		for (GameUserImage a: faceImages) {
			if (a.getUUID() == null)
				throw new IllegalArgumentException();
			usrImageUUIDs.add(a.getUUID());
		}
		GameUser g = new GameUser(code_name, thumbnail.getUUID(), usrImageUUIDs);
		g.setAPN(apn);
		g.setPlatformID(platformID);
		
		PersistenceManager pm = getPersistenceManager();
		try {
			pm.makePersistent(thumbnail);
			pm.makePersistentAll(faceImages);
			pm.makePersistent(g);
		} finally {
			pm.close();
		}
		return g.getUUID();
	}
	
	public void deleteUser(String uuid) {
		PersistenceManager pm = getPersistenceManager();
		try {
			GameUser GameUserToDelete = pm.getObjectById(GameUser.class, uuid);
			pm.deletePersistent(GameUserToDelete);
		} finally {
			pm.close();
		}
	}
	
	//Game Management
	public GameCreated createGame(String hostUUID, ArrayList<String> playerUUIDs) {
		PersistenceManager pm = getPersistenceManager();
		ArrayList<ArrayList<GameUserImage>> faceImages = 
				new ArrayList<ArrayList<GameUserImage>>();
		Game g = new Game(hostUUID, playerUUIDs);
		try {
			GameUser player;
			for (String a: playerUUIDs) {
				player = pm.getObjectById(GameUser.class, a);
				ArrayList<GameUserImage> playerImages = new ArrayList<GameUserImage>();
				for (String i: player.getUsrImageUUIDs()) {
					playerImages.add(pm.getObjectById(GameUserImage.class, i));
				}
				faceImages.add(playerImages);
			}
			pm.makePersistent(g);
		} finally {
			pm.close();
		}
		GameCreated retObject = new GameCreated(faceImages, g.getUUID());
		return retObject;
	}
	
	public GameStarted startGame(String gameUUID, GameData data) {
		PersistenceManager pm = getPersistenceManager();
		GameStarted retObject;
		ArrayList<HashMap<String, String>> apnData =
				new ArrayList<HashMap<String, String>>();
		try {
			Game g = pm.getObjectById(Game.class, gameUUID);
			g.setGamePlayDataUUID(data.getUUID());
			g.startGame();
			
			//Assigns Targets
			ArrayList<String> playerUUIDs = g.getPlayerUUIDs();
			Collections.shuffle(playerUUIDs);
			int numPlayers = playerUUIDs.size();
			for (int i= 0; i < numPlayers; i ++) {
				GameUser hunter = pm.getObjectById(GameUser.class, playerUUIDs.get(i));
				hunter.setTargetUUID(gameUUID, playerUUIDs.get((i+1)%numPlayers));
			}
			
			//Collect APN Information
			String hostUUID = g.getHostUUID();
			for (String usrUUID: playerUUIDs) {
				if (usrUUID != hostUUID) {
					GameUser usr = pm.getObjectById(GameUser.class, usrUUID);
					HashMap<String, String> entry = 
							new HashMap<String, String>();
					entry.put("apn", usr.getAPN());
					entry.put("platformId", usr.getPlatformID());
					apnData.add(entry);
				}
			}
		} finally {
			pm.close();
		}
		retObject = new GameStarted(apnData);
		return retObject;
	}

	public GameData getGamePlayData(String gameUUID) {
		PersistenceManager pm = getPersistenceManager();
		GameData retObject;
		try {
			Key k = KeyFactory.createKey(Game.class.getSimpleName(), gameUUID);
			Game g = pm.getObjectById(Game.class, k);
			retObject = pm.getObjectById(GameData.class, g.getGamePlayDataUUID());
		} finally {
			pm.close();
		}
		return retObject;
	}

	/*public GameStarted restartGame(String gameUUID) {
		PersistenceManager pm = getPersistenceManager();
		GameStarted retObject;
		ArrayList<HashMap<String, String>> array =
				new ArrayList<HashMap<String, String>>();
		try {
			Key k = KeyFactory.createKey(Game.class.getSimpleName(), gameUUID);
			Game g = pm.getObjectById(Game.class, k);
			g.startGame();
			String hostUUID = g.getHost().getUUID();
			for (GameUser a: g.getPlayerList()) {
				if (a.getUUID() != hostUUID) {
					HashMap<String, String> entry = 
							new HashMap<String, String>();
					entry.put("apn", a.getAPN());
					entry.put("platformId", a.getPlatformID());
					array.add(entry);
				}
			}
		} finally {
			pm.close();
		}
		retObject = new GameStarted(array);
		return retObject;
	}*/
	
	//Game Play
	public String getTarget(String gameUUID, String userUUID) {
		PersistenceManager pm = getPersistenceManager();
		String retVal;
		try {
			GameUser a = pm.getObjectById(GameUser.class, userUUID);
			retVal = a.getTargetUUID(gameUUID);
		} finally {
			pm.close();
		}
		return retVal;
	}
	
	public GameUserImage getUsrThumbnail(String usrUUID) {
		PersistenceManager pm = getPersistenceManager();
		GameUserImage retObject;
		try {
			GameUser a = pm.getObjectById(GameUser.class, usrUUID);
			retObject = pm.getObjectById(GameUserImage.class, a.getThumbnailUUID());
		} finally {
			pm.close();
		}
		return retObject;
	}
	
	public UserKilled killUser(String gameUUID, String assassinUUID, String victimUUID) {
		PersistenceManager pm = getPersistenceManager();
		ArrayList<HashMap<String, String>> victimAPN = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> otherAPN = new ArrayList<HashMap<String, String>>();
		String victimCodeName;
		String nextTargetUUID;
		try {
			Game g = pm.getObjectById(Game.class, gameUUID);
			GameUser assassin = pm.getObjectById(GameUser.class, assassinUUID);
			GameUser victim = pm.getObjectById(GameUser.class, victimUUID);
			
			nextTargetUUID = killUser(gameUUID, assassin, victim);
			victimCodeName = victim.getUserCodeName();
			
			//Collects APN information
			for (String usrUUID: g.getPlayerUUIDs()) {
				GameUser usr = pm.getObjectById(GameUser.class, usrUUID);
				HashMap<String, String> entry = new HashMap<String, String>();
				entry.put("apn",usr.getAPN());
				entry.put("platformId", usr.getPlatformID());
				if (usrUUID == victimUUID) {
					victimAPN.add(entry);
				} else if (usrUUID != assassinUUID) {
					otherAPN.add(entry);
				}
			}
		} finally {
			pm.close();
		}
		UserKilled retObject = new UserKilled(otherAPN, victimAPN, 
				victimCodeName, nextTargetUUID);
		return retObject;
	}
	
	public GameEnded endGame(String gameUUID, String winnerUUID) {
		PersistenceManager pm = getPersistenceManager();
		GameEnded retObject;
		ArrayList<HashMap<String, String>> apnData =
				new ArrayList<HashMap<String, String>>();
		String winnerCode_Name;
		try {
			Game g = pm.getObjectById(Game.class, gameUUID);
			GameUser winner = pm.getObjectById(GameUser.class, winnerUUID);
			winnerCode_Name = winner.getUserCodeName();
			g.endGame(winner);
			winner.addWin(); //Increments Winner's win count
			for (String usrUUID: g.getPlayerUUIDs()) {
				if (usrUUID != winnerUUID) {
					GameUser usr = pm.getObjectById(GameUser.class, usrUUID);
					HashMap<String, String> entry = 
							new HashMap<String, String>();
					entry.put("apn", usr.getAPN());
					entry.put("platformId", usr.getPlatformID());
					apnData.add(entry);
				}
			}
		} finally {
			pm.close();
		}
		retObject = new GameEnded(apnData, winnerCode_Name);
		return retObject;
	}
	
	//Returns an instance of the PersistenceManager
	private PersistenceManager getPersistenceManager() {
		return JDOHelper.getPersistenceManagerFactory("transactions-optional")
				.getPersistenceManager();
	}
	
	private String killUser(String gameUUID, GameUser assassin, GameUser victim) {
		assassin.addKill();
		victim.addDeath();
		assassin.setTargetUUID(gameUUID, victim.getTargetUUID(gameUUID)); //Assigns new target to assassin
		victim.removeTarget(gameUUID);
		return assassin.getTargetUUID(gameUUID);
	}
		


}