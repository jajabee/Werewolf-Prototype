package com.werewolf.game.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Lobby {
    String name;
    List<Player> players = new ArrayList<>();
    boolean isGameStarted;
    @JsonIgnore
    Game game;

    public Lobby(String name) {
        this.name = name;
    }

    public boolean hasPlayerWithUsername(String username) {
        for (Player player : players) {
            if (player.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public void startGame(SimpMessagingTemplate messagingTemplate) {
        // check that all players are ready
        for (Player player : players) {
            if (!player.isReady()) {
                throw new IllegalStateException("Not all players are ready");
            }
        }

        isGameStarted = true;
        game = new Game(players, name, messagingTemplate);
    }
}
