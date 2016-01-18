package info.interactivesystems.gamificationengine.api;

import info.interactivesystems.gamificationengine.api.exeption.ApiError;
import info.interactivesystems.gamificationengine.api.validation.ValidApiKey;
import info.interactivesystems.gamificationengine.api.validation.ValidListOfDigits;
import info.interactivesystems.gamificationengine.api.validation.ValidPositiveDigit;
import info.interactivesystems.gamificationengine.dao.OrganisationDAO;
import info.interactivesystems.gamificationengine.dao.PlayerDAO;
import info.interactivesystems.gamificationengine.dao.PlayerGroupDAO;
import info.interactivesystems.gamificationengine.entities.Organisation;
import info.interactivesystems.gamificationengine.entities.Player;
import info.interactivesystems.gamificationengine.entities.PlayerGroup;
import info.interactivesystems.gamificationengine.entities.goal.FinishedGoal;
import info.interactivesystems.gamificationengine.entities.rewards.Achievement;
import info.interactivesystems.gamificationengine.entities.rewards.Badge;
import info.interactivesystems.gamificationengine.entities.rewards.PermanentReward;
import info.interactivesystems.gamificationengine.utils.ImageUtils;
import info.interactivesystems.gamificationengine.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webcohesion.enunciate.metadata.rs.TypeHint;

/**
 * Players can be assigned to a group by its creation or at a later point in time.
 * For example depending on the respective organization, a group can be a 
 * department, a work group or several employees with the same occupation. It is
 * possible to create special tasks which can be done only as a group. 
 * When a member of a group completed such a task the group obtains its rewards. 
 * So a group can also have a list of already earned rewards and finished Goals. 
 * Like a player, a group can be assigned an image as a logo. This can either be 
 * done when creating the group or later through a PUT query. Later players can 
 * also be added to a group or the group’s name can be changed.
 */
@Path("/playerGroup")
@Stateless
@Produces(MediaType.APPLICATION_JSON)
public class PlayerGroupApi {

	private static final Logger log = LoggerFactory.getLogger(GoalApi.class);

	@Inject
	OrganisationDAO organisationDao;
	@Inject
	PlayerGroupDAO groupDao;
	@Inject
	PlayerDAO playerDao;

	/**
	 * Creates a new group of players and so the method generates the PlayerGroup-id.
	 * The organisation's API key is mandatory otherwise a warning with the hint for a 
	 * non valid API key is returned. 
	 * By the creation the player-ids of the players are passed who should be assigned 
	 * to this group. A PlayerGroup can has a name and optional a logo which are query 
	 * parameters. It is checked, if the ids of the players are positive numbers otherwise
	 * a message for the invalid number is returned.
	 * 
	 * @param playerIds
	 *            A list of player-ids can be passed that a group has. These ids are 
	 *            separated by commas. This parameter is required.
	 * @param groupName
	 *            The name of the group. This parameter is required.
	 * @param logoPath
	 *            Optionally a group logo as a HTTP reference can be passed.
	 * @param apiKey
	 *           The valid query parameter API key affiliated to one specific organisation, 
	 *           to which this group of players belongs to.
	 * @return {@link Response} of {@link PlayerGroup} in JSON
	 */
	@POST
	@Path("/")
	@TypeHint(PlayerGroup.class)
	public Response createNewGroup(@QueryParam("playerIds") @NotNull @ValidListOfDigits String playerIds,
			@QueryParam("name") @NotNull String groupName, @QueryParam("logoPath") String logoPath, @QueryParam("apiKey") @ValidApiKey String apiKey) {

		log.debug("created new Group");

		Organisation organisation = organisationDao.getOrganisationByApiKey(apiKey);

		PlayerGroup group = new PlayerGroup();

		group.setName(groupName);

		// Find all Players by Id
		String[] playerIdList = playerIds.split(",");
		List<Player> players = new ArrayList<>();

		for (String playerIdString : playerIdList) {
			log.debug("Player To Add: " + playerIdString);
			Player player = playerDao.getPlayer(ValidateUtils.requireGreaterThenZero(playerIdString), apiKey);
			if (player != null) {
				log.debug("Player added: " + player.getId());
				players.add(player);
			}
		}

		group.setPlayers(players);
		group.setBelongsTo(organisation);
		if (logoPath != null) {
			group.setGroupLogo(ImageUtils.imageToByte(logoPath));
		}

		// persist Group
		groupDao.insertGroup(group);

		return ResponseSurrogate.created(group);
	}

