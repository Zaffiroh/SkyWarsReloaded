package com.walrusone.skywarsreloaded.objects;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.walrusone.skywarsreloaded.managers.MatchManager;
import com.walrusone.skywarsreloaded.managers.WorldManager;
import com.walrusone.skywarsreloaded.objects.GameKit;
import com.walrusone.skywarsreloaded.objects.GameMap;
import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.MatchState;
import com.walrusone.skywarsreloaded.enums.Vote;
import com.walrusone.skywarsreloaded.listeners.LobbyListener;
import com.walrusone.skywarsreloaded.utilities.GameMapComparator;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import com.walrusone.skywarsreloaded.utilities.Util;

import java.io.File;
import java.io.IOException;

import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;

public class GameMap {
 
	private static ArrayList<GameMap> arenas;
	private static Map<String, MapData> mapData = new HashMap<String, MapData>();
	private static Inventory arenasManager = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Arenas Manager");	
	
	private boolean forceStart;
	private boolean allowFallDamage;
	private boolean allowRegen;
    private boolean thunder;
    private String winner = "";
    private int strikeCounter;
    private int nextStrike;
    private MatchState matchState;
    private HashMap<Integer, PlayerCard> playerCards;
    private ArrayList<UUID> dead = new ArrayList<UUID>();
    private ArrayList<UUID> spectators = new ArrayList<UUID>();
    private String name;
    private int timer;
    private int displayTimer = 0;
    private int restartTimer = -1;
    private int minPlayers;
    private GameKit kit;
    private Inventory arenaManager;
    private Inventory kitsVoteMenu;
    private Inventory timeVoteMenu;
    private Inventory chestVoteMenu;
    private Inventory weatherVoteMenu;
    private Inventory modifierVoteMenu;
    private String currentTime;
    private String currentChest;
    private String currentWeather;
    private String currentModifier;
    private Map<GameKit, Boolean> availableKits;
	private Map<Integer, EmptyChest> chests = Maps.newHashMap();
	private Map<Integer, EmptyChest> doubleChests = Maps.newHashMap();
	private int mapUseCount;
	private String displayName;
	private String designedBy;
	private ArrayList<SWRSign> signs;
	private Scoreboard scoreboard;
	private Objective objective;
	private MapData mp;
	private boolean registered;

	private static final ArrayList<String> timeItemList = new ArrayList<>(Arrays.asList("timerandom", "timedawn", "timenoon", "timedusk", "timemidnight"));
	private static final ArrayList<Vote> timeVoteList = new ArrayList<>(Arrays.asList(Vote.TIMERANDOM, Vote.TIMEDAWN, Vote.TIMENOON, Vote.TIMEDUSK, Vote.TIMEMIDNIGHT));
	private static final ArrayList<String> chestItemList = new ArrayList<>(Arrays.asList("chestrandom", "chestbasic", "chestnormal", "chestop", "chestscavenger"));
	private static final ArrayList<Vote> chestVoteList = new ArrayList<>(Arrays.asList(Vote.CHESTRANDOM, Vote.CHESTBASIC, Vote.CHESTNORMAL, Vote.CHESTOP, Vote.CHESTSCAVENGER));
	private static final ArrayList<String> weatherItemList = new ArrayList<>(Arrays.asList("weatherrandom", "weathersunny", "weatherrain", "weatherstorm", "weathersnow"));
	private static final ArrayList<Vote> weatherVoteList = new ArrayList<>(Arrays.asList(Vote.WEATHERRANDOM, Vote.WEATHERSUN, Vote.WEATHERRAIN, Vote.WEATHERTHUNDER, Vote.WEATHERSNOW));
	private static final ArrayList<String> modifierItemList = new ArrayList<>(Arrays.asList("modifierrandom", "modifierspeed", "modifierjump", "modifierstrength", "modifiernone"));
	private static final ArrayList<Vote> modifierVoteList = new ArrayList<>(Arrays.asList(Vote.MODIFIERRANDOM, Vote.MODIFIERSPEED, Vote.MODIFIERJUMP, Vote.MODIFIERSTRENGTH, Vote.MODIFIERNONE));
			
    public GameMap(final String name, boolean forceRegister, boolean startup) {
    	playerCards = new HashMap<Integer, PlayerCard>();
    	mp = mapData.get(name);
        if (mp == null) {
        	mapData.put(name, new MapData(name, name, ((int) playerCards.size()/2) >= 2 ? (int) playerCards.size()/2 : 2 , "", null, forceRegister));
         	saveMapData();
         	mp = mapData.get(name);
        }
         
        this.minPlayers = mp.getMinPlayers() >= 2 ? mp.getMinPlayers() : 2;
        this.displayName = mp.getDisplayName();
        this.designedBy = mp.getCreator();
        signs = new ArrayList<SWRSign>();
		this.name = name.toLowerCase();
		
        this.thunder = false;
        allowRegen = true;
        
        if(startup) {
        	this.registered = mp.isRegistered();
        } else {
        	this.registered = forceRegister;
        }
        this.matchState = MatchState.OFFLINE;
        timer = SkyWarsReloaded.getCfg().getWaitTimer();
        mapUseCount = 0;
        
        if (mp.getSigns() != null) {
        	for (String sign: mp.getSigns()) {
        		signs.add(new SWRSign(this.getName(), Util.get().stringToLocation(sign)));
        	}
        }
        
        arenaManager = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Arena Manager: " + this.name);
               
        if (registered) {
        	attemptRegistration();
        } 
    }
   
	public void update() {
		updateArenasManager();
		this.updateArenaManager();
        this.updateSigns();
        this.sendBungeeUpdate();
        if (this.isRegistered()) {
            this.updateScoreboard();
        }
        LobbyListener.updateJoinMenu();
	}
	

   
	/*Player Handling Methods*/
	
	public boolean addPlayer(final Player player) {
		if (Util.get().isBusy(player.getUniqueId())) {
			return false;
		}
    	for (int i = 1; i <= playerCards.size(); i++) {
    		PlayerCard pCard = playerCards.get(i);
    		if (pCard.getPlayer() == null) {
    			pCard.setPlayer(player);
    			pCard.setPreElo(PlayerStat.getPlayerStats(player.getUniqueId()).getElo());
   			
    			MatchManager.get().teleportToArena(this, player, pCard.getSpawn());
    			if (SkyWarsReloaded.getCfg().kitVotingEnabled()) {
        	        updateKitVotes();
    			}
    	        timer = SkyWarsReloaded.getCfg().getWaitTimer();
    	        this.update();
    			return true;
    		}  		
    	}
    	this.update();
    	updateScoreboard();
    	return false;
    }
	
	public boolean addParty(final Party party) {
		ArrayList<Player> players = new ArrayList<Player>();
		for (UUID uuid: party.getMembers()) {
			Player player = Bukkit.getPlayer(uuid);
			if (Util.get().isBusy(uuid)) {
				party.sendPartyMessage(new Messaging.MessageFormatter().setVariable("player", player.getName()).format("party.memberbusy"));
			} else {
				PlayerStat ps = PlayerStat.getPlayerStats(uuid);
				for (int i = 1; i <= playerCards.size(); i++) {
		    		PlayerCard pCard = playerCards.get(i);
		    		if (pCard.getPlayer() == null) {
		    			pCard.setPlayer(player);
		    			pCard.setPreElo(ps.getElo());
		    			players.add(player);
		    			break;
		    		}
				}
			}
		}
		
		if (players.size() == party.getSize()) {
			for (Player player: players) {
				PlayerCard pCard = this.getPlayerCard(player);
				MatchManager.get().teleportToArena(this, player, pCard.getSpawn());
				if (SkyWarsReloaded.getCfg().kitVotingEnabled()) {
			        updateKitVotes();
				}
		        timer = SkyWarsReloaded.getCfg().getWaitTimer();
		        this.update();
			}
			return true;
		} else {
			for (Player player: players) {
				PlayerCard pCard = this.getPlayerCard(player);
				pCard.reset();
			}
		}
    	this.update();
    	updateScoreboard();
    	return false;
	}

