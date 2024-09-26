package com.werewolf.game.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

@Getter
@Setter
public class Game {

    public enum Role {
        Doppelganger,
        Werewolf,
        Seer,
        Hunter,
        Villager
    }

    public enum GameState {
        GAME_START,
        NIGHT,
        DAY,
    }

    private transient Map<Role, List<Player>> players = new HashMap<>();
    private transient GameState gameState = GameState.GAME_START;
    private transient String lobbyName;
    private transient SimpMessagingTemplate messagingTemplate;

    public Game(List<Player> players, String lobbyName, SimpMessagingTemplate messagingTemplate) {

        this.lobbyName = lobbyName;
        this.messagingTemplate = messagingTemplate;
        this.messagingTemplate.convertAndSend("/topic/game/" + lobbyName, "Game started");

        // check that there are enough players
        if (players.size() < 3) {
            throw new IllegalStateException("Not enough players to start the game");
        }

        // check that there are not too many players
        if (players.size() > 10) {
            throw new IllegalStateException("Too many players to start the game");
        }

        List<Role> roles;
        if(players.size() == 3)
            roles = Arrays.asList(Role.Werewolf, Role.Villager, Role.Seer);
        else if(players.size() == 4)
            roles = Arrays.asList(Role.Werewolf, Role.Villager, Role.Seer, Role.Hunter);
        else if(players.size() == 5)
            roles = Arrays.asList(Role.Werewolf, Role.Doppelganger, Role.Villager, Role.Seer, Role.Hunter);
        else if(players.size() == 6)
            roles = Arrays.asList(Role.Werewolf, Role.Doppelganger, Role.Villager, Role.Villager, Role.Seer, Role.Hunter);
        else if(players.size() == 7)
            roles = Arrays.asList(Role.Werewolf, Role.Doppelganger, Role.Villager, Role.Villager, Role.Seer, Role.Hunter, Role.Villager);
        else if(players.size() == 8)
            roles = Arrays.asList(Role.Werewolf, Role.Doppelganger, Role.Villager, Role.Villager, Role.Seer, Role.Hunter, Role.Villager, Role.Villager);
        else if(players.size() == 9)
            roles = Arrays.asList(Role.Werewolf, Role.Werewolf, Role.Doppelganger, Role.Villager, Role.Villager, Role.Seer, Role.Hunter, Role.Villager, Role.Villager);
        else
            throw new IllegalStateException("Too many players to start the game");

        // shuffle the roles
        Collections.shuffle(roles);

        // assign the roles to the players
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setRole(roles.get(i));
            if(!this.players.containsKey(roles.get(i)))
                this.players.put(roles.get(i), new ArrayList<>());
            this.players.get(roles.get(i)).add(players.get(i));
        }