	/**
	 * Returns the group of players associated with the passed id. If the API key is not 
	 * valid an analogous message is returned. It is also checked, if the player id is a 
	 * positive number otherwise a message for an invalid number is returned.
	 *
	 * @param id
	 *           Required path parameter as integer which uniquely identify the {@link PlayerGroup}.
	 * @param apiKey
	 *          The valid query parameter API key affiliated to one specific organisation, 
	 *          to which this group of players belongs to.
	 * @return {@link Response} of {@link PlayerGroup} in JSON
	 */
	@GET
	@Path("/{id}")
	@TypeHint(PlayerGroup.class)
	public Response getPlayerGroup(@PathParam("id") @ValidPositiveDigit String id, @QueryParam("apiKey") @ValidApiKey String apiKey) {

		Organisation organisation = organisationDao.getOrganisationByApiKey(apiKey);
		PlayerGroup group = groupDao.getPlayerGroupByIdAndOrganisation(ValidateUtils.requireGreaterThenZero(id), organisation);

		if (group == null) {
			throw new ApiError(Response.Status.NOT_FOUND, "No such PlayerGroup: " + id);
		}

		return ResponseSurrogate.of(group);
	}

	/**
	 * With this method the fields of a PlayerGroup can be changed. For this the id of the 
	 * group, the API key of the specific organisation, the name of the field and the new 
	 * value are needed.
	 * To modify the name the new String has to be passed with the attribute field. For a
	 * new logo the path of new image is needed in the attribute parameter. The format of 
	 * the image has to be .jpg or .png. A new list of players can be passed when their ids
	 * are separated by commas. 
	 * If the API key is not valid an analogous message is returned. It is also checked, if 
	 * the ids are a positive number otherwise a message for an invalid number is returned.
	 * 
	 * @param id
	 *           Required integer which uniquely identify the {@link PlayerGroup}.
	 * @param attribute
	 *           The name of the attribute which should be modified. This parameter is 
	 *           required. The following names of attributes can be used to change the 
	 *           associated field:
	 *           "name", "playerIds" and "logo".
	 * @param value
	 *           The new value of the attribute. This parameter is required.
	 * @param apiKey
	 *           The valid query parameter API key affiliated to one specific organisation, 
	 *           to which this role belongs to.
	 * @return {@link Response} of {@link PlayerGroup} in JSON
	 */
	@PUT
	@Path("/{id}/attributes")
	@TypeHint(PlayerGroup.class)
	public Response changePlayerGroupAttributes(@PathParam("id") @ValidPositiveDigit String id, @QueryParam("attribute") @NotNull String attribute,
			@QueryParam("value") @NotNull String value, @QueryParam("apiKey") @ValidApiKey String apiKey) {

		log.debug("change Attribute of PlayerGroup");

		Organisation organisation = organisationDao.getOrganisationByApiKey(apiKey);
		PlayerGroup plGroup = groupDao.getPlayerGroupByIdAndOrganisation(ValidateUtils.requireGreaterThenZero(id), organisation);

		if ("null".equals(value)) {
			value = null;
		}

		// not changeable: id -> generated & belongsTo;
		switch (attribute) {

		case "name":
			plGroup.setName(value);
			break;

		case "playerIds":
			changePlayerIds(value, plGroup, apiKey);
			break;

		case "logo":
			plGroup.setGroupLogo(ImageUtils.imageToByte(value));
			break;

		}

		groupDao.insertGroup(plGroup);

		return ResponseSurrogate.updated(plGroup);
	}

	/**
	 * This method converts the string of player ids which are transfered to a list of players.
	 * These players are then set as the new list of players of a group. 
	 * 
	 * @param value
	 * 		   	The new values of players as string separated by commas. This parameter is 
	 * 		   	required.
	 * @param plGroup
	 * 		  	The group whose field of players will be modified. This parameter should be not 
	 * 		  	null because this method is called by a method which checks the given id if a 
	 * 			group exists. 
	 * @param apiKey
	 *   	   	The valid query parameter API key affiliated to one specific organisation, 
	 *        	to which this group and the players belong to.
	 */
	private void changePlayerIds(@NotNull String value, PlayerGroup plGroup, String apiKey) {
		String commaSeparatedList = StringUtils.validateAsListOfDigits(value);
		List<Integer> ids = StringUtils.stringArrayToIntegerList(commaSeparatedList);
		List<Player> players = playerDao.getPlayers(ids, apiKey);
		plGroup.setPlayers(players);
	}

