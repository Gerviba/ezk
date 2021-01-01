'use strict';

const GameMenu = {
    LOADING: 'loading',
    LOBBY:   'lobby',
    INTRO:   'intro',
    WIRES:   'wires',
    KEYPAD:  'keypad',
    NOTES:   'notes',
    TEAM:    'team',
    VIEW:    'view',
    PATH:    'path',
    NAV:     'nav'
};

const GateType = {
    WIRES:      'WIRES',
    KEYPAD:     'KEYPAD',
    PATHS:      'PATHS',
    DELAY:      'DELAY'
};

const NoteType = {
    SIMPLE:    'SIMPLE',
    PATH_INFO: 'PATH_INFO'
};

const FeatureType = {
    MODEL:        'MODEL',
    VERSION:      'VERSION',
    CREATED:      'CREATED',
    EDITION:      'EDITION',
    MANUFACTURER: 'MANUFACTURER',
    MADE_IN:      'MADE IN',
    ABORT:        ''
};

const fragments = [
    GameMenu.LOADING, GameMenu.LOBBY, GameMenu.INTRO,
    GameMenu.WIRES, GameMenu.KEYPAD, GameMenu.NOTES,
    GameMenu.TEAM, GameMenu.VIEW, GameMenu.PATH];

const timers = ['time1', 'time2', 'time3'];

const graphicFragments = [GameMenu.WIRES, GameMenu.KEYPAD, GameMenu.NOTES, GameMenu.TEAM, GameMenu.VIEW, GameMenu.PATH];

const GameState = {
    FAILED:        'FAILED',
    LOBBY:         'LOBBY',
    INGAME_INTRO:  'INGAME_INTRO',
    INGAME_DELAY:  'INGAME_DELAY',
    INGAME_TASK:   'INGAME_TASK',
    INGAME_SELECT: 'INGAME_SELECT',
    LOSE:          'LOSE',
    WIN:           'WIN',
    CLOSED:        'CLOSED'
};

const Direction = {
    LEFT:    'LEFT',
    FORWARD: 'FORWARD',
    RIGHT:   'RIGHT'
};

const GameActiveFragments = {
    LOADING: 'loading',
    LOBBY:   'lobby',
    INGAME:  'ingame',
    DELAY:   'delay',
    END:     'end'
};

const hintMapping = {
    alreadyStarted:  'This game is already started',
    invalidPassword: 'Invalid password',
    roomIsFull:      'Selected room is full',
    roomNotFound:    'Room not found'
};

const colorMapping = {
    RED:    'red',
    GREEN:  'green',
    BLUE:   'blue',
    WHITE:  'white',
    GRAY:   'gray',
    BLACK:  'black',
    YELLOW: 'yellow',
    PURPLE: 'purple'
};

const range1to9 = [1, 2, 3, 4, 5, 6, 7, 8, 9];
const range1to3 = [1, 2, 3];

let stompClient;
let state = { state: GameState.LOBBY };
let players = [];
let activeFragment = GameActiveFragments.LOADING;
let targetTime = 0;
let visibleMenu = null;

function connectToRoom() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({username: USERNAME, uniqueId: getUniqueId()}, function (frame) {

        stompClient.subscribe('/user/topic/game-state', function (data) {
            const s = JSON.parse(data.body);
            state = s;
            notifyStateChanged();
        });

        stompClient.subscribe('/user/topic/players', function (data) {
            const p = JSON.parse(data.body);
            players = p;
            notifyPlayersChanged();
        });

        if (SCREEN) {
            setTimeout(function () {
                stompClient.send("/app/spectate", {}, JSON.stringify({
                    roomCode: CODE
                }));
            }, 500);
        } else {
            setTimeout(function () {
                stompClient.send("/app/join", {}, JSON.stringify({
                    roomCode: CODE,
                    password: PASSWORD
                }));
            }, 500);
        }

    });

    startTimers();
}

function startTimers() {
    setInterval(function () {
        const delta = parseInt((targetTime - Date.now()) / 1000);
        if (delta < 0) {
            updateTime(0, 0);
            return;
        }
        updateTime(parseInt(delta / 60), parseInt(delta % 60));
    }, 50);
}

