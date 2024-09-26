# Werewolf-Prototype
This is a prototype of the cardgame Werewolf and is designed specifically for visual impaired and blind people.  

### Technologies which has been used for the development of the prototype:
#### HTML as Frontend
The webpage is divided into two sections: the top half displays essential game information, and the bottom half is for confirming roles or actions using swipe gestures and double-tap confirmations. 

#### Cookies for Data Storage
Cookies are used to store session data such as the player's username and lobby information, allowing for seamless re-entry without re-authentication.  

#### WebSockets for Real-time Communication
[WebSockets are used to enable bidirectional, real-time communication between the client and server, crucial for immediate updates in gameplay, reducing latency and enhancing interactivity.](https://spring.io/guides/gs/messaging-stomp-websocket)

#### STOMP Protocol
[SSTOMP simplifies message delivery over WebSockets, ensuring efficient event distribution across clients.](https://en.wikipedia.org/wiki/Streaming_Text_Oriented_Messaging_Protocol)

#### Spring Framework
The backend uses Spring and Spring Boot for efficient development and real-time features, including WebSocket and STOMP integration, ensuring synchronization across players.  

#### Text-to-Speech
[The Web Speech API's SpeechSynthesisUtterance interface converts text to speech, aiding blind users in receiving real-time information via audio.](https://developer.mozilla.org/en-US/docs/Web/API/Web_Speech_API)

#### Gesture Controls
Three key gestures are used: [swiping](https://swipe.js.org/) to select players, [double-tapping](https://api.jquery.com/dblclick/) to confirm, and single-tapping to replay text. These gestures minimize physical effort and are managed with Vanilla-JavaScript and jQuery. 

#### Important Note:
This application only runs if the server is started on your own device and the game can only be joined if every player is in the same wifi.
