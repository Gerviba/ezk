'use strict';

const MAX_PLAYERS = 6;
const MAX_DIFFICULTY = 3;

function getMaxPlayers() {
    for (let i = 2; i <= MAX_PLAYERS; i++) {
        if (document.getElementById(`max${i}`).disabled)
            return i;
    }
    return 6;
}

function setMaxPlayers(maxPlayers) {
    for (let i = 2; i <= MAX_PLAYERS; i++)
        document.getElementById(`max${i}`).disabled = (maxPlayers === i);
}

function getDifficulty() {
    for (let i = 1; i <= MAX_DIFFICULTY; i++) {
        if (document.getElementById(`diff${i}`).disabled)
            return i;
    }
    return 3;
}

function setDifficulty(difficulty) {
    for (let i = 1; i <= MAX_DIFFICULTY; i++)
        document.getElementById(`diff${i}`).disabled = (difficulty === i);
}

function getPassword() {
    return document.getElementById('private').checked ? document.getElementById('password').value : '';
}

function getMap() {
    let map = document.getElementById('map');
    return map.options[map.selectedIndex].value;
}

function generateData() {
    return {
        map: getMap(),
        difficulty: getDifficulty(),
        maxPlayers: getMaxPlayers(),
        locked: document.getElementById('private').checked,
        password: getPassword()
    };
}

function createRoom() {
    document.getElementById('feedback').innerText = '...';
    fetch('/api/create', {
            method: 'POST',
            cache: 'no-cache',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json',
                'Content-type': 'application/json;charset=UTF-8'
            },
            redirect: 'follow',
            referrerPolicy: 'no-referrer',
            body: JSON.stringify(generateData())
        })
        .then(response => response.json())
        .then(data => {
            if (data.done) {
                joinRoom(data.roomCode, getPassword());
            } else {
                document.getElementById('feedback').innerText = data.hint;
            }
        });
}

function setPasswordEnabled() {
    document.getElementById('password').disabled = !(document.getElementById('private').checked);
}