function updateTime(min, sec) {
    const secStr = (sec < 10) ? ("0" + sec) : ("" + sec);
    timers.forEach(id => document.getElementById(id).innerText = `${min}:${secStr}`);
}

function onLoaded() {
    setTimeout(function() { connectToRoom() }, 500);
}

function closeAllUi() {
    fragments.forEach(id => document.getElementById(id).style.display = 'none');
    document.getElementById(GameMenu.NAV).style.display = 'none';
    visibleMenu = null;
}

function closeAllGraphical() {
    graphicFragments.forEach(id => document.getElementById(id).style.display = 'none');
}

function showLobby() {
    closeAllUi();
    document.getElementById('lobbyRoomCode').innerText = state.room.code;
    document.getElementById('lobbyMaxPlayers').innerText = state.room.maxPlayers;
    document.getElementById('lobbyMap').innerText = state.room.map;
    if (activeFragment !== GameActiveFragments.LOBBY)
        document.getElementById('lobbyReadyButton').disabled = false;
    document.getElementById('lobbyStartButton').disabled = !(state.youHost);
    document.getElementById(GameMenu.LOBBY).style.display = 'block';

    if (SCREEN) {
        document.getElementById('lobbyReadyButton').style.display = 'none';
        document.getElementById('lobbyStartButton').style.display = 'none';
    }

    activeFragment = GameActiveFragments.LOBBY;
}

function showFailed(reason) {
    closeAllUi();
    document.getElementById('loadingMessage').innerText = reason;
    document.getElementById('loadingBackToRoom').style.display = 'block';
    document.getElementById(GameMenu.LOADING).style.display = 'block';
    activeFragment = GameActiveFragments.LOADING;
}

function showIngameTask() {
    if (visibleMenu === null) {
        visibleMenu = state.noteDetails.gateDetails.type;
        showNavBar();
        updateNotes();
        targetTime = state.noteDetails.targetTime;

        if (visibleMenu === GateType.WIRES) {
            updateWires();
            openWires();
        } else if (visibleMenu === GateType.KEYPAD) {
            updateKeypad();
            openKeypad();
        }
    } else {
        if (visibleMenu === GateType.WIRES) {
            updateWires();
        } else if (visibleMenu === GateType.KEYPAD) {
            updateKeypad();
        }
    }

}

function showIngame() {
    if (activeFragment !== GameActiveFragments.INGAME)
        closeAllUi();

    updateBackground();

    if (state.state === GameState.INGAME_INTRO) {
        document.getElementById(GameMenu.INTRO).style.display = 'block';
        if (SCREEN)
            document.getElementById('introStart').style.display = 'none';
        updateBackgroundTo('spaceship_intro');
        activeFragment = GameActiveFragments.LOADING;

    } else if (state.state === GameState.INGAME_DELAY) {
        closeAllUi();
        showView();
        activeFragment = GameActiveFragments.DELAY;

    } else if (state.state === GameState.WIN || state.state === GameState.LOSE) {
        closeAllUi();
        showView();

        if (state.state === GameState.WIN) {
            updateBackgroundTo('spaceship_win');
        } else if (state.state === GameState.LOSE
            && state.noteDetails
            && state.noteDetails.gateDetails
            && state.noteDetails.gateDetails.type === GateType.PATHS) {
            updateBackgroundTo('spaceship_path_lose');
        } else if (state.state === GameState.LOSE) {
            updateBackgroundTo('spaceship_gate_failed');
        }

        activeFragment = GameActiveFragments.END;

    } else if (state.state === GameState.INGAME_TASK) {
        showIngameTask();
        activeFragment = GameActiveFragments.INGAME;

    } else if (state.state === GameState.INGAME_SELECT) {
        closeAllUi();
        document.getElementById(GameMenu.PATH).style.display = 'block';
        activeFragment = GameActiveFragments.INGAME;

    }
}

function showView() {
    document.getElementById('viewTitle').innerText = ifNotFoundDefault(state.noteDetails.customTextHeader, "");
    document.getElementById('viewContent').innerText = ifNotFoundDefault(state.noteDetails.customTextContent, "");
    document.getElementById('viewContinue').style.display = (state.noteDetails.nextVisible && !SCREEN) ? 'inline-block' : 'none';
    document.getElementById('viewLeave').style.display = (state.noteDetails.leaveVisible && !SCREEN) ? 'inline-block' : 'none';
    document.getElementById(GameMenu.VIEW).style.display = 'block';
}

