'use strict';

function generateBox(room) {
    return `<article class="room">
            <h2>${room.code}</h2>
            <button class="join-btn" onclick="${room.locked ? 'openModal' : 'joinRoom'}('${room.code}'); return false">join</button>
            <table>
                <tr>
                    <td>Players</td>
                    <td><span class="data">${room.connectedPlayers}</span>/<span>${room.maxPlayers}</span></td>
                    <td>Host</td>
                    <td class="data">${room.owner}</td>
                </tr>
                <tr>
                    <td>Difficulty</td>
                    <td>
                        <span class="box-${(room.difficulty >= 1) ? 'full' : 'empty'}"></span>
                        <span class="box-${(room.difficulty >= 2) ? 'full' : 'empty'}"></span>
                        <span class="box-${(room.difficulty >= 3) ? 'full' : 'empty'}"></span></td>
                    <td>Map</td>
                    <td class="data">${room.map}</td>
                </tr>
                <tr>
                    <td>Private</td>
                    <td class="data"><span class="material-icons">${room.locked ? 'lock' : 'lock_open'}</span></td>
                </tr>
            </table>
        </article>`;
}

function refresh() {
    document.getElementById('rooms').innerHTML = '<h2>...</h2>';
    fetch('/api/rooms')
        .then(response => response.json())
        .then(data => {
            document.getElementById('rooms').innerHTML = data.map(generateBox).join('');
        });
}

function closeModal() {
    document.getElementById('password-prompt').style.display = 'none';
}

function openModal(code) {
    document.getElementById('roomCodeField').value = code;
    document.getElementById('roomPasswordField').value = '';
    document.getElementById('password-prompt').style.display = 'flex';

    document.getElementById('roomPasswordField').focus();
}