	/**
	 * Removes the group with the assigned id from data base. It is checked, if the passed id is a 
	 * positive number otherwise a message for an invalid number is returned. If the API key is not 
	 * valid an analogous message is returned.
	 * 
	 * @param id
	 *           Required path parameter as integer which uniquely identify the {@link PlayerGroup}.
	 * @param apiKey
	 *           The valid query parameter API key affiliated to one specific organisation, 
	 *           to which this group of players belongs to.
	 * @return {@link Response} of {@link PlayerGroup} in JSON.
	 */
	@DELETE
	@Path("/{id}")
	@TypeHint(PlayerGroup.class)
	public Response deletePlayerGroup(@PathParam("id") @ValidPositiveDigit String id, @QueryParam("apiKey") @ValidApiKey String apiKey) {
		if (id == null) {
			throw new ApiError(Response.Status.FORBIDDEN, "no GroupId transferred");
		}

		Organisation organisation = organisationDao.getOrganisationByApiKey(apiKey);
		PlayerGroup plGroup = groupDao.deletePlayerGroupByIdAndOrganisation(ValidateUtils.requireGreaterThenZero(id), organisation);

		if (plGroup == null) {
			throw new ApiError(Response.Status.NOT_FOUND, "No such PlayerGroup: " + plGroup);
		}

		return ResponseSurrogate.deleted(plGroup);
	}

	/**
	 * Returns a list of all already finished goals of a specific group of players. 
	 * If the API key is not valid an analogous message is returned. It is also checked, if the 
	 * group id is a positive number otherwise a message for an invalid number is returned.
	 *
	 * @param id
	 *         Required path parameter as integer which uniquely identify the {@link PlayerGroup}.
	 * @param apiKey
	 *         The valid query parameter API key affiliated to one specific organisation, 
	 *         to which this group belongs to.
	 * @return {@link Response} as {@link List} of {@link FinishedGoal}s in JSON.
	 */
	@GET
	@Path("/{id}/goals")
	@TypeHint(FinishedGoal[].class)
	public Response getPlayerGroupFinishedGoals(@PathParam("id") @NotNull @ValidPositiveDigit String id, @QueryParam("apiKey") @ValidApiKey String apiKey) {

		log.debug("getFinishedGoals of Group requested");
		List<FinishedGoal> fgoals = groupDao.getPlayerGroup(ValidateUtils.requireGreaterThenZero(id), apiKey).getFinishedGoals();
				
		return ResponseSurrogate.of(fgoals);
	}

	
	/**
	 * Returns the avatar which is associated with a group of players. To identify the group its id and 
	 * the API key is needed to which the group belongs to. 
	 * If the API key is not valid an analogous message is returned. It is also checked, if the id 
	 * is a positive number otherwise a message for an invalid number is returned.
	 * 
	 * @param id
	 *          Required path parameter as integer which uniquely identify the {@link PlayerGroup}.
	 * @param apiKey
	 *           The valid query parameter API key affiliated to one specific organisation, 
	 *           to which this group of players belongs to.
	 * @return {@link Response} of {@link Object} with an byte[] in JSON.
	 */
	@GET
	@Path("{id}/avatar")
	@TypeHint(byte[].class)
	public Response getAvatar(@PathParam("id") @ValidPositiveDigit String id, @QueryParam("apiKey") @ValidApiKey String apiKey) {
		log.debug("get groups's avatar image");

		int groupId = ValidateUtils.requireGreaterThenZero(id);
		PlayerGroup group = groupDao.getPlayerGroup(ValidateUtils.requireGreaterThenZero(groupId), apiKey);

		byte[] bytes = group.getGroupLogo();

		return ResponseSurrogate.of(new Object() {
			public byte[] image = bytes;
		});
	}
	
	/**
	 * Returns the current amount of points associated with the group of players of the passed id. If 
	 * the API key is not valid an analogous message is returned. It is also checked, if
	 * the group id is a positive number otherwise a message for an invalid number is 
	 * returned.
	 *
	 * @param id
	 *          Required path parameter as integer which uniquely identify the {@link PlayerGroup}.
	 * @param apiKey
	 *          The valid query parameter API key affiliated to one specific organisation, 
	 *          to which this group of players belongs to.
	 * @return {@link Response} of {@link int} in JSON.
	 */
	@GET
	@Path("/{id}/points")
	@TypeHint(int.class)
	public Response getPlayerGroupPoints(@PathParam("id") @NotNull @ValidPositiveDigit String id, @QueryParam("apiKey") @ValidApiKey String apiKey) {

		log.debug("get earned points from group requested");
		PlayerGroup group = groupDao.getPlayerGroup(ValidateUtils.requireGreaterThenZero(id), apiKey);
		int points = group.getPoints();

		return ResponseSurrogate.of(points);
	}