function showNavBar() {
    if (!SCREEN)
        document.getElementById(GameMenu.NAV).style.display = 'block';
}

function updateWires() {
    const wires = range1to9.map(i => document.getElementById(`wireWire${i}`));
    const wireHolders = range1to9.map(i => document.getElementById(`wireHolder${i}`));
    const plates = range1to9.map(i => document.getElementById(`wirePlate${i}`));
    const features = range1to3.map(i => document.getElementById(`wireFeature${i}`));
    const featuresCategories = range1to3.map(i => document.getElementById(`wireFeature${i}Category`));
    const featuresValues = range1to3.map(i => document.getElementById(`wireFeature${i}Value`));

    wires.forEach(it => it.style.display = 'none');
    wireHolders.forEach(it => it.style.display = 'none');
    plates.forEach(it => it.style.display = 'none');
    features.forEach(it => it.style.display = 'none');

    state.noteDetails.gateDetails.buttons.forEach(button => {
        wires[button.id].style.display = 'block';
        wireHolders[button.id].style.display = 'table-cell';
        wires[button.id].classList = ['wire', 'wire-' + colorMapping[button.color], button.broken ? 'wire-broken' : 'wire-not-broken'].join(" ");

        plates[button.id].style.display = 'table-cell';
        plates[button.id].text = button.name
    });

    state.noteDetails.gateDetails.features.forEach(feature => {
        features[feature.id].style.display = 'block';
        featuresCategories[feature.id].innerText = ifNotFoundDefault(FeatureType[feature.type], "Unknown");
        featuresValues[feature.id].innerText = feature.value;
    });
}

function updateKeypad() {
    const keys = range1to9.map(i => document.getElementById(`keyKey${i}`));
    const features = range1to3.map(i => document.getElementById(`keyFeature${i}`));
    const featuresCategories = range1to3.map(i => document.getElementById(`keyFeature${i}Category`));
    const featuresValues = range1to3.map(i => document.getElementById(`keyFeature${i}Value`));

    keys.forEach(it => it.style.display = 'none');
    features.forEach(it => it.style.display = 'none');

    state.noteDetails.gateDetails.buttons.forEach( button => {
        keys[button.id].style.display = 'block';
        keys[button.id].innerText = button.name;
        keys[button.id].diabled = button.broken;
        keys[button.id].classList = ['key', 'key-' + colorMapping[button.color], button.broken ? 'key-broken' : 'key-not-broken'].join(" ");
    });

    state.noteDetails.gateDetails.features.forEach(feature => {
        features[feature.id].style.display = 'block';
        featuresCategories[feature.id].innerText = ifNotFoundDefault(FeatureType[feature.type], "Unknown");
        featuresValues[feature.id].innerText = feature.value;
    });
    document.getElementById('keyInputScreen').innerText = ' ' + state.noteDetails.code + ' ';
}

function updateBackground() {
    if (typeof state.noteDetails !== "undefined"
            && state.noteDetails !== null
            && typeof state.noteDetails.gateDetails !== "undefined"
            && state.noteDetails.gateDetails !== null) {

        updateBackgroundTo(state.noteDetails.gateDetails.graphics);
    }
}

function updateBackgroundTo(graphics) {
    document.getElementById('background-image-holder').style.backgroundImage = `url('/images/${graphics}.png')`;
}


function updateNotes() {
    document.getElementById('notesNotes').innerHTML = state.noteDetails.notes.map(generateNote).join("");
}

function openGate() {
    if (visibleMenu === GateType.WIRES) {
        openWires();
    } else if (visibleMenu === GateType.KEYPAD) {
        openKeypad();
    }
}

function openWires() {
    closeAllGraphical();
    document.getElementById(GameMenu.WIRES).style.display = 'block';
}

function openKeypad() {
    closeAllGraphical();
    document.getElementById(GameMenu.KEYPAD).style.display = 'block';
}

function openNotes() {
    closeAllGraphical();
    document.getElementById(GameMenu.NOTES).style.display = 'block';
}

