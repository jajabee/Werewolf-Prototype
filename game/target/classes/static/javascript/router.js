async function routeToPage(page) {
    if (window.location.pathname !== page) {
        window.location.href = page;
        // wait to make sure that no other js code is executed before the page is redirected
        await new Promise(r => setTimeout(r, 10000));
    }
}

async function routeToCorrectPage() {
    let response = await fetch('/getUserState');
    let state = await response.json();
    console.log(state);
    if (state === "NOT_AUTHENTICATED" || state === "NOT_IN_LOBBY") {
        await routeToPage("/lobbies.html");
    } else if (state === "IN_LOBBY") {
        await routeToPage("/lobby.html");
    } else if (state === "IN_GAME") {
        await routeToPage("/game.html");
    }
}