	public boolean removePlayer(final Player player) {
    	for (int i = 1; i <= playerCards.size(); i++) {
    		PlayerCard pCard = playerCards.get(i);
    		if (player != null) {
    			if (pCard.getPlayer() != null) {
    				if(pCard.getPlayer().equals(player)) {
            			pCard.reset();
            			this.update();
            			return true;
            		}
    			}
    		}
    	}
    	this.update();
    	return false;
    }
 
    public ArrayList<Player> getAlivePlayers() {
    	ArrayList<Player> alivePlayers = new ArrayList<Player>();
    	for (PlayerCard pCard: playerCards.values()) {
    		if (pCard.getPlayer() != null) {
        		if (!this.getDead().contains(pCard.getPlayer().getUniqueId())) {
            		alivePlayers.add(pCard.getPlayer());
        		}
    		}
    	}
    	return alivePlayers;
    }
    
    public ArrayList<Player> getAllPlayers() {
    	ArrayList<Player> allPlayers = new ArrayList<Player>();
    	for (PlayerCard pCard: playerCards.values()) {
    		if (pCard.getPlayer() != null) {
            	allPlayers.add(pCard.getPlayer());
    		}
    	}
    	return allPlayers;
    }
    
    public boolean canAddPlayer() {
    	int playerCount = getPlayerCount();
        return (this.matchState == MatchState.WAITINGSTART && playerCount < playerCards.size() && this.registered);
    }
    
    public boolean canAddParty(Party party) {
    	int playerCount = getPlayerCount();
    	return (this.matchState == MatchState.WAITINGSTART && playerCount + party.getSize()-1 < playerCards.size() && this.registered);
    }
    
	/*Map Handling Methods*/
	
    static {
        GameMap.arenas = new ArrayList<GameMap>();
    }
	
	public static GameMap getMap(final String mapName) {
    	shuffle();
    	for (final GameMap map : GameMap.arenas) {
            if (map.name.equalsIgnoreCase(ChatColor.stripColor(mapName))) {
                return map;
            }
        }
        return null;
    }
	
	public static boolean addMap(String name, boolean forceRegister, boolean startup) {
		GameMap gMap = new GameMap(name, forceRegister, startup);
		arenas.add(new GameMap(name, forceRegister, startup));
		return gMap.isRegistered();
	}
	
	public void removeMap() {
		unregister();
		mapData.remove(name);
		File dataDirectory = new File (SkyWarsReloaded.get().getDataFolder(), "maps");
		File target = new File (dataDirectory, name);
		SkyWarsReloaded.getWM().deleteWorld(target);
		arenas.remove(this);
		saveMapData();
	}
        
    public static void loadMaps() {
    	loadMapData();
    	    	
    	GameMap.getMaps().clear();
    	File dataDirectory = SkyWarsReloaded.get().getDataFolder();
		File maps = new File (dataDirectory, "maps");
		if (maps.exists() && maps.isDirectory()) {
			for (File map : maps.listFiles()) {
				if (map.isDirectory()) {
					addMap(map.getName().toLowerCase(), true, true);
				} 
			}
		} else {
			SkyWarsReloaded.get().getLogger().info("Maps directory is missing or no Maps were found!");
		} 
		saveMapData();
    }
    
	public static World createNewMap(String mapName) {
    	World newWorld = SkyWarsReloaded.getWM().createEmptyWorld(mapName);
		if (newWorld == null) {
			return null;
		}
		newWorld.save();
		SkyWarsReloaded.getWM().unloadWorld(mapName);
		File dataDirectory = new File (SkyWarsReloaded.get().getDataFolder(), "maps");
		File target = new File (dataDirectory, mapName);
		SkyWarsReloaded.getWM().deleteWorld(target);
		File source = new File (SkyWarsReloaded.get().getServer().getWorldContainer().getAbsolutePath(), mapName);
		SkyWarsReloaded.getWM().copyWorld(source, target);
		SkyWarsReloaded.getWM().loadWorld(mapName);
		addMap(mapName, false, false);
		return SkyWarsReloaded.get().getServer().getWorld(mapName);
	}
	
	public void unregister() {
		this.registered = false;
		saveMapData();
		stopGameInProgress();
	}
	
	public void stopGameInProgress() {
		this.matchState = MatchState.OFFLINE;
		for (final UUID uuid: this.getSpectators()) {
    		final Player player = SkyWarsReloaded.get().getServer().getPlayer(uuid);
    		if (player != null) {
    			MatchManager.get().removeSpectator(this, player);
    		}
    	}
        for (final Player player : this.getAlivePlayers()) {
        	if (player != null) {
                MatchManager.get().playerLeave(player, DamageCause.CUSTOM, true, false);
        	}
        }
        SkyWarsReloaded.getWM().deleteWorld(this.getName() + "_" + this.getMapCount());
	}
	
	public boolean attemptRegistration() {
		if (!this.matchState.equals(MatchState.OFFLINE)) {
			stopGameInProgress();
		}
    	SkyWarsReloaded.get().getLogger().info("Attempting to register Map: " + name);
        boolean loaded = loadWorldForScanning(name);
        if (loaded) {
        	ChunkIterator();
 			SkyWarsReloaded.getWM().deleteWorld(name);
            if (playerCards.size() >= 2 && registered) {
            	kitsVoteMenu = Bukkit.createInventory(null, 45, new Messaging.MessageFormatter().format("menu.kit-voting-menu"));
                if (SkyWarsReloaded.getCfg().kitVotingEnabled()) {
                	availableKits = Maps.newHashMap();
                    addKits();
                }
                chestVoteMenu = Bukkit.createInventory(null, 27, new Messaging.MessageFormatter().format("menu.chest-voting-menu"));
                timeVoteMenu = Bukkit.createInventory(null, 27, new Messaging.MessageFormatter().format("menu.time-voting-menu"));
                weatherVoteMenu = Bukkit.createInventory(null, 27, new Messaging.MessageFormatter().format("menu.weather-voting-menu"));
                modifierVoteMenu = Bukkit.createInventory(null, 27, new Messaging.MessageFormatter().format("menu.modifier-voting-menu"));
               
                refreshMap();
                SkyWarsReloaded.get().getLogger().info("Registered Map " + name + "!");
                return true;
            } else {
            	this.registered = false;
             	saveMapData();
             	SkyWarsReloaded.get().getLogger().info("Could Not Register Map: " + name + " - Map must have at least 2 Spawn Points");
             	return false;
            }
        } else {
        	return false;
        }  
	}
    
    public static boolean loadWorldForScanning(String name) {
        	File dataDirectory = SkyWarsReloaded.get().getDataFolder();
    		File maps = new File (dataDirectory, "maps");
    		
    			String root = SkyWarsReloaded.get().getServer().getWorldContainer().getAbsolutePath();
    			File rootDirectory = new File(root);
    			WorldManager wm = SkyWarsReloaded.getWM();
    			File source = new File(maps, name);
    			File target = new File(rootDirectory, name);
    			wm.copyWorld(source, target);
    			boolean mapExists = false;
    			if(target.isDirectory()) {			 
    				if(target.list().length > 0) {
    		 			mapExists = true;
    				}	 
    			}
    			if (mapExists) {
    				SkyWarsReloaded.getWM().deleteWorld(name);
    			}
    			
    			wm.copyWorld(source, target);
    			
    			boolean loaded = SkyWarsReloaded.getWM().loadWorld(name);
    			if(!loaded) {
    				SkyWarsReloaded.get().getLogger().info("Could Not Load Map: " + name);
    			}
    			return loaded;
	}

	public static ArrayList<GameMap> getMaps() {
		return new ArrayList<GameMap>(arenas);
	}
	
	public static ArrayList<GameMap> getSortedMaps() {
		ArrayList<GameMap> sorted = new ArrayList<GameMap>();
		for (GameMap gMap: arenas) {
			if (gMap.isRegistered()) {
				sorted.add(gMap);
			}
		}
		Collections.sort(sorted, new GameMapComparator());
		return sorted;
	}
	
	public static ArrayList<GameMap> getSortedArenas() {
		ArrayList<GameMap> sorted = new ArrayList<GameMap>();
		for (GameMap gMap: arenas) {
			sorted.add(gMap);
		}
		Collections.sort(sorted, new GameMapComparator());
		return sorted;
	}
	