function openTeam() {
    closeAllGraphical();
    document.getElementById(GameMenu.TEAM).style.display = 'block';
}

function openView() {
    closeAllGraphical();
    document.getElementById('viewTitle').innerText = '';
    document.getElementById('viewContent').innerText = '';
    document.getElementById('viewContinue').style.display = 'none';
    document.getElementById('viewLeave').style.display = 'none';
    document.getElementById(GameMenu.VIEW).style.display = 'block';
}

function notifyStateChanged() {
    if (state.state === GameState.LOBBY) {
        showLobby();

    } else if (state.state === GameState.CLOSED) {
        showView();
        activeFragment = GameActiveFragments.END;

    } else if (state.state === GameState.INGAME_DELAY
            || state.state === GameState.INGAME_SELECT
            || state.state === GameState.INGAME_TASK
            || state.state === GameState.INGAME_INTRO
            || state.state === GameState.WIN
            || state.state === GameState.LOSE) {

        showIngame();

    } else if (state.state === GameState.FAILED) {
        const reason = ifNotFoundDefault(hintMapping[state.hint], "Failed to connect...");
        console.error(`EZK: Failed due to: ${reason}`);
        showFailed(reason);
    }
}

function notifyPlayersChanged() {
    if (state.state === GameState.LOBBY) {
        document.getElementById('lobbyPlayers').innerHTML = players.map(generateLobbyPlayer).join("");
        document.getElementById('lobbyReadyPlayers').innerText = players.filter(p => p.ready).length;
    } else {
        document.getElementById('teamPlayers').innerHTML = players.map(generateTeamPlayer).join("");
    }
}

function localState() {
    return { innerState: -1 };
}

function goLeft() {
    sendGoDirection(Direction.LEFT);
}

function goForward() {
    sendGoDirection(Direction.FORWARD);
}

function goRight() {
    sendGoDirection(Direction.RIGHT);
}

function onWireClick(i) {
    if (!(document.getElementById(`wireWire${i + 1}`).classList.contains('wire-broken')))
        sendButtonClick(i);
}

function onKeyClick(i) {
    if (!(document.getElementById(`keyKey${i + 1}`).classList.contains('key-broken')))
        sendButtonClick(i);
}

function sendReady() {
    document.getElementById('lobbyReadyButton').disabled = true;
    stompClient.send("/app/ready", {}, JSON.stringify({ }));
}

function sendStartGame() {
    stompClient.send("/app/start", {}, JSON.stringify({ }));
}

function sendSkipIntro() {
    stompClient.send("/app/next", {}, JSON.stringify(localState()));
}

function sendGoDirection(direction) {
    stompClient.send(`/app/path/${direction}`, {}, JSON.stringify(localState()));
}

function sendButtonClick(i) {
    stompClient.send(`/app/click/${i}`, {}, JSON.stringify(localState()));
}

function sendAbort() {
    stompClient.send("/app/abort", {}, JSON.stringify(localState()));
}

function sendSubmit() {
    stompClient.send("/app/submit", {}, JSON.stringify(localState()));
}

function sendNext() {
    stompClient.send("/app/next", {}, JSON.stringify(localState()));
}

function generateLobbyPlayer(player) {
    return `<tr class="tr-host">
                <td><div class="host">${player.host ? '@' : ''}</div></td>
                <td><div class="${player.ready ? 'ready-sign' : 'not-ready-sign'}"></div></td>
                <td>${player.name}</td>
            </tr>`;
}

function generateTeamPlayer(player) {
    return `<tr><td>${player.name}</td></tr>`;
}

function generateNote(note) {
    return `<div class="note${note.type === NoteType.PATH_INFO ? ' note-path' : ''}">
                <h4>${note.title}</h4>
                <p>${note.text}</p>
            </div>`;
}

function getUniqueId() {
    if (sessionStorage.getItem('uniqueId') == null)
        sessionStorage.setItem('uniqueId', 'webui_' + Math.random().toString(36).substr(2, 9));
    return sessionStorage.getItem('uniqueId');
}

function ifNotFoundDefault(expression, defaultValue) {
    if (typeof expression === "undefined")
        return defaultValue;
    return expression;
}
