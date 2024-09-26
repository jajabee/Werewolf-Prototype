package com.werewolf.game.api;

import com.werewolf.game.models.Game;
import com.werewolf.game.models.Lobby;
import com.werewolf.game.models.LobbyPlayers;
import com.werewolf.game.models.Player;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class LobbyController {

    public static final HashMap<String, Lobby> lobbies = new HashMap<>();

    // enum for user state
    public enum UserState {
        NOT_AUTHENTICATED,
        NOT_IN_LOBBY,
        IN_LOBBY,
        IN_GAME
    }

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/getUserState")
    public UserState getUserState(HttpSession session) {
        Player player = (Player) session.getAttribute("player");
        if (player == null) {
            return UserState.NOT_AUTHENTICATED;
        }

        if (player.getLobby() == null) {
            return UserState.NOT_IN_LOBBY;
        }

        if(!player.getLobby().isGameStarted()) {
            return UserState.IN_LOBBY;
        }

        return UserState.IN_GAME;
    }

    @PostMapping("/createLobby")
    public Boolean createLobby(@RequestParam String lobbyName) {
        // first we check that the lobby doesn't already exist
        if (lobbies.containsKey(lobbyName)) {
            return false;
        }

        Lobby lobby = new Lobby(lobbyName);
        lobbies.put(lobbyName, lobby);
        messagingTemplate.convertAndSend("/topic/lobbies", "Lobby created: " + lobbyName);
        return true;
    }

    @GetMapping("/getLobbies")
    public List<String> getLobbies() {
        return List.copyOf(lobbies.keySet());
    }

    @GetMapping("/deleteLobby")
    public Boolean deleteLobby(@RequestParam String lobbyName) {
        // First check that the lobby exists
        if (!lobbies.containsKey(lobbyName)) {
            throw new RuntimeException("Lobby does not exist");
        }

        lobbies.remove(lobbyName);
        messagingTemplate.convertAndSend("/topic/lobbies", "Lobby deleted: " + lobbyName);
        return true;
    }

    @PostMapping("/joinLobby")
    public Boolean joinLobby(@RequestParam String lobbyName, @RequestParam String username, HttpSession session) {

        // First check that the lobby exists
        Lobby lobby = lobbies.get(lobbyName);
        if (lobby == null) {
            throw new RuntimeException("Lobby does not exist");
        }

        // Check that a player is not already in the lobby with the same username
        if (lobby.hasPlayerWithUsername(username)) {
            return false;
        }

        Player existingPlayer = (Player) session.getAttribute("player");
        if (existingPlayer != null && existingPlayer.getLobby().equals(lobby)) {
            return false;
        }

        // Create a new player object
        Player player = new Player();
        player.setUsername(username);

        // Add the player to the lobby
        lobby.getPlayers().add(player);
        player.setLobby(lobby);
        
        // Set player in session
        session.setAttribute("player", player);

        // Notify WebSocket clients in the lobby
        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyName, "Player joined: " + username);

        return true;
    }
    
    @PostMapping("/leaveLobby")
    public ResponseEntity<?> leaveLobby(HttpSession session) {
        Player player = (Player) session.getAttribute("player");
        if (player == null) {
            throw new RuntimeException("You are not authenticated");
        }

        Lobby lobby = player.getLobby();
        lobby.getPlayers().remove(player);
        player.setLobby(null);

        session.removeAttribute("player");

        // Notify WebSocket clients in the lobby
        messagingTemplate.convertAndSend("/topic/lobby/" + lobby.getName(), "Player left: " + player.getUsername());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/getCurrentLobbyName")
    public String getCurrentLobbyName(HttpSession session) {
        Player player = (Player) session.getAttribute("player");
        if (player == null) {
            throw new RuntimeException("You are not authenticated");
        }

        Lobby lobby = player.getLobby();
        if (lobby == null) {
            throw new RuntimeException("You are not in a lobby");
        }

        return lobby.getName();
    }

    @GetMapping("/getSelf")
    public Player getSelf(HttpSession session) {
        return (Player) session.getAttribute("player");
    }

    @GetMapping("/getPlayers")
    public LobbyPlayers getPlayers(HttpSession session) {
        // get the player from the session
        Player player = (Player) session.getAttribute("player");
        if (player == null) {
            return null;
        }

        LobbyPlayers lobbyPlayers = new LobbyPlayers();
        lobbyPlayers.setSelf(player);
        lobbyPlayers.setPlayers(player.getLobby().getPlayers());

        return lobbyPlayers;
    }

    @PostMapping("/updateReadyState")
    public ResponseEntity<?> updateReadyState(HttpSession session, @RequestParam boolean isReady) {

        // get the player from the session
        Player player = (Player) session.getAttribute("player");
        if (player == null) {
            return ResponseEntity.badRequest().body("You are not authenticated");
        }

        // check that player is in a lobby
        Lobby lobby = player.getLobby();
        if (lobby == null) {
            return ResponseEntity.badRequest().body("You are not in a lobby");
        }

        player.setReady(isReady);

        // Notify WebSocket clients in the lobby
        messagingTemplate.convertAndSend("/topic/lobby/" + lobby.getName(), "Player ready state updated: " + player.getUsername() + " " + isReady);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/startGame")
    public ResponseEntity<?> startGame(HttpSession session) {
        // get the player from the session
        Player player = (Player) session.getAttribute("player");
        if (player == null) {
            return ResponseEntity.badRequest().body("You are not authenticated");
        }

        // check that player is in a lobby
        Lobby lobby = player.getLobby();
        if (lobby == null) {
            return ResponseEntity.badRequest().body("You are not in a lobby");
        }

        lobbies.remove(lobby.getName());

        lobby.startGame(messagingTemplate);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/setGameAction")
    public ResponseEntity<?> setGameAction(HttpSession session, @RequestParam String action) {
        // get the player from the session
        Player player = (Player) session.getAttribute("player");
        if (player == null) {
            return ResponseEntity.badRequest().body("You are not authenticated");
        }

        // check that player is in a lobby
        Lobby lobby = player.getLobby();
        if (lobby == null) {
            return ResponseEntity.badRequest().body("You are not in a lobby");
        }

        player.setCurrentAction(Player.Action.valueOf(action));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/copyPlayersRole")
    public ResponseEntity<?> copyPlayersRole(HttpSession session, @RequestParam String username) {
        // get the player from the session
        Player player = (Player) session.getAttribute("player");
        if (player == null) {
            return ResponseEntity.badRequest().body("You are not authenticated");
        }

        // check that player is in a lobby
        Lobby lobby = player.getLobby();
        if (lobby == null) {
            return ResponseEntity.badRequest().body("You are not in a lobby");
        }

        // check that the player is allowed to copy
        if (player.getRole() != Game.Role.Doppelganger) {
            return ResponseEntity.badRequest().body("You are not allowed to copy");
        }

        // find the player to copy
        Player playerToCopy = lobby.getPlayers().stream().filter(p -> p.getUsername().equals(username)).findFirst().orElse(null);
        if (playerToCopy == null) {
            return ResponseEntity.badRequest().body("Player not found");
        }

        player.setRole(playerToCopy.getRole());
        player.setCurrentAction(Player.Action.Waiting);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/doWerewolfVote")
    public ResponseEntity<?> doWerewolfKill(HttpSession session, @RequestParam String username) {
        // get the player from the session
        Player player = (Player) session.getAttribute("player");
        if (player == null) {
            return ResponseEntity.badRequest().body("You are not authenticated");
        }

        // check that player is in a lobby
        Lobby lobby = player.getLobby();
        if (lobby == null) {
            return ResponseEntity.badRequest().body("You are not in a lobby");
        }

        // check that the player is a werewolf
        if (player.getRole() != Game.Role.Werewolf) {
            return ResponseEntity.badRequest().body("You are not a werewolf");
        }

        // check that the player is allowed to kill
        if (player.getCurrentAction() != Player.Action.Werewolf) {
            return ResponseEntity.badRequest().body("You are not allowed to kill");
        }


        // find the player to kill
        Player playerToKill = lobby.getPlayers().stream().filter(p -> p.getUsername().equals(username)).findFirst().orElse(null);
        if (playerToKill == null) {
            return ResponseEntity.badRequest().body("Player not found");
        }

        playerToKill.setWerewolfVotes(playerToKill.getWerewolfVotes() + 1);
        player.setCurrentAction(Player.Action.Waiting);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/doHunterKill")
    public ResponseEntity<?> doHunterKill(HttpSession session, @RequestParam String username) {
        // get the player from the session
        Player player = (Player) session.getAttribute("player");
        if (player == null) {
            return ResponseEntity.badRequest().body("You are not authenticated");
        }

        // check that player is in a lobby
        Lobby lobby = player.getLobby();
        if (lobby == null) {
            return ResponseEntity.badRequest().body("You are not in a lobby");
        }

        // check that the player is a hunter
        if (player.getRole() != Game.Role.Hunter) {
            return ResponseEntity.badRequest().body("You are not a hunter");
        }

        // check that the player is allowed to kill
        if (player.getCurrentAction() != Player.Action.Hunter) {
            return ResponseEntity.badRequest().body("You are not allowed to kill");
        }

        // find the player to kill
        Player playerToKill = lobby.getPlayers().stream().filter(p -> p.getUsername().equals(username)).findFirst().orElse(null);
        if (playerToKill == null) {
            return ResponseEntity.badRequest().body("Player not found");
        }

        // check that the player to kill is not already dead
        if (!playerToKill.isAlive()) {
            return ResponseEntity.badRequest().body("You cannot kill this player");
        }

        playerToKill.setAlive(false);
        player.setCurrentAction(Player.Action.Waiting);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/doHunterKillDay")
    public ResponseEntity<?> doHunterKillDay(HttpSession session, @RequestParam String username) {
        // get the player from the session
        Player player = (Player) session.getAttribute("player");
        if (player == null) {
            return ResponseEntity.badRequest().body("You are not authenticated");
        }

        // check that player is in a lobby
        Lobby lobby = player.getLobby();
        if (lobby == null) {
            return ResponseEntity.badRequest().body("You are not in a lobby");
        }

        // check that the player is a hunter
        if (player.getRole() != Game.Role.Hunter) {
            return ResponseEntity.badRequest().body("You are not a hunter");
        }

        // check that the player is allowed to kill
        if (player.getCurrentAction() != Player.Action.HunterDay) {
            return ResponseEntity.badRequest().body("You are not allowed to kill");
        }

        // find the player to kill
        Player playerToKill = lobby.getPlayers().stream().filter(p -> p.getUsername().equals(username)).findFirst().orElse(null);
        if (playerToKill == null) {
            return ResponseEntity.badRequest().body("Player not found");
        }

        // check that the player to kill is not already dead
        if (!playerToKill.isAlive()) {
            return ResponseEntity.badRequest().body("You cannot kill this player");
        }

        playerToKill.setAlive(false);
        playerToKill.setKilledByHunterDuringDay(true);
        player.setCurrentAction(Player.Action.HunterKilled);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/doVillagerVote")
    public ResponseEntity<?> doVillagerVote(HttpSession session, @RequestParam String username) {
        // get the player from the session
        Player player = (Player) session.getAttribute("player");
        if (player == null) {
            return ResponseEntity.badRequest().body("You are not authenticated");
        }

        // check that player is in a lobby
        Lobby lobby = player.getLobby();
        if (lobby == null) {
            return ResponseEntity.badRequest().body("You are not in a lobby");
        }

        // check that the player is allowed to vote
        if (player.getCurrentAction() != Player.Action.Villager || !player.isAlive()) {
            return ResponseEntity.badRequest().body("You are not allowed to vote");
        }

        // find the player to vote for
        Player playerToVoteFor = lobby.getPlayers().stream().filter(p -> p.getUsername().equals(username)).findFirst().orElse(null);
        if (playerToVoteFor == null) {
            return ResponseEntity.badRequest().body("Player not found");
        }

        playerToVoteFor.setVillagerVotes(playerToVoteFor.getVillagerVotes() + 1);
        player.setCurrentAction(Player.Action.Voted);

        return ResponseEntity.ok().build();
    }

}