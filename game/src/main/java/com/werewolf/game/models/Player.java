package com.werewolf.game.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Player {

    public enum Action {
        Confirming,
        Waiting,
        Doppelganger,
        Werewolf,
        Seer,
        Hunter,
        Villager,
        Voted,
        HunterKilled,
        HunterDay,
        GameDone
    }

    private String username;
    @JsonIgnore
    private transient Lobby lobby;
    private transient boolean ready;
    private transient Game.Role role;
    private transient Action currentAction = Action.Confirming;
    private boolean isAlive = true;
    private int villagerVotes = 0;
    private int werewolfVotes = 0;
    private boolean killedByVillage = false;
    private boolean killedByHunterDuringDay = false;
}