	public static boolean mapRegistered(String name) {
		for (GameMap gMap: arenas) {
			if (gMap.getName().equalsIgnoreCase(name)) {
				return gMap.isRegistered();
			}
		}
		return false;
	}
	   
	public boolean loadMap() {
			WorldManager wm = SkyWarsReloaded.getWM();
			String mapName = name + "_" + mapUseCount;
			boolean mapExists = false;
	    	File dataDirectory = SkyWarsReloaded.get().getDataFolder();
			File maps = new File (dataDirectory, "maps");
			File source = new File(maps, name);
			String root = SkyWarsReloaded.get().getServer().getWorldContainer().getAbsolutePath();
			File rootDirectory = new File(root);
			File target = new File(rootDirectory, mapName);
			if(target.isDirectory()) {			 
				if(target.list().length > 0) {
		 			mapExists = true;
				}	 
			}
			if (mapExists) {
				SkyWarsReloaded.getWM().deleteWorld(mapName);
			}
			
			wm.copyWorld(source, target);
			
			boolean loaded = SkyWarsReloaded.getWM().loadWorld(mapName);
			
			if (loaded) {
				World world = SkyWarsReloaded.get().getServer().getWorld(mapName);
			    world.setAutoSave(false);
			    world.setThundering(false);
			    world.setStorm(false);
			    world.setDifficulty(Difficulty.NORMAL);
			    world.setSpawnLocation(2000, 0, 2000);
			    world.setTicksPerAnimalSpawns(1);
			    world.setTicksPerMonsterSpawns(1);
		        world.setGameRuleValue("doMobSpawning", "false");
		        world.setGameRuleValue("mobGriefing", "false");
		        world.setGameRuleValue("doFireTick", "false");
		        world.setGameRuleValue("showDeathMessages", "false");
		        createSpawnPlatforms(world);
			}
			return loaded;
	}
	
	public void ChunkIterator() {
		World chunkWorld;
		chunkWorld = SkyWarsReloaded.get().getServer().getWorld(name);
		int mapSize = SkyWarsReloaded.getCfg().getMaxMapSize();
		int max1 = mapSize/2;
		int min1 = -mapSize/2;
		Block min = chunkWorld.getBlockAt(min1, 0, min1);
		Block max = chunkWorld.getBlockAt(max1, 0, max1);
		Chunk cMin = min.getChunk();
		Chunk cMax = max.getChunk();
		int countSpawns = 1;
		int countChests = 0;
		int countDChests = 0;
		
		for(int cx = cMin.getX(); cx < cMax.getX(); cx++) {
			for(int cz = cMin.getZ(); cz < cMax.getZ(); cz++) {
		           Chunk currentChunk = chunkWorld.getChunkAt(cx, cz);
		           currentChunk.load(true);

		           for(BlockState te : currentChunk.getTileEntities()) {
		               	if(te instanceof Beacon){
			                  Beacon beacon = (Beacon) te;
			                  Block block = beacon.getBlock().getRelative(0, -1, 0);
			                  if(!block.getType().equals(Material.GOLD_BLOCK) && !block.getType().equals(Material.IRON_BLOCK) 
			                		  && !block.getType().equals(Material.DIAMOND_BLOCK)&& !block.getType().equals(Material.EMERALD_BLOCK)) {
				                  Location loc = beacon.getLocation();
				                  playerCards.put(countSpawns, new PlayerCard(loc, null, -1, this));
				                  countSpawns++;
			                  }
			            } else if (te instanceof Chest) {
				                  Chest chest = (Chest) te;
				                  InventoryHolder ih = chest.getInventory().getHolder();
				                  if (ih instanceof DoubleChest){
				                      int x = chest.getX();
					                  int z = chest.getZ();
					                  int y = chest.getY();	
					                  doubleChests.put(countDChests, new EmptyChest(x, y, z));
						              countDChests++;
					              } else {
					                  int x = chest.getX();
					                  int z = chest.getZ();
					                  int y = chest.getY();
				                	  chests.put(countChests, new EmptyChest(x, y, z));
					                  countChests++;
				                  }
			            } 
		           }
		        }
	     }
		
	}
	
	public void refreshMap() {
		for (PlayerCard pCard: playerCards.values()) {
			pCard.reset();
		}
		thunder = false;
		forceStart = false;
		allowRegen = true;
        dead.clear();
        kit = null;
        kitsVoteMenu.clear();
        setDisplayTimer(0);
        restartTimer = -1;
        winner = "";
		if (SkyWarsReloaded.getCfg().kitVotingEnabled()) {
			availableKits.clear();
	        addKits();
		}
        prepareMenu(chestVoteMenu, "chest");
        prepareMenu(timeVoteMenu, "time");
        prepareMenu(weatherVoteMenu, "weather");
        prepareMenu(modifierVoteMenu, "modifier");
        SkyWarsReloaded.getWM().deleteWorld(name + "_" + mapUseCount);
        mapUseCount++;
        this.loadMap();
        matchState = MatchState.WAITINGSTART;
        getScoreBoard();
        MatchManager.get().start(this);
        update();
	}
	
	/*Inventories*/
	
	public void updateArenaManager() {
		if (!arenaManager.getViewers().isEmpty()) {
			List<String> lores = new ArrayList<String>();
			lores.add(ChatColor.GOLD + "" + minPlayers);
			lores.add(ChatColor.AQUA + "Left Click to Increase.");
			lores.add(ChatColor.AQUA + "Right Click to Decrease.");
			ItemStack min = SkyWarsReloaded.getNMS().getItemStack(new ItemStack(Material.DIAMOND_HELMET, 1), lores, "Minimun Players");
			
			
			lores.clear();
			if (registered) {
				lores.add(ChatColor.GREEN + "REGISTERED");
				lores.add(ChatColor.GOLD + matchState.toString().toUpperCase());
				lores.add(ChatColor.GREEN + "" + getAlivePlayers().size() + " of " + playerCards.size() + " Players");
				lores.add(" ");
				lores.add(ChatColor.RED + "Shift Left Click to Unregister.");
				lores.add(ChatColor.RED + "Unregistering will end the match!");
			} else {
				lores.add(ChatColor.RED + "UNREGISTERED");
				lores.add(" ");
				lores.add(ChatColor.AQUA + "Shift Left Click to");
				lores.add(ChatColor.AQUA + "Attempt Registration!");
			}
			ItemStack status = SkyWarsReloaded.getNMS().getItemStack(new ItemStack(Material.MAP, 1), lores, "Arena Status");
			
			lores.clear();
			lores.add(ChatColor.translateAlternateColorCodes('&', displayName));
			lores.add(" ");
			lores.add(ChatColor.AQUA + "Left Click to Change");
			lores.add(ChatColor.AQUA + "the Display Name.");
			ItemStack display = SkyWarsReloaded.getNMS().getItemStack(new ItemStack(Material.NAME_TAG, 1), lores, "Display Name");
			
			lores.clear();
			lores.add(ChatColor.translateAlternateColorCodes('&', getDesigner()));
			lores.add(" ");
			lores.add(ChatColor.AQUA + "Left Click to Change");
			lores.add(ChatColor.AQUA + "the Creator.");
			ItemStack creator = SkyWarsReloaded.getNMS().getItemStack(new ItemStack(Material.SKULL_ITEM, 1, (short) 3), lores, "Map Creator");
			
			lores.clear();
			lores.add(ChatColor.GOLD + "" + signs.size() + " Sign Available!");
			lores.add(" ");
			lores.add(ChatColor.AQUA + "Left Click for a list");
			lores.add(ChatColor.AQUA + "of Sign Locations.");
			ItemStack signs = SkyWarsReloaded.getNMS().getItemStack(new ItemStack(Material.SIGN, 1), lores, "Join Signs");
			
			lores.clear();
			if (registered) {
				lores.add(ChatColor.AQUA + "Left Click to spectate!");
			} else {
				lores.add(ChatColor.RED + "Spectate Currently Unavailable!");
			}
			ItemStack spectate = SkyWarsReloaded.getNMS().getItemStack(new ItemStack(Material.COMPASS, 1), lores, "Spectate");
			
			lores.clear();
			lores.add(ChatColor.AQUA + "Shift Left Click to");
			lores.add(ChatColor.AQUA + "End Current Match!");
			ItemStack end = SkyWarsReloaded.getNMS().getItemStack(new ItemStack(Material.ENDER_PORTAL_FRAME, 1), lores, "End Match");
			
			lores.clear();
			lores.add(ChatColor.AQUA + "Shift Left Click to Edit Map!");
			if (registered) {
				lores.add(ChatColor.RED + "Editing will End Match");
				lores.add(ChatColor.RED + "and unregister the map!");
			}
			ItemStack edit = SkyWarsReloaded.getNMS().getItemStack(new ItemStack(Material.WORKBENCH, 1), lores, "Edit Map");
			
			arenaManager.setItem(0, status);
			arenaManager.setItem(2, display);
			arenaManager.setItem(4, creator);
			arenaManager.setItem(6, min);
			arenaManager.setItem(8, signs);
			arenaManager.setItem(10, spectate);
			arenaManager.setItem(12, end);
			arenaManager.setItem(14, edit);
			
			ItemStack item = new ItemStack(Material.BARRIER, 1);
			lores.clear();
			lores.add(ChatColor.YELLOW + "Return to Arenas Menu");
			arenaManager.setItem(arenaManager.getSize()-1, SkyWarsReloaded.getNMS().getItemStack(item, lores, "EXIT"));
		}
	}
	
	
	public void openArenaManager(Player player) {
		if (player.hasPermission("sw.arenas")) {
			player.openInventory(arenaManager);
		}
	}
	
