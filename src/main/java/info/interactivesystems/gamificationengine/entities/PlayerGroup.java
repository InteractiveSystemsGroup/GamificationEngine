package info.interactivesystems.gamificationengine.entities;

import info.interactivesystems.gamificationengine.api.GoalApi;
import info.interactivesystems.gamificationengine.api.ValidateUtils;
import info.interactivesystems.gamificationengine.dao.PlayerDAO;
import info.interactivesystems.gamificationengine.entities.goal.FinishedGoal;
import info.interactivesystems.gamificationengine.entities.goal.Goal;
import info.interactivesystems.gamificationengine.entities.rewards.Achievement;
import info.interactivesystems.gamificationengine.entities.rewards.Badge;
import info.interactivesystems.gamificationengine.entities.rewards.PermanentReward;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Players can be assigned to a group by its creation or at a later point in time. 
 * For example depending on the respective organization, a group can be a 
 * department, a work group or several employees with the same occupation. It is 
 * possible to create special tasks which can be done only as a group. 
 * When a member of a group completed such a task the group obtains its rewards. 
 * So a group can also have a list of already earned rewards and finished Goals. 
 * Like a player, a group can be assigned an image as a logo.
 */
@Entity
@JsonIgnoreProperties({ "belongsTo", "groupLogo" })
public class PlayerGroup {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoalApi.class);
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@NotNull
	private String name;

	@NotNull
	@ManyToOne
	private Organisation belongsTo;

	@ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
	private List<Player> players;

	@OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, fetch = FetchType.EAGER)
	private List<FinishedGoal> finishedGoals;

	@ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
	private List<PermanentReward> rewards;

	private int coins;
	private int points;

	private int levelIndex;
	private String levelLabel;

	@Lob
	@Column(columnDefinition = "MEDIUMBLOB", length = 3000000)
	private byte[] groupLogo;

	
	public PlayerGroup() {
		players = new ArrayList<>();
		finishedGoals = new ArrayList<>();
	}
	

	/**
	 * Gets the name of the group.
	 * 
	 * @return a name of the group as String.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name field to label a group.
	 * 
	 * @param name
	 *           The name of the group. May not be null.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the current list of players who are belong to this group.
	 * 
	 * @return a list of all players.
	 */
	public List<Player> getPlayers() {
		return players;
	}

	/**
	 * Sets the list of players who belong to this group.
	 * 
	 * @param players
	 *            The new list of players who are assigned to this group.
	 */
	public void setPlayers(List<Player> players) {
		this.players = players;
	}

	/**
	 * Gets all Goals a group has already completed.
	 * 
	 * @return The list of already finished goals.
	 */
	public List<FinishedGoal> getFinishedGoals() {
		return finishedGoals;
	}

	/**
	 * Sets the list of Goals a group has already finished.
	 * 
	 * @param finishedGoals
	 *            goals a group has finished
	 */
	public void setFinishedGoals(List<FinishedGoal> finishedGoals) {
		this.finishedGoals = finishedGoals;
	}

	/**
	 * Gets the current amount of points a group owns.
	 * 
	 * @return Current amount of points this group owns as int.
	 */
	public int getPoints() {
		return points;
	}

	/**
	 * Set the current amount of points a group has collected.
	 * 
	 * @param points
	 *            The new amount of points this group owns.
	 */
	public void setPoints(int points) {
		this.points = points;
	}

	/**
	 * The organisation the group belongs to. This parameter must 
	 * not be null
	 * 
	 * @return The organisations object the group belongs to.
	 */
	public Organisation getBelongsTo() {
		return belongsTo;
	}

	/**
	 * Sets the organisation to which this group belongs. The parameter
	 * must not be null. 
	 * 
	 * @param belongsTo
	 *            The organisation to which the group belongs to henceforth.  
	 */
	public void setBelongsTo(Organisation belongsTo) {
		this.belongsTo = belongsTo;
	}

	/**
	 * Gets the id of the group.
	 * 
	 * @return The group's id as int.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the id of the group.
	 * 
	 * @param id
	 *            The id of the group henceforth.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Gets the logo of a group as a byte[].
	 * 
	 * @return Byte[] of the group's image's content.
	 */
	public byte[] getGroupLogo() {
		return groupLogo;
	}

	/**
	 * Sets the new logo of a group.
	 * 
	 * @param groupLogo
	 *            Byte[] of the image's content.
	 */
	public void setGroupLogo(byte[] groupLogo) {
		this.groupLogo = groupLogo;
	}

	/**
	 * This method checks if a group belongs to a specific organisation. Therefore
	 * it is tested if the organisation's API key matches the group's API key. 
	 * 
	 * @param organisation
	 * 			The organisation object a group may belong to.
	 * @return Boolean value if the API key of the group is the same 
	 * 			of the tested organisation (true) or not (false).
	 */
	public boolean belongsTo(Organisation organisation) {
		return getBelongsTo().getApiKey().equals(organisation.getApiKey());
	}

	/**
	 * Adds a permanent reward like a Badge or Achievement to the list of all
	 * already obtained rewards.
	 * 
	 * @param reward
	 *            The permanent reward object that was just obtained and should 
	 *            be added to the list.
	 */
	public void addPermanentReward(PermanentReward reward) {
		rewards.add(reward);
	}

	/**
	 * Raises the current amount of coins a group owns by adding the amount 
	 * of earned coins. 
	 *           
	 * @param amount
	 *  		The amount of coins that should be added passed as int.
	 */
	public void awardCoins(int amount) {
		setCoins(getCoins() + amount);
	}

	/**
	 * Raises the current amount of points a group owns by adding the amount 
	 * of earned coins. 
	 *           
	 * @param amount
	 *            The amount of coins that should be added passed as int.
	 */
	public void awardPoints(int amount) {
		this.points = this.points + amount;
	}

	/**
	 * This method tests if a goal was already finished and if so it returns
	 * all finished goal objects of the same type that are completed by this 
	 * group.
	 * 
	 * @param goal
	 *          The goal object that should be compared.
	 * @return A list of all finished goals of the same type of the passed goal.
	 */
	public List<FinishedGoal> getFinishedGoalsByGoal(Goal goal) {
		List<FinishedGoal> returnList = new ArrayList<>();
		for (FinishedGoal fGoal : finishedGoals) {
			if (fGoal.getGoal().equals(goal)) {
				returnList.add(fGoal);
			}
		}
		return returnList;
	}

	/**
	 * Gets the current amount of coins a group of players has obtained.
	 * 
	 * @return The amount of obtained coins as int.
	 */
	public int getCoins() {
		return coins;
	}

	/**
	 * Sets the current amount of coins a group of players has obtained.
	 * 
	 * @param coins
	 *            The amount of current coins a group owns has.
	 */
	public void setCoins(int coins) {
		this.coins = coins;
	}

	/**
	 * Gets all permanent rewards a group of player has already obtained. These 
	 * are for example all badges and achievements.
	 * 
	 * @return List of all obtained permanent rewards.
	 */
	public List<PermanentReward> getRewards() {
		return rewards;
	}

	/**
	 * Sets the list of all permanent rewards a group of players has obtained.
	 * 
	 * @param rewards
	 *            All permanent rewards a group of players has obtained.
	 */
	public void setRewards(List<PermanentReward> rewards) {
		this.rewards = rewards;
	}
	
	/**
	 * Gets only all Badges a group of players has already obtained.
	 * If the group has no Badge, null is returned. 
	 * 
	 * @return A List of all obtained Badges as List.
	 */
	public List<Badge> getOnlyBadges() {
		// filter PermanentRewards for Badges
		if (rewards != null) {
			List<Badge> badges = rewards.stream().filter(r -> r instanceof Badge).map(r -> (Badge) r).collect(Collectors.toList());
			return badges;
		} else {
			return null;
		}
	}

	/**
	 * Gets only all Achievements a group of players has already obtained.
	 * If the group has no Achievement, null is returned. 
	 * 
	 * @return A List of all obtained Achievements as List.
	 */
	public List<Achievement> getOnlyAchievement() {
		// filter PermanantAchievements for Achievements
		if (rewards != null) {
			List<Achievement> achievements = rewards.stream().filter(r -> r instanceof Achievement).map(r -> (Achievement) r)
					.collect(Collectors.toList());

			return achievements;
		} else {
			return null;
		}
	}

	
	
	/**
	 * Gets the current level index of a group of players.
	 * 
	 * @return Level index returned as int.
	 */
	public int getLevelIndex() {
		return levelIndex;
	}

	/**
	 * Sets the current level index a group of players has obtained.
	 * 
	 * @param levelIndex
	 *            The index of player's current level.
	 */
	public void setLevelIndex(int levelIndex) {
		this.levelIndex = levelIndex;
	}

	/**
	 * Gets the label of a group's current level.
	 * 
	 * @return The name of the player's current level as String.
	 */
	public String getLevelLabel() {
		return levelLabel;
	}

	/**
	 * Sets the label of a group's current level.
	 * 
	 * @param levelLabel
	 *            The name of the group's current level.
	 */
	public void setLevelLabel(String levelLabel) {
		this.levelLabel = levelLabel;
	}
	
	/**
	 * This method adds one or more players to the group's list of current players, but only if they
	 * are not in this list already.
	 * 
	 * @param newPlayer
	 * 			The new players that should be added to the list of players.
	 */
	public void addPlayers(List<Player> newPlayer){
		for(Player player : newPlayer){
			if(!players.contains(player)){
				players.add(player);
			}
		}
	}
	
	/**
	 * This method removes one or more players of the group's list of current players, but only if they
	 * are in this list already.
	 * 
	 * @param oldPlayer
	 * 			The players that should be removed from the list of players.
	 */
	public void removePlayers(List<Player> oldPlayer){
		for(Player player : oldPlayer){
			if(players.contains(player)){
				players.remove(player);
			}
		}
	}
	
	/**
	 * This method returns a list of players associated with the passed ids
	 * 
	 * @param playerIds 
	 * 			The ids that should be converted to a list of players.
	 * @param playerDao
	 * 			The player DAO is required to access created players. 
	 * @param apiKey
	 * 			The API key of the organisation is needed to access the created players. 
	 * @return A list of Players that are associated with the passed ids.
	 */
	public List<Player> parseIdsToPlayerList(String playerIds,PlayerDAO playerDao, String apiKey){
		// Find all Players by Id
		String[] playerIdList = playerIds.split(",");
		List<Player> players = new ArrayList<>();

		for (String playerIdString : playerIdList) {
			LOGGER.debug("Player To Add: " + playerIdString);
			Player player = playerDao.getPlayer(ValidateUtils.requireGreaterThanZero(playerIdString), apiKey);
			if (player != null) {
				LOGGER.debug("Player added: " + player.getId());
				players.add(player);
			}
		}
		return players;
	}
}
