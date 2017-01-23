package info.interactivesystems.gamificationengine.entities.goal;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PreRemove;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import info.interactivesystems.gamificationengine.entities.Player;

/**
 * When a player has completed a Goal, it will be added to the player’s list of finished goals. If the goal is
 * a group goal it is also stored in the group's list of finished goals. At the same time the date is also 
 * stored when this request was sent and the goal was officially be done.
 */
@Entity
@JsonIgnoreProperties({ "player" })
public class FinishedGoal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@NotNull
	private LocalDateTime finishedDate;

	@NotNull
	@ManyToOne
	private Goal goal;

	
	@ManyToOne
	private Player player;

	@PreRemove
	private void removeFGoal() {
		if(player !=null){
           player.removeFinishedGoal(this);
		}
    }
	
    
	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	
	
	/**
	 * Gets the id of the finished goal.
	 * 
	 * @return The goal's id as int.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the id of the finished goal.
	 * 
	 * @param id
	 *          The id of the finished goal henceforth.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Gets the date and time a goal was completed. 
	 * 
	 * @return The date and time when a goal was finished as local date time.
	 * 
	 */
	public LocalDateTime getFinishedDate() {
		return finishedDate;
	}

	/**
	 * Sets the date and time a goalw as completed.
	 * 
	 * @param finishedDate
	 * 				The date and time when a goal was completed.
	 */
	public void setFinishedDate(LocalDateTime finishedDate) {
		this.finishedDate = finishedDate;
	}

	/**
	 * Get the goal object which was finished.
	 * 
	 * @return The finished goal as object. 
	 */
	public Goal getGoal() {
		return goal;
	}

	/**
	 * Sets the goal that was finished.
	 * 
	 * @param goal
	 * 			The goal that was finished.
	 */
	public void setGoal(Goal goal) {
		this.goal = goal;
	}

}