	public static void openArenasManager(Player player) {
		if (player.hasPermission("sw.arenas")) {
			player.openInventory(arenasManager);
		}
	}
	
	public static void updateArenasManager() {
		if (!arenasManager.getViewers().isEmpty()) {
			ArrayList<GameMap> maps = getSortedArenas();
			if (arenasManager.getSize() < maps.size()) {
				int rowCount = 9;
		        double numRows = maps.size() / 5.0;
		        for (int i = 0; i < Math.ceil(numRows)-1; i++) {
		        	rowCount += 9;
		        }
		        arenasManager = Bukkit.createInventory(null, rowCount, ChatColor.DARK_PURPLE + "Arenas Manager");
			}
			
			List<String> lores = new ArrayList<String>();
			int i = 0;
			for(GameMap gMap: maps) {
				ItemStack item = new ItemStack(Material.WOOL, 1, (short) 13);
				if (!gMap.isRegistered()) {
					item = new ItemStack(Material.WOOL, 1, (short) 14);
				}
				lores.clear();
				lores.add(ChatColor.AQUA + "Display Name: " + ChatColor.translateAlternateColorCodes('&', gMap.getDisplayName()));
				lores.add(ChatColor.AQUA + "Creator: " + ChatColor.translateAlternateColorCodes('&', gMap.getDesigner()));
				if (gMap.isRegistered()) {
					lores.add(ChatColor.AQUA + "Status: " + ChatColor.GREEN + "REGISTERED");
				} else {
					lores.add(ChatColor.AQUA + "Status: " + ChatColor.RED + "UNREGISTERED");
				}
				lores.add(ChatColor.AQUA + "Match State: " + ChatColor.GOLD + gMap.getMatchState().toString());
				lores.add(ChatColor.AQUA + "Minimum Players: " + ChatColor.GOLD + gMap.getMinPlayers());
				lores.add(ChatColor.AQUA + "Current Players: " + ChatColor.GOLD + gMap.getAlivePlayers().size() + " of " + gMap.getMaxPlayers());
				lores.add(ChatColor.AQUA + "Number of Join Signs: " + ChatColor.GOLD + gMap.getSigns().size());
				arenasManager.setItem(i, SkyWarsReloaded.getNMS().getItemStack(item, lores, ChatColor.DARK_PURPLE + gMap.getName()));
				i++;
			}
			
			ItemStack item = new ItemStack(Material.BARRIER, 1);
			lores.clear();
			lores.add(ChatColor.YELLOW + "Close Menu");
			arenasManager.setItem(arenasManager.getSize()-1, SkyWarsReloaded.getNMS().getItemStack(item, lores, "EXIT"));	
		}
	}
	
    /*Kit voting methods*/
	
    private void addKits() {
    	for (GameKit gameKit: GameKit.getAvailableKits()) {
    		if (kitsVoteMenu.getItem(gameKit.getPosition()) == null || 
    				kitsVoteMenu.getItem(gameKit.getPosition()).getType().equals(Material.AIR)) {
    			loadKit(null, null, gameKit);    			
    		}
    	}
	}
    
    public void loadKit(Player player, GameMap gMap, GameKit gameKit) {
    	List<String> lores = gameKit.getColorLores();
    	ItemStack kit = null;
    	boolean state = false;
    	if (player == null) {
    		if (gameKit.needPermission()) {
    			state = true;
    		}
    	}    	
      	if (!state) {
    		kit = gameKit.getIcon();
    		kit.setAmount(1);
		} else {
    		kit = gameKit.getLIcon();
    		kit.setAmount(1);
    		lores.add(" ");
    		lores.add(gameKit.getColoredLockedLore());
		}
		kit = SkyWarsReloaded.getNMS().getItemStack(kit, lores, gameKit.getColorName());
		kitsVoteMenu.setItem(gameKit.getPosition(), kit);
		availableKits.put(gameKit, state);
		if (player != null && gMap != null) {
			MatchManager.get().message(gMap, new Messaging.MessageFormatter().setVariable("player", player.getName()).setVariable("kit", gameKit.getColorName()).format("game.unlock-kit"));
		}
    }
       
	public void setKitVote(Player player, GameKit kit2) {
		for (PlayerCard pCard: playerCards.values()) {
			if (pCard.getPlayer().equals(player)) {
				pCard.setKitVote(kit2);
				return;
			}
		}
	}
    
	public void updateKitVotes() {
		HashMap <GameKit, Integer> votes = new HashMap<GameKit, Integer>();
		for (GameKit gKit: availableKits.keySet()) {
			votes.put(gKit, 0);
		}
		
		for (PlayerCard pCard: playerCards.values()) {
			Player player = pCard.getPlayer();
			if (player != null) {
				GameKit gKit = pCard.getKitVote();
				if (gKit != null) {
					int multiplier = Util.get().getMultiplier(player);
					votes.put(gKit, votes.get(gKit) + (1 * multiplier));
				}
			}	
		}
		for (GameKit gKit: votes.keySet()) {
			boolean locked = availableKits.get(gKit);
			ItemStack kit = null;
			List<String> lores = gKit.getColorLores(); 
			if (locked) {
				kit = gKit.getLIcon();
				lores.add(" ");
				lores.add(gKit.getColoredLockedLore());
			} else {
				kit = gKit.getIcon();
				lores.add(" ");
				lores.add(new Messaging.MessageFormatter().setVariable("number", "" + votes.get(gKit)).format("game.vote-display"));
			}
			kit.setAmount(votes.get(gKit) == 0 ? 1 : votes.get(gKit));
			kit = SkyWarsReloaded.getNMS().getItemStack(kit, lores, ChatColor.translateAlternateColorCodes('&', gKit.getColorName()));
    		kitsVoteMenu.setItem(gKit.getPosition(), kit);
		}
		
	}
	
    public void getVotedKit() {
    	HashMap <GameKit, Integer> votes = new HashMap<GameKit, Integer>();
		for (GameKit gKit: availableKits.keySet()) {
			votes.put(gKit, 0);
		}
		
		for (PlayerCard pCard: playerCards.values()) {
			Player player = pCard.getPlayer();
			if (player != null) {
				GameKit gKit = pCard.getKitVote();
				if (gKit != null) {
					int multiplier = Util.get().getMultiplier(player);
					while (gKit.getName().equalsIgnoreCase(new Messaging.MessageFormatter().format("kit.vote-random"))) {
						Random rand = new Random();
						int n = rand.nextInt(GameKit.getAvailableKits().size());
						gKit = GameKit.getAvailableKits().get(n);
					}
					votes.put(gKit, votes.get(gKit) + (1 * multiplier));
				}
			}	
		}
		int highest = 0;
		GameKit voted = null;
		for (GameKit gKit: votes.keySet()) {
			if (votes.get(gKit) >= highest) {
				highest = votes.get(gKit);
				voted = gKit;
			}
		}
		this.kit = voted;
    }
    