        // start the game in a new thread
        new Thread(this::startGame).start();
    }

    private void startGame() {
        // wait for all players to be in waiting state
        waitUntilAllPlayersHaveAction(Player.Action.Waiting);

        // start the game
        GameState gameState = GameState.NIGHT;
        messagingTemplate.convertAndSend("/topic/game/" + lobbyName, "Night has started");
        //-----------------------------order of players at night------------------------------------------
        //first is doppelganger
        // takes form of another players role

        //werewolf
        // kill one person

        //seer
        // can see a roll of one person

        //people can not check what role they have
        if(players.containsKey(Role.Doppelganger)) {
            players.get(Role.Doppelganger).getFirst().setCurrentAction(Player.Action.Doppelganger);
            messagingTemplate.convertAndSend("/topic/game/" + lobbyName, "Doppelganger is active");
            waitUntilAllPlayersHaveAction(Player.Action.Waiting);
        }
        remapPlayers();

        for (Player player : players.get(Role.Werewolf)) {
            player.setCurrentAction(Player.Action.Werewolf);
            messagingTemplate.convertAndSend("/topic/game/" + lobbyName, "Werewolf is active");
            waitUntilAllPlayersHaveAction(Player.Action.Waiting);
        }
        checkWereWolfVotes();

        if(players.containsKey(Role.Seer)) {
            for (Player player : players.get(Role.Seer)) {
                if(!player.isAlive())
                    continue;
                player.setCurrentAction(Player.Action.Seer);
                messagingTemplate.convertAndSend("/topic/game/" + lobbyName, "Seer is active");
                waitUntilAllPlayersHaveAction(Player.Action.Waiting);
            }
        }

        //after night is over
        //and when hunter is dead
        //he can kill one person of his choice
        if(players.containsKey(Role.Hunter)) {
            Set<Player> huntersThatHaveKilled = new HashSet<>();
            int numberOfHunters = players.get(Role.Hunter).size();
            for (int i = 0; i <= numberOfHunters; i++) {
                for (Player player : players.get(Role.Hunter)) {
                    if(player.isAlive())
                        continue;
                    if(huntersThatHaveKilled.contains(player))
                        continue;
                    player.setCurrentAction(Player.Action.Hunter);
                    huntersThatHaveKilled.add(player);
                    messagingTemplate.convertAndSend("/topic/game/" + lobbyName, "Hunter is active");
                    waitUntilAllPlayersHaveAction(Player.Action.Waiting);
                }
            }
        }

        // check if there is more than one player alive that is not a werewolf
        if (getAllPlayers().stream().filter(Player::isAlive).count() <= 1) {
            gameDone();
            return;
        }

        // check if all remaining players are werewolves
        if (getAllPlayers().stream().filter(Player::isAlive).allMatch(player -> player.getRole() == Role.Werewolf)) {
            gameDone();
            return;
        }

        // check if a werewolf has been killed during the night
        for (Player player : players.get(Role.Werewolf)) {
            if(!player.isAlive()) {
                gameDone();
                return;
            }
        }

        for (Player player : getAllPlayers()) {
            player.setCurrentAction(Player.Action.Villager);
        }
        messagingTemplate.convertAndSend("/topic/game/" + lobbyName, "Village is awake");
        checkVillageVotes();
        if(getPlayerKilledByVillage().getRole() == Role.Hunter) {
            getPlayerKilledByVillage().setCurrentAction(Player.Action.HunterDay);
            for(Player player : getAllPlayers()) {
                if(player != getPlayerKilledByVillage())
                    player.setCurrentAction(Player.Action.HunterKilled);
            }
            messagingTemplate.convertAndSend("/topic/game/" + lobbyName, "Hunter is active");
            waitUntilAllPlayersHaveAction(Player.Action.HunterKilled);
        }

        gameDone();
    }

    private Player getPlayerKilledByVillage() {
        for (Player player : getAllPlayers()) {
            if (player.isKilledByVillage())
                return player;
        }
        return null;
    }

    private void checkVillageVotes() {
        while (!getAllPlayers().stream().allMatch(player -> player.getCurrentAction() == Player.Action.Voted || !player.isAlive())) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int mostVotes = 0;
        List<Player> mostVotedPlayers = new ArrayList<>();
        for (Player player : getAllPlayers()) {
            if (player.getVillagerVotes() > mostVotes) {
                mostVotes = player.getVillagerVotes();
                mostVotedPlayers.clear();
                mostVotedPlayers.add(player);
            } else if (player.getVillagerVotes() == mostVotes) {
                mostVotedPlayers.add(player);
            }
        }

        // if there is a tie, randomly select one of the players
        Player killedPlayer;
        if (mostVotedPlayers.size() > 1) {
            killedPlayer = mostVotedPlayers.get(new Random().nextInt(mostVotedPlayers.size()));
        } else {
            killedPlayer = mostVotedPlayers.getFirst();
        }
        killedPlayer.setAlive(false);
        killedPlayer.setKilledByVillage(true);
    }

    private void checkWereWolfVotes() {
        int mostVotes = 0;
        List<Player> mostVotedPlayers = new ArrayList<>();
        for (Player player : getAllPlayers()) {
            if (player.getWerewolfVotes() > mostVotes) {
                mostVotes = player.getWerewolfVotes();
                mostVotedPlayers.clear();
                mostVotedPlayers.add(player);
            } else if (player.getWerewolfVotes() == mostVotes) {
                mostVotedPlayers.add(player);
            }
            player.setWerewolfVotes(0);
        }
        Player killedPlayer;
        if (mostVotedPlayers.size() > 1) {
            killedPlayer = mostVotedPlayers.get(new Random().nextInt(mostVotedPlayers.size()));
        } else {
            killedPlayer = mostVotedPlayers.getFirst();
        }
        killedPlayer.setAlive(false);
    }

    private void gameDone() {
        for (Player player : getAllPlayers()) {
            player.setCurrentAction(Player.Action.GameDone);
        }
        messagingTemplate.convertAndSend("/topic/game/" + lobbyName, "Villagers win");
    }

    private void waitUntilAllPlayersHaveAction(Player.Action action) {
        while (getAllPlayers().stream().anyMatch(player -> player.getCurrentAction() != action)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    private List<Player> getAllPlayers() {
        List<Player> allPlayers = new ArrayList<>();
        for (List<Player> playerList : players.values()) {
            allPlayers.addAll(playerList);
        }
        return allPlayers;
    }

    private void remapPlayers() {
        Map<Role, List<Player>> newPlayers = new HashMap<>();
        for (Role role : players.keySet()) {
            for (Player player : players.get(role)) {
                if(!newPlayers.containsKey(player.getRole()))
                    newPlayers.put(player.getRole(), new ArrayList<>());
                newPlayers.get(player.getRole()).add(player);
            }
        }
        players = newPlayers;
    }
}