	/**
	 * Returns the current amount of coins associated with the group of players of the passed id. If 
	 * the API key is not valid an analogous message is returned. It is also checked, if
	 * the group id is a positive number otherwise a message for an invalid number is 
	 * returned.
	 * 
	 * @param id
	 *          Required path parameter as integer which uniquely identify the {@link PlayerGroup}.
	 * @param apiKey
	 *          The valid query parameter API key affiliated to one specific organisation, 
	 *          to which this group of players belongs to.
	 * @return {@link Response} of {@link int} in JSON.
	 */
	@GET
	@Path("/{id}/coins")
	@TypeHint(int.class)
	public Response getPlayerGroupCoins(@PathParam("id") @NotNull @ValidPositiveDigit String id, @QueryParam("apiKey") @ValidApiKey String apiKey) {

		log.debug("get earned coins from group of players requested");
		PlayerGroup group = groupDao.getPlayerGroup(ValidateUtils.requireGreaterThenZero(id), apiKey);
		int coins = group.getCoins();

		return ResponseSurrogate.of(coins);
	}
	
	/**
	 * Returns a list of all already awarded rewards associated with the group of players of the 
	 * given id.
	 * If the API key is not valid an analogous message is returned. It is also checked, if the 
	 * group id is a positive number otherwise a message for an invalid number is returned.
	 *
	 * @param id
	 *          Required path parameter as integer which uniquely identify the {@link PlayerGroup}.
	 * @param apiKey
	 *         The valid query parameter API key affiliated to one specific organisation, 
	 *         to which this group of players belongs to.
	 * @return {@link Response} as {@link List} of {@link PermanentReward}s in JSON.
	 */
	@GET
	@Path("/{id}/rewards")
	@TypeHint(PermanentReward[].class)
	public Response getPlayerGroupRewards(@PathParam("id") @NotNull @ValidPositiveDigit String id, @QueryParam("apiKey") @ValidApiKey String apiKey) {

		log.debug("getPlayerGroupPermanentRewards requested");
		List<PermanentReward> pRewards = groupDao.getPlayerGroup(ValidateUtils.requireGreaterThenZero(id), apiKey).getRewards();

		return ResponseSurrogate.of(pRewards);
	}

	/**
	 * Returns a list of all awarded badges associated with the group of players of the passed 
	 * id. If the API key is not valid an analogous message is returned. It is also checked, 
	 * if the group id is a positive number otherwise a message for an invalid number is 
	 * returned.
	 *
	 * @param id
	 *          Required path parameter as integer which uniquely identify the {@link PlayerGroup}.
	 * @param apiKey
	 *          The valid query parameter API key affiliated to one specific organisation, 
	 *          to which this group belongs to.
	 * @return {@link Response} as {@link List} of {@link Badge}s in JSON.
	 */
	@GET
	@Path("/{id}/badges")
	@TypeHint(Badge[].class)
	public Response getPlayerGroupBadges(@PathParam("id") @NotNull @ValidPositiveDigit String id, @QueryParam("apiKey") @ValidApiKey String apiKey) {

		log.debug("get earned Badges from PlayerGroup requested");
		List<Badge> badges = groupDao.getPlayerGroup(ValidateUtils.requireGreaterThenZero(id), apiKey).getOnlyBadges();
				
		return ResponseSurrogate.of(badges);
	}

	/**
	 * Returns a list of all awarded achievements associated with the group of players of the 
	 * passed id. If the API key is not valid an analogous message is returned. It is also 
	 * checked, if the player id is a positive number otherwise a message for an invalid number 
	 * is returned.
	 *
	 * @param id
	 *          Required path parameter as integer which uniquely identify the {@link PlayerGroup}.
	 * @param apiKey
	 *          The valid query parameter API key affiliated to one specific organisation, 
	 *          to which this group belongs to.
	 * @return {@link Response} as {@link List} of {@link Achievement}s in JSON
	 */
	@GET
	@Path("/{id}/achievements")
	@TypeHint(Achievement[].class)
	public Response getPlayerGroupAchievements(@PathParam("id") @NotNull @ValidPositiveDigit String id, @QueryParam("apiKey") @ValidApiKey String apiKey) {

		log.debug("get earned Achievements from Player requested");
		List<Achievement> achievements = groupDao.getPlayerGroup(ValidateUtils.requireGreaterThenZero(id), apiKey).getOnlyAchievement();

		return ResponseSurrogate.of(achievements);
	}
	
}