    public GameKit getSelectedKit(Player player) {
    	for (PlayerCard pCard: playerCards.values()) {
    		if (pCard != null) {
    			if (player.equals(pCard.getPlayer())) {
    				return pCard.getKitVote();
    			}
    		}
    	}
    	return null;
    }
    
	public boolean isKitLocked(GameKit kit2) {
		return availableKits.get(kit2);
	}
    
    public Inventory getKitsVoteMenu() {
    	return kitsVoteMenu;
    }
    
    public Inventory getChestVoteMenu() {
    	return chestVoteMenu;
    }   
    
    /*Other Voting Menu Methods*/
    
    private void prepareMenu(Inventory inv, String type) {
		ArrayList<String> itemList = null;
		
		if (type.equalsIgnoreCase("time")) {
			itemList = timeItemList;
		} else if (type.equalsIgnoreCase("chest")) {
			itemList = chestItemList;
		} else if (type.equalsIgnoreCase("weather")) {
			itemList = weatherItemList;
		} else if (type.equalsIgnoreCase("modifier")) {
			itemList = modifierItemList;
		}
		
		inv.clear();
		inv.setItem(9, SkyWarsReloaded.getIM().getItem(itemList.get(0)));
		inv.setItem(11, SkyWarsReloaded.getIM().getItem(itemList.get(1)));
		inv.setItem(13, SkyWarsReloaded.getIM().getItem(itemList.get(2)));
		inv.setItem(15, SkyWarsReloaded.getIM().getItem(itemList.get(3)));
		inv.setItem(17, SkyWarsReloaded.getIM().getItem(itemList.get(4)));
		inv.setItem(inv.getSize()-1, SkyWarsReloaded.getIM().getItem("exitMenuItem"));
		
		currentTime = getVoteString(Vote.TIMENOON);
		currentChest = getVoteString(Vote.CHESTNORMAL);
		currentWeather = getVoteString(Vote.WEATHERSUN);
		currentModifier = getVoteString(Vote.MODIFIERNONE);
	}
    
	public void setVote(Player player, Vote vote, String type) {
		for (PlayerCard pCard: playerCards.values()) {
			if (pCard.getPlayer().equals(player)) {
				if (type.equalsIgnoreCase("time")) {
					pCard.setGameTime(vote);
					return;
				} else if (type.equalsIgnoreCase("chest")) {
					pCard.setChestVote(vote);
					return;
				} else if (type.equalsIgnoreCase("weather")) {
					pCard.setWeather(vote);
					return;
				} else if (type.equalsIgnoreCase("modifier")) {
					pCard.setModifier(vote);
					return;
				}
				
			}
		}
	}
	
	public void updateVotes(String type) {
		HashMap <Vote, Integer> votes = getVotes(type, false);
		
		ArrayList<String> itemList = null;
		ArrayList<Vote> voteList = null;
		Inventory inv = null;
		
		if (type.equalsIgnoreCase("time")) {
			itemList = timeItemList;
			voteList = timeVoteList;
			inv = timeVoteMenu;
		} else if (type.equalsIgnoreCase("chest")) {
			itemList = chestItemList;
			voteList = chestVoteList;
			inv = chestVoteMenu;
		} else if (type.equalsIgnoreCase("weather")) {
			itemList = weatherItemList;
			voteList = weatherVoteList;
			inv = weatherVoteMenu;
		} else if (type.equalsIgnoreCase("modifier")) {
			itemList = modifierItemList;
			voteList = modifierVoteList;
			inv = modifierVoteMenu;
		}
		
		for (Vote vote: votes.keySet()) {
			if (vote == voteList.get(0)) {
				updateSlot(votes, vote, 0, 9, itemList, inv);
			} else if (vote == voteList.get(1)) {
				updateSlot(votes, vote, 1, 11, itemList, inv);
			} else if (vote == voteList.get(2)) {
				updateSlot(votes, vote, 2, 13, itemList, inv);
			} else if (vote == voteList.get(3)) {
				updateSlot(votes, vote, 3, 15, itemList, inv);
			} else if (vote == voteList.get(4)) {
				updateSlot(votes, vote, 4, 17, itemList, inv);
			}
		}
		
		if (type.equalsIgnoreCase("time")) {
			currentTime = getVoteString(getVoted("time"));
		} else if (type.equalsIgnoreCase("chest")) {
			currentChest = getVoteString(getVoted("chest"));
		} else if (type.equalsIgnoreCase("weather")) {
			currentWeather = getVoteString(getVoted("weather"));
		} else if (type.equalsIgnoreCase("modifier")) {
			currentModifier = getVoteString(getVoted("modifier"));
		}
		updateScoreboard();
	}
	
	private void updateSlot(HashMap <Vote, Integer> votes, Vote vote, int count, int slot, ArrayList<String> itemList, Inventory inv) {
		ItemStack item = SkyWarsReloaded.getIM().getItem(itemList.get(count));
		item.setAmount(votes.get(vote) == 0 ? 1 : votes.get(vote));
		ItemMeta itemMeta = item.getItemMeta();
		List<String> lores = itemMeta.getLore();
		lores.add(" ");
		lores.add(new Messaging.MessageFormatter().setVariable("number", "" + votes.get(vote)).format("game.vote-display"));
		itemMeta.setLore(lores);
		item.setItemMeta(itemMeta);
		inv.setItem(slot, item);
	}
	
    public Vote getVoted(String type) {
    	HashMap <Vote, Integer> votes = getVotes(type, true);
				
		int highest = 0;
		Vote voted = null;
		for (Vote vote: votes.keySet()) {
			if (votes.get(vote) >= highest) {
				highest = votes.get(vote);
				voted = vote;
			}
		}
		if (highest == 0) {
			if (type.equalsIgnoreCase("time")) {
				return Vote.TIMENOON;
			} else if (type.equalsIgnoreCase("chest")) {
				return Vote.CHESTNORMAL;
			} else if (type.equalsIgnoreCase("weather")) {
				return Vote.WEATHERSUN;
			} else if (type.equalsIgnoreCase("modifier")) {
				return Vote.MODIFIERNONE;
			}
		}
		return voted;
    }
    
    private HashMap<Vote, Integer> getVotes(String type, boolean getRandom) {
    	HashMap <Vote, Integer> votes = new HashMap<Vote, Integer>();
		
		ArrayList<Vote> voteList = null;
		
		if (type.equalsIgnoreCase("time")) {
			voteList = timeVoteList;
		} else if (type.equalsIgnoreCase("chest")) {
			voteList = chestVoteList;
		} else if (type.equalsIgnoreCase("weather")) {
			voteList = weatherVoteList;
		} else if (type.equalsIgnoreCase("modifier")) {
			voteList = modifierVoteList;
		}
		
		votes.put(voteList.get(0), 0);
		votes.put(voteList.get(1), 0);
		votes.put(voteList.get(2), 0);
		votes.put(voteList.get(3), 0);
		votes.put(voteList.get(4), 0);
		
		for (PlayerCard pCard: playerCards.values()) {
			Player player = pCard.getPlayer();
			if (player != null) {
				Vote vote = pCard.getVote(type);
				if (vote != null) {
					if ((vote == Vote.TIMERANDOM || vote == Vote.WEATHERRANDOM || vote == Vote.MODIFIERRANDOM || vote == Vote.CHESTRANDOM) && getRandom) {
						vote = Vote.getRandom(type);
					}
					int multiplier = Util.get().getMultiplier(player);
					votes.put(vote, votes.get(vote) + (1 * multiplier));
				}
			}	
		}
		
		return votes;
    }
    
    public Inventory getTimeVoteMenu() {
    	return timeVoteMenu;
    }
    
    public Inventory getWeatherVoteMenu() {
    	return weatherVoteMenu;
    }
        
