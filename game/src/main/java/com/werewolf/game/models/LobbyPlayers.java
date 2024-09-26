package com.werewolf.game.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LobbyPlayers {
    Player self;
    List<Player> players;
}
