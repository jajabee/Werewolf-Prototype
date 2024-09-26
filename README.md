# Werewolf-Prototype
This is a prototype of the cardgame Werewolf and is designed specifically for visual impaired and blind people.  

### Technologies which has been used for the development of the prototype:
#### HTML as Frontend
HTML forms the foundation of this web-based game, providing a simple UI. Since it's designed for visually impaired players, HTML ensures efficient performance with minimal visuals and easy navigation through swipe and tap gestures.  

#### Cookies for Data Storage
Cookies store session data, such as player status, ensuring players stay logged in and identified without re-authentication.  

#### WebSockets for Real-time Communication
WebSockets enable low-latency, bidirectional communication for real-time interactions in the game, essential for smooth gameplay updates.  

#### STOMP Protocol
STOMP, built on WebSockets, simplifies messaging, ensuring real-time game events are effectively communicated to clients.  

#### Spring Framework
The backend uses Spring and Spring Boot for efficient development and real-time features, including WebSocket and STOMP integration, ensuring synchronization across players.  

#### Text-to-Speech
Using the Web Speech API, the game provides auditory feedback, crucial for blind players to receive game information.  

#### Gesture Controls
Players navigate the game with swipe, double-tap, and single-tap gestures, enabling easy interaction with minimal effort, especially important for blind users.  

#### important note:
This application only runs if the server is started on your own device and the game can only be joined if every player is in the same wifi.