    public Inventory getModifierVoteMenu() {
    	return modifierVoteMenu;
    }
    
	
	
	/*Scoreboard Methods*/
	
	public void getScoreBoard() {
		if (scoreboard != null) {
            resetScoreboard();
        }
		ScoreboardManager manager = SkyWarsReloaded.get().getServer().getScoreboardManager();
		scoreboard = manager.getNewScoreboard();
        objective = scoreboard.registerNewObjective("info", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		updateScoreboard();
	}
	
	public void updateScoreboard() {
        if (objective != null) {
            objective.unregister();
        }
        objective = scoreboard.registerNewObjective("info", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        String sb = "";
        if (matchState.equals(MatchState.WAITINGSTART)) {
        	sb = "scoreboards.waitboard.line";
        } else if (matchState.equals(MatchState.PLAYING) || matchState.equals(MatchState.SUDDENDEATH)) {
        	sb = "scoreboards.playboard.line";
        } else if (matchState.equals(MatchState.ENDING)) {
        	sb = "scoreboards.endboard.line";
        	if (restartTimer == -1) {
        		startRestartTimer();
        	}
        }
        
        for (int i = 1; i < 17; i++) {
        	if (i == 1) {
    	        String leaderboard = getScoreboardLine(sb + i);
    	        objective.setDisplayName(leaderboard);
    		} else {
    			String s = "";
    			if (getScoreboardLine(sb + i).length() == 0) {
    				for (int j = 0; j < i; j++) {
    					s = s + " ";
    				}
    			} else {
    				s = getScoreboardLine(sb + i);
    			}
    			if (!s.equalsIgnoreCase("remove")) {
        			Score score = objective.getScore(s);
    				score.setScore(17-i);
    			}
    		}
        }	
	}
	
	private void startRestartTimer() {
		restartTimer = SkyWarsReloaded.getCfg().getTimeAfterMatch();
		if (SkyWarsReloaded.get().isEnabled()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					restartTimer--;
					if (restartTimer == 0) {
						this.cancel();
					}
					updateScoreboard();
				}
			}.runTaskTimer(SkyWarsReloaded.get(), 0, 20);
		}
	}
	

	
	private String getScoreboardLine(String lineNum) {
		return new Messaging.MessageFormatter()
				.setVariable("mapname", displayName)
				.setVariable("time", "" + Util.get().getFormattedTime(displayTimer))
				.setVariable("players", "" + getAlivePlayers().size())
				.setVariable("maxplayers", "" + playerCards.size())
				.setVariable("winner", winner)
				.setVariable("restarttime", "" + restartTimer)
				.setVariable("chestvote", ChatColor.stripColor(currentChest))
				.setVariable("timevote", ChatColor.stripColor(currentTime))
				.setVariable("weathervote", ChatColor.stripColor(currentWeather))
				.setVariable("modifiervote", ChatColor.stripColor(currentModifier))
				.format(lineNum);
	}
	
	private String getVoteString(Vote vote) {
		switch(vote) {
		case CHESTRANDOM: return new Messaging.MessageFormatter().format("items.chest-random");
		case CHESTBASIC: return new Messaging.MessageFormatter().format("items.chest-basic");
		case CHESTNORMAL: return new Messaging.MessageFormatter().format("items.chest-normal");
		case CHESTOP: return new Messaging.MessageFormatter().format("items.chest-op");
		case CHESTSCAVENGER: return new Messaging.MessageFormatter().format("items.chest-scavenger");
		case TIMERANDOM: return new Messaging.MessageFormatter().format("items.time-random");
		case TIMEDAWN: return new Messaging.MessageFormatter().format("items.time-dawn");
		case TIMENOON: return new Messaging.MessageFormatter().format("items.time-noon");
		case TIMEDUSK: return new Messaging.MessageFormatter().format("items.time-dusk");
		case TIMEMIDNIGHT: return new Messaging.MessageFormatter().format("items.time-midnight");
		case WEATHERRANDOM: return new Messaging.MessageFormatter().format("items.weather-random");
		case WEATHERSUN: return new Messaging.MessageFormatter().format("items.weather-sunny");
		case WEATHERRAIN: return new Messaging.MessageFormatter().format("items.weather-rain");
		case WEATHERTHUNDER: return new Messaging.MessageFormatter().format("items.weather-storm");
		case WEATHERSNOW: return new Messaging.MessageFormatter().format("items.weather-snow");
		case MODIFIERRANDOM: return new Messaging.MessageFormatter().format("items.modifier-random");
		case MODIFIERSPEED: return new Messaging.MessageFormatter().format("items.modifier-speed");
		case MODIFIERJUMP: return new Messaging.MessageFormatter().format("items.modifier-jump");
		case MODIFIERSTRENGTH: return new Messaging.MessageFormatter().format("items.modifier-strength");
		case MODIFIERNONE: return new Messaging.MessageFormatter().format("items.modifier-none");
		default: return "";
		}
	}
	
    private void resetScoreboard() {
        if (objective != null) {
            objective.unregister();
        }
        
        if (scoreboard != null) {
            scoreboard = null;
        }
    }

	public Scoreboard getScoreboard() {
		return scoreboard;
	}

	
	
	/*Glass Color Handling Methods*/
	
	public void createSpawnPlatforms(World world) {
    	for(PlayerCard pCard: playerCards.values()) {
            int x = pCard.getSpawn().getBlockX();
            int y = pCard.getSpawn().getBlockY();
            int z = pCard.getSpawn().getBlockZ();

            world.getBlockAt(x, y, z).setType(Material.GLASS);
            world.getBlockAt(x, y + 1, z + 1).setType(Material.GLASS);
            world.getBlockAt(x, y + 1, z - 1).setType(Material.GLASS);
            world.getBlockAt(x + 1, y + 1, z).setType(Material.GLASS);
            world.getBlockAt(x - 1, y + 1, z).setType(Material.GLASS);
            world.getBlockAt(x, y + 2, z + 1).setType(Material.GLASS);
            world.getBlockAt(x, y + 2, z - 1).setType(Material.GLASS);
            world.getBlockAt(x + 1, y + 2, z).setType(Material.GLASS);
            world.getBlockAt(x - 1, y + 2, z).setType(Material.GLASS);
            world.getBlockAt(x, y + 3, z + 1).setType(Material.GLASS);
            world.getBlockAt(x, y + 3, z - 1).setType(Material.GLASS);
            world.getBlockAt(x + 1, y + 3, z).setType(Material.GLASS);
            world.getBlockAt(x - 1, y + 3, z).setType(Material.GLASS);
            world.getBlockAt(x, y + 4, z).setType(Material.GLASS);
    	}
    }
	
	@SuppressWarnings("deprecation")
	public void setGlassColor(Player player, String color) {
		if (matchState == MatchState.WAITINGSTART) {
			PlayerCard pCard = null;
			for (PlayerCard playerCard: playerCards.values()) {
				if (playerCard.getPlayer().equals(player)) {
					pCard = playerCard;
					break;
				}
			}
			
			if (pCard != null) {
				World mapWorld;
				mapWorld = SkyWarsReloaded.get().getServer().getWorld(this.getName() + "_" + this.getMapCount());
	            int x = pCard.getSpawn().getBlockX();
	            int y = pCard.getSpawn().getBlockY();
	            int z = pCard.getSpawn().getBlockZ();
	            
				byte cByte = Util.get().getByteFromColor(color.toLowerCase());
				if (cByte <= -1) {
					Material material = Material.GLASS;
					GlassColor gColor = SkyWarsReloaded.getLM().getGlassByColor(color.toLowerCase());
					if (gColor != null) {
						material = gColor.getItem().getType();
					}
					 mapWorld.getBlockAt(x, y, z).setType(material);
					 mapWorld.getBlockAt(x, y + 1, z + 1).setType(material);
			         mapWorld.getBlockAt(x, y + 1, z - 1).setType(material);
			         mapWorld.getBlockAt(x + 1, y + 1, z).setType(material);
			         mapWorld.getBlockAt(x - 1, y + 1, z).setType(material);
			         mapWorld.getBlockAt(x, y + 2, z + 1).setType(material);
			         mapWorld.getBlockAt(x, y + 2, z - 1).setType(material);
			         mapWorld.getBlockAt(x + 1, y + 2, z).setType(material);
			         mapWorld.getBlockAt(x - 1, y + 2, z).setType(material);
			         mapWorld.getBlockAt(x, y + 3, z + 1).setType(material);
			         mapWorld.getBlockAt(x, y + 3, z - 1).setType(material);
			         mapWorld.getBlockAt(x + 1, y + 3, z).setType(material);
			         mapWorld.getBlockAt(x - 1, y + 3, z).setType(material);
			         mapWorld.getBlockAt(x, y + 4, z).setType(material);
				} else {
		            mapWorld.getBlockAt(x, y, z).setType(Material.STAINED_GLASS);
		            mapWorld.getBlockAt(x, y, z).setData(cByte);
		            mapWorld.getBlockAt(x, y + 1, z + 1).setType(Material.STAINED_GLASS);
		            mapWorld.getBlockAt(x, y + 1, z + 1).setData(cByte);
		            mapWorld.getBlockAt(x, y + 1, z - 1).setType(Material.STAINED_GLASS);
		            mapWorld.getBlockAt(x, y + 1, z - 1).setData(cByte);
		            mapWorld.getBlockAt(x + 1, y + 1, z).setType(Material.STAINED_GLASS);
		            mapWorld.getBlockAt(x + 1, y + 1, z).setData(cByte);
		            mapWorld.getBlockAt(x - 1, y + 1, z).setType(Material.STAINED_GLASS);
		            mapWorld.getBlockAt(x - 1, y + 1, z).setData(cByte);
		            mapWorld.getBlockAt(x, y + 2, z + 1).setType(Material.STAINED_GLASS);
		            mapWorld.getBlockAt(x, y + 2, z + 1).setData(cByte);
		            mapWorld.getBlockAt(x, y + 2, z - 1).setType(Material.STAINED_GLASS);
		            mapWorld.getBlockAt(x, y + 2, z - 1).setData(cByte);
		            mapWorld.getBlockAt(x + 1, y + 2, z).setType(Material.STAINED_GLASS);
		            mapWorld.getBlockAt(x + 1, y + 2, z).setData(cByte);
		            mapWorld.getBlockAt(x - 1, y + 2, z).setType(Material.STAINED_GLASS);
		            mapWorld.getBlockAt(x - 1, y + 2, z).setData(cByte);
		            mapWorld.getBlockAt(x, y + 3, z + 1).setType(Material.STAINED_GLASS);
		            mapWorld.getBlockAt(x, y + 3, z + 1).setData(cByte);
		            mapWorld.getBlockAt(x, y + 3, z - 1).setType(Material.STAINED_GLASS);
		            mapWorld.getBlockAt(x, y + 3, z - 1).setData(cByte);
		            mapWorld.getBlockAt(x + 1, y + 3, z).setType(Material.STAINED_GLASS);
		            mapWorld.getBlockAt(x + 1, y + 3, z).setData(cByte);
		            mapWorld.getBlockAt(x - 1, y + 3, z).setType(Material.STAINED_GLASS);
		            mapWorld.getBlockAt(x - 1, y + 3, z).setData(cByte);
		            mapWorld.getBlockAt(x, y + 4, z).setType(Material.STAINED_GLASS);
		            mapWorld.getBlockAt(x, y + 4, z).setData(cByte);
				}
			}
		}
	}
    
    public void removeSpawnHousing() {
    	World mapWorld;
		mapWorld = SkyWarsReloaded.get().getServer().getWorld(this.getName() + "_" + this.getMapCount());
		final GameMap gMap = this;
		this.allowFallDamage = false;
        new BukkitRunnable() {
			@Override
			public void run() {
				if (kit != null) {
					gMap.allowFallDamage = SkyWarsReloaded.getCfg().allowFallDamage();
				}
			}
        	
        }.runTaskLater(SkyWarsReloaded.get(), 20L);
    	for(PlayerCard pCard: playerCards.values()) {
            int x = pCard.getSpawn().getBlockX();
            int y = pCard.getSpawn().getBlockY();
            int z = pCard.getSpawn().getBlockZ();
            
            mapWorld.getBlockAt(x, y, z).setType(Material.AIR);
            mapWorld.getBlockAt(x, y + 1, z + 1).setType(Material.AIR);
            mapWorld.getBlockAt(x, y + 1, z - 1).setType(Material.AIR);
            mapWorld.getBlockAt(x + 1, y + 1, z).setType(Material.AIR);
            mapWorld.getBlockAt(x - 1, y + 1, z).setType(Material.AIR);
            mapWorld.getBlockAt(x, y + 2, z + 1).setType(Material.AIR);
            mapWorld.getBlockAt(x, y + 2, z - 1).setType(Material.AIR);
            mapWorld.getBlockAt(x + 1, y + 2, z).setType(Material.AIR);
            mapWorld.getBlockAt(x - 1, y + 2, z).setType(Material.AIR);
            mapWorld.getBlockAt(x, y + 3, z + 1).setType(Material.AIR);
            mapWorld.getBlockAt(x, y + 3, z - 1).setType(Material.AIR);
            mapWorld.getBlockAt(x + 1, y + 3, z).setType(Material.AIR);
            mapWorld.getBlockAt(x - 1, y + 3, z).setType(Material.AIR);
            mapWorld.getBlockAt(x, y + 4, z).setType(Material.AIR);
    	}
    }
	
    
    
	/*Bungeemode Methods*/
	
	public void sendBungeeUpdate() {
		if (SkyWarsReloaded.getCfg().bungeeMode()) {
			String playerCount = "" + this.getAlivePlayers().size();
			String maxPlayers = "" + this.getMaxPlayers();
			String gameStarted = "" + this.matchState.toString();
			ArrayList<String> messages = new ArrayList<String>();
			messages.add("ServerUpdate");
			messages.add(SkyWarsReloaded.get().getServerName());
			messages.add(playerCount);
			messages.add(maxPlayers);
			messages.add(gameStarted);
			Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
			if (player != null) {
				SkyWarsReloaded.get().sendSWRMessage(player, SkyWarsReloaded.getCfg().getBungeeLobby(), messages);
			}
		}
	}
	
	
	/*Mapdata Methods*/
	
	public static Map<String, MapData> getMapData() {
		return mapData;
	}
	
	public static void loadMapData() {
		mapData.clear();
        File mapFile = new File(SkyWarsReloaded.get().getDataFolder(), "maps.yml");

        if (!mapFile.exists()) {
        	SkyWarsReloaded.get().saveResource("maps.yml", false);
        }
        
        if (mapFile.exists()) {
            FileConfiguration storage = YamlConfiguration.loadConfiguration(mapFile);

            if (storage.getConfigurationSection("maps") != null) {
                for (String key: storage.getConfigurationSection("maps").getKeys(false)) {
                	String name = key;
                	String displayname = storage.getString("maps." + key + ".displayname");
                	int minplayers = storage.getInt("maps." + key + ".minplayers");
                	String creator = storage.getString("maps." + key + ".creator");
                	List<String> signs = storage.getStringList("maps." + key + ".signs");
                	boolean registered = storage.getBoolean("maps." + key + ".registered");
                	mapData.put(key, new MapData(name, displayname, minplayers, creator, signs, registered));
                }
            }
        }
        
        for (GameMap gMap: arenas) {
            gMap.setMapData(mapData.get(gMap.getName()));
        }
	}
	
	private void setMapData(MapData mpdata) {
		this.mp = mpdata;
		
		if (mp == null) {
        	mapData.put(name, new MapData(name, name, ((int) playerCards.size()/2) >= 2 ? (int) playerCards.size()/2 : 2 , "", null, false));
        	saveMapData();
        	mp = mapData.get(name);
        }
        
        this.minPlayers = mp.getMinPlayers() >= 2 ? mp.getMinPlayers() : 2;
        this.displayName = mp.getDisplayName();
        this.designedBy = mp.getCreator();
        this.registered = mp.isRegistered();
        
        if (mp.getSigns() != null) {
        	for (String sign: mp.getSigns()) {
        		signs.add(new SWRSign(this.getName(), Util.get().stringToLocation(sign)));
        	}
        }
	}
	
	private static void saveMapData() {
		try {
			 File mapFile = new File(SkyWarsReloaded.get().getDataFolder(), "maps.yml");

			 if (!mapFile.exists()) {
				 SkyWarsReloaded.get().saveResource("maps.yml", false);
		     }
		        
		     if (mapFile.exists()) {
		         FileConfiguration storage = YamlConfiguration.loadConfiguration(mapFile);
		         storage.set("maps", null);
		         for (GameMap gMap: GameMap.getMaps()) {
		        	 storage.set("maps." + gMap.getName() + ".displayname", gMap.getDisplayName());
		        	 storage.set("maps." + gMap.getName() + ".minplayers", gMap.getMinPlayers());
		        	 storage.set("maps." + gMap.getName() + ".creator", gMap.getDesigner());
		        	 storage.set("maps." + gMap.getName() + ".registered", gMap.isRegistered());
		        	 List<String> stringSigns = new ArrayList<String>();
		        	 for (SWRSign s: gMap.getSigns()) {
		        		 stringSigns.add(Util.get().locationToString(s.getLocation()));
		        	 }
		        	 storage.set("maps." + gMap.getName() + ".signs", stringSigns);
		         }
		         storage.save(mapFile);
		     }
        } catch (IOException ioException) {
            System.out.println("Failed to save mapData");
        }
	}
	
	
	
    /*Sign Methods*/
	
	public void updateSigns() {
		for (SWRSign s : signs) {
			s.update();
		}
	}
	
	public List<SWRSign> getSigns() {
		return this.signs;
	}
	

	public boolean hasSign(Location loc) {
		for (SWRSign s: signs) {
			if (s.getLocation().equals(loc)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean removeSign(Location loc) {
		SWRSign sign = null;
		for (SWRSign s: signs) {
			if (s.getLocation().equals(loc)) {
				sign = s;
			}
		}
		if (sign != null) {
			signs.remove(sign);
			saveMapData();
			updateSigns();
			return true;
		}
		return false;
	}
	
	public void addSign(Location loc) {
		signs.add(new SWRSign(name, loc));
		saveMapData();
		updateSigns();
	}
	
	
	
	/*Getter and Setter Methods*/
        
    public String getDisplayName() {
    	return this.displayName;
    }
    
    public String getDesigner() {
    	return this.designedBy;
    }
    
    public int getMapCount() {
    	return mapUseCount;
    }
    
    public Map<Integer, EmptyChest> getChests(){
		return chests;
	}
	
	public Map<Integer, EmptyChest> getDoubleChests(){
		return doubleChests;
	}
	
	public boolean containsSpawns() {
		if (playerCards.size() >= 2) {
			return true;
		}
		return false;
	}
    
    public MatchState getMatchState() {
        return this.matchState;
    }
    
    public void setMatchState(final MatchState state) {
        this.matchState = state;
    }
    
    public int getPlayerCount() {
    	int count = 0;
		for (PlayerCard pCard: playerCards.values()) {
			if (pCard.getPreElo() != -1) {
				count++;
			}
		}
		return count;
    }
    
    public int getMinPlayers() {
    	if (minPlayers == 0) {
    		return playerCards.size();
    	}
    	return minPlayers;
    }
    
    public void setMinPlayers(int x) {
    	minPlayers = x;
    	saveMapData();
    }
        
    public int getTimer() {
        return this.timer;
    }
    
    public void setTimer(final int lenght) {
        this.timer = lenght;
    }
    
    public GameKit getKit() {
    	return kit;
    }
          
    public String getName() {
        return this.name;
    }
       
	public static void shuffle() {
		Collections.shuffle(arenas);
	}
	
	public void setAllowFallDamage(boolean b) {
		allowFallDamage = b;
	}
   
	public boolean allowFallDamage() {
		return allowFallDamage;
	}

	public ArrayList<UUID> getDead() {
		return dead;
	}

	public ArrayList<UUID> getSpectators() {
		return spectators;
	}
	
	public boolean isThunder() {
		return thunder;
	}

	public void setNextStrike(int randomNum) {
		nextStrike = randomNum;
	}
	
	public int getNextStrike() {
		return nextStrike;
	}
	
	public void setStrikeCounter(int num) {
		strikeCounter = num;
	}
	
	public int getStrikeCounter() {
		return strikeCounter;
	}
	
	public int getMaxPlayers() {
		return playerCards.size();
	}
	
	public boolean isMatchStarted() {
		if (this.matchState == MatchState.WAITINGSTART) {
			return false;
		}
		return true;
	}
	
	public void setThunderStorm(boolean b) {
		this.thunder = b;
	}
	

	public HashMap<Integer, PlayerCard> getPlayerCards() {
		return playerCards;
	}
	
	public PlayerCard getPlayerCard(Player player) {
		for (PlayerCard pCard: playerCards.values()) {
			if (pCard.getPlayer().equals(player)) {
				return pCard;
			}
		}
		return null;
	}
	
	public void setForceStart(boolean state) {
		forceStart = true;
	}
	
	public boolean getForceStart() {
		return forceStart;
	}

	public static GameMap getMapByDisplayName(String name) {
		for (GameMap gMap: arenas) {
			if (ChatColor.stripColor((ChatColor.translateAlternateColorCodes('&', gMap.getDisplayName()))).equalsIgnoreCase(name)) {
				return gMap;
			}
		}
		return null;
	}

	public int getDisplayTimer() {
		return displayTimer;
	}

	public void setDisplayTimer(int displayTimer) {
		this.displayTimer = displayTimer;
	}

	public void setAllowRegen(boolean b) {
		allowRegen = b;
	}

	public boolean allowRegen() {
		return allowRegen;
	}
	
	public void setWinner(String name) {
		winner = name;
	}
	
	public boolean isRegistered() {
		return registered;
	}

	public void setRegistered(boolean b) {
		registered = b;
		saveMapData();
	}

	public void setCreator(String creator) {
		this.designedBy = creator;
		saveMapData();
	}

	public void setDisplayName(String displayName2) {
		this.displayName = displayName2;
		saveMapData();
	}

	public static Inventory getArenasManager() {
		return arenasManager;
	}

	public static void editMap(GameMap gMap, Player player) {
		gMap.unregister();
		String worldName = gMap.getName();
		boolean alreadyLoaded = false;
		for (World world: SkyWarsReloaded.get().getServer().getWorlds()) {
			if (world.getName().equalsIgnoreCase(worldName)) {
				alreadyLoaded = true;
				World editWorld = SkyWarsReloaded.get().getServer().getWorld(worldName);
				player.teleport(new Location(editWorld, 0, 95, 0), TeleportCause.PLUGIN);
				player.setGameMode(GameMode.CREATIVE);
				player.setAllowFlight(true);
				player.setFlying(true);
			}
		} 
		
		if (!alreadyLoaded) {
			File dataDirectory = new File(SkyWarsReloaded.get().getDataFolder(), "maps");
			File source = new File (dataDirectory, worldName);
			File target = new File (SkyWarsReloaded.get().getServer().getWorldContainer().getAbsolutePath(), worldName);
			boolean mapExists = false;
			if(target.isDirectory()) {			 
				if(target.list().length > 0) {
		 			mapExists = true;
				}	 
			}
			if (mapExists) {
				SkyWarsReloaded.getWM().deleteWorld(worldName);
			}
			SkyWarsReloaded.getWM().copyWorld(source, target);
			boolean loaded = SkyWarsReloaded.getWM().loadWorld(worldName);
			if (loaded) {
				SkyWarsReloaded.get().getServer().getScheduler().scheduleSyncDelayedTask(SkyWarsReloaded.get(), new Runnable() {
					public void run() {
						World editWorld = SkyWarsReloaded.get().getServer().getWorld(worldName);
						player.teleport(new Location(editWorld, 0, 95, 0), TeleportCause.PLUGIN);
						player.setGameMode(GameMode.CREATIVE);
						player.setAllowFlight(true);
						player.setFlying(true);
					}
				}, 20);
			} else {
				player.sendMessage(new Messaging.MessageFormatter().format("error.map-fail-load"));
			}
		}
	}

}
