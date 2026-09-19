import Foundation
import SwiftUI

enum Language: String, CaseIterable {
    case en, lv, ru

    var displayName: String {
        switch self {
        case .en: return "English"
        case .lv: return "Latviešu"
        case .ru: return "Русский"
        }
    }

    var flag: String {
        switch self {
        case .en: return "🇬🇧"
        case .lv: return "🇱🇻"
        case .ru: return "🇷🇺"
        }
    }
}

struct I18nStrings {
    // App / Header
    let appTitle: String
    let appSubtitle: String
    // Profile & Leaderboard
    let profile: String
    let profileInfo: String
    let editDetails: String
    let winsLabel: String
    let save: String
    let signOut: String
    let close: String
    let leaderboard: String
    let topPlayers: String
    let loadingLeaderboard: String
    let gamesPlayed: String
    let rank: String
    let player: String
    let totalWins: String
    let matches: String
    let displayName: String
    let profileUpdated: String
    let saving: String
    let saveProfile: String
    let editProfile: String
    // Login
    let battleships: String
    let multiplayerNavalCombat: String
    let createGame: String
    let joinGame: String
    let createNewGame: String
    let roomPinOptional: String
    let threedigitPin: String
    let timePerPlayer: String
    let createRoom: String
    let back: String
    let joinGameTitle: String
    let roomCode: String
    let roomCodePlaceholder: String
    let pinIfRequired: String
    let joinRoom: String
    let availableRooms: String
    let refresh: String
    let noRoomsAvailable: String
    let waiting: String
    let placing: String
    let battle: String
    let enterPin: String
    let pinPlaceholder: String
    let incorrectPin: String
    let join: String
    let spectate: String
    let spectatorName: String
    let enterYourName: String
    let yourNamePlaceholder: String
    let watchGame: String
    let createAndJoin: String
    let joinGameBtn: String
    // Waiting Room
    let waitingForOpponent: String
    let roomCodeLabel: String
    let pinLabel: String
    let timeLabel: String
    let roomCodeCopied: String
    let copyCode: String
    let joinMyGame: String
    let shareRoom: String
    let share: String
    let leaveRoom: String
    let hostBadge: String
    let hostControls: String
    let startGame: String
    let kickPlayer: String
    let confirmKick: String
    let hostHint: String
    let waitingForHost: String
    let waitingForHostHint: String
    let youSlot: String
    let readyStatus: String
    let waitingFor: String
    let notJoined: String
    // Placement
    let spectating: String
    let playersPlacingShips: String
    let placeYourShips: String
    let horiz: String
    let vert: String
    let placementHint: String
    let cantPlaceThere: String
    let shipMoved: String
    let opponentIsReady: String
    let opponentPlacing: String
    let lockedHint: String
    let shipsCount: String
    let notReady: String
    let ready: String
    let placeAllFirst: String
    let leave: String
    // Battle
    let namesTurn: String
    let yourTurnFire: String
    let extraShotHint: String
    let opponentsTurn: String
    let enemyWaters: String
    let yourFleet: String
    let spectatorCount: String
    let surrender: String
    let yourHits: String
    let theirHits: String
    // Game Over
    let gameOver: String
    let victory: String
    let defeat: String
    let winsMessage: String
    let youSunkEnemy: String
    let destroyedYourFleet: String
    let opWantsRematch: String
    let accept: String
    let decline: String
    let waitingForOpponentRematch: String
    let cancelRematch: String
    let playAgain: String
    let mainMenu: String
    // Chat
    let chat: String
    let noMessagesYet: String
    let messagePlaceholder: String
    let send: String
    let toggleSound: String
    let toggleMusic: String
    let openChat: String
    let closeChat: String
    // Timer
    let you: String
    let opponent: String
    let ticking: String
    // Connection
    let connectionLost: String
    let reconnecting: String
    let tapToRetry: String
    let reconnectingToGame: String
    let reconnected: String
    let couldNotRejoin: String
    let opponentLostConnection: String
    let opponentReconnected: String
    let opponentDisconnected: String
    // Messages
    let gameStartedByHost: String
    let youWereKicked: String
    let playerKicked: String
    let failedLoadRooms: String
    let failedValidatePin: String
    let roomCodeRequired: String
    let enterNameFirst: String
    let bothPlayersIn: String
    let errorJoining: String
    let opponentJoined: String
    let unknownError: String
    let waitingOpponentPlace: String
    let sunkTheirShip: String
    let yourShipSunk: String
    let hitShootAgain: String
    let theyHitYourShip: String
    let missOpponentTurn: String
    let theyMissedYourTurn: String
    let newGamePlaceShips: String
    let playerLeftGame: String
    let playerLeftWaiting: String
    let youSurrendered: String
    let opponentSurrendered: String
    let opponentDeclinedRematch: String
    let spectatingNames: String
    let yourClockRanOut: String
    let opponentClockRanOut: String
    let confirmSurrender: String
    let yes: String
    let no: String
    let min: String
    let shipCarrier: String
    let shipBattleship: String
    let shipDestroyer: String
    let shipSubmarine: String
    let shipPatrol: String
    // Board legend
    let boardWater: String
    let boardShip: String
    let boardHit: String
    let boardMiss: String
    let boardSunk: String
}

extension String {
    func fmt(_ args: Any?...) -> String {
        var result = self
        for (i, arg) in args.enumerated() {
            result = result.replacingOccurrences(of: "{\(i)}", with: "\(arg ?? "")")
        }
        return result
    }
}

// MARK: - English

let stringsEN = I18nStrings(
    appTitle: "Battleships",
    appSubtitle: "Multiplayer · Real-time",
    profile: "Profile",
    profileInfo: "Profile Info",
    editDetails: "Edit Details",
    winsLabel: "Wins",
    save: "Save",
    signOut: "Sign Out",
    close: "Close",
    leaderboard: "Leaderboard",
    topPlayers: "Top players",
    loadingLeaderboard: "Loading leaderboard...",
    gamesPlayed: "games played",
    rank: "Rank",
    player: "Player",
    totalWins: "Total Wins",
    matches: "Matches",
    displayName: "Display Name",
    profileUpdated: "Profile updated successfully!",
    saving: "Saving...",
    saveProfile: "Save Profile",
    editProfile: "Edit Profile",
    battleships: "BATTLESHIPS",
    multiplayerNavalCombat: "Multiplayer Naval Combat",
    createGame: "Create Game",
    joinGame: "Join Game",
    createNewGame: "Create New Game",
    roomPinOptional: "Room PIN (optional)",
    threedigitPin: "3-digit PIN",
    timePerPlayer: "Time Per Player",
    createRoom: "Create Room",
    back: "Back",
    joinGameTitle: "Join Game",
    roomCode: "Room Code",
    roomCodePlaceholder: "e.g. ABC123",
    pinIfRequired: "PIN (if required)",
    joinRoom: "Join Room",
    availableRooms: "Available Rooms",
    refresh: "Refresh",
    noRoomsAvailable: "No rooms available",
    waiting: "Waiting",
    placing: "Placing",
    battle: "Battle",
    enterPin: "Enter PIN",
    pinPlaceholder: "PIN",
    incorrectPin: "Incorrect PIN",
    join: "Join",
    spectate: "Spectate",
    spectatorName: "Spectator Name",
    enterYourName: "Enter Your Name",
    yourNamePlaceholder: "Your name",
    watchGame: "Watch Game",
    createAndJoin: "Create & Join",
    joinGameBtn: "Join Game",
    waitingForOpponent: "Waiting for Opponent",
    roomCodeLabel: "Room Code",
    pinLabel: "PIN",
    timeLabel: "Time",
    roomCodeCopied: "Room code copied!",
    copyCode: "Copy Code",
    joinMyGame: "Join my Battleships game!",
    shareRoom: "Share room",
    share: "Share",
    leaveRoom: "Leave Room",
    hostBadge: "HOST",
    hostControls: "Host Controls",
    startGame: "Start Game",
    kickPlayer: "Kick",
    confirmKick: "Are you sure you want to kick this player?",
    hostHint: "Start when both players are ready",
    waitingForHost: "Waiting for host to start…",
    waitingForHostHint: "The host will start the game when ready",
    youSlot: "You",
    readyStatus: "Ready",
    waitingFor: "Waiting…",
    notJoined: "Not joined",
    spectating: "Spectating",
    playersPlacingShips: "Players are placing ships…",
    placeYourShips: "Place Your Ships",
    horiz: "↔ Horiz",
    vert: "↕ Vert",
    placementHint: "Ships need 1-cell gap including corners",
    cantPlaceThere: "Can't place there!",
    shipMoved: "Ship moved!",
    opponentIsReady: "{0} is ready!",
    opponentPlacing: "Waiting for {0} to place ships…",
    lockedHint: "Fleet locked in — tap Unready to make changes",
    shipsCount: "Ships",
    notReady: "Not Ready",
    ready: "Ready",
    placeAllFirst: "Place all ships first",
    leave: "Leave",
    namesTurn: "{0}'s turn",
    yourTurnFire: "Your Turn — Fire!",
    extraShotHint: "Hit a ship for an extra shot!",
    opponentsTurn: "{0}'s Turn",
    enemyWaters: "{0}'s Waters",
    yourFleet: "Your Fleet",
    spectatorCount: "spectator(s)",
    surrender: "Surrender",
    yourHits: "Your hits",
    theirHits: "Their hits",
    gameOver: "Game Over",
    victory: "VICTORY!",
    defeat: "DEFEAT",
    winsMessage: "{0} wins!",
    youSunkEnemy: "You sank {0}'s fleet!",
    destroyedYourFleet: "{0} destroyed your fleet.",
    opWantsRematch: "{0} wants a rematch!",
    accept: "Accept",
    decline: "Decline",
    waitingForOpponentRematch: "Waiting for opponent…",
    cancelRematch: "Cancel request",
    playAgain: "Play Again",
    mainMenu: "Main Menu",
    chat: "Chat",
    noMessagesYet: "No messages yet ⛵",
    messagePlaceholder: "Message…",
    send: "Send",
    toggleSound: "Toggle sound effects",
    toggleMusic: "Toggle music",
    openChat: "Open chat",
    closeChat: "Close chat",
    you: "You",
    opponent: "Opponent",
    ticking: "ticking",
    connectionLost: "Connection Lost",
    reconnecting: "Attempting to reconnect…",
    tapToRetry: "(Tap screen to force retry)",
    reconnectingToGame: "Reconnecting...",
    reconnected: "Reconnected!",
    couldNotRejoin: "Could not rejoin",
    opponentLostConnection: "{0} lost connection, waiting...",
    opponentReconnected: "{0} reconnected!",
    opponentDisconnected: "{0} disconnected",
    gameStartedByHost: "Game started! Place your ships.",
    youWereKicked: "You have been kicked from the room",
    playerKicked: "Player has been kicked",
    failedLoadRooms: "Failed to load rooms",
    failedValidatePin: "Failed to validate PIN",
    roomCodeRequired: "Room code required",
    enterNameFirst: "Enter your name first",
    bothPlayersIn: "Both players in! Place your ships.",
    errorJoining: "Error joining game",
    opponentJoined: "{0} joined! Place your ships.",
    unknownError: "Unknown error",
    waitingOpponentPlace: "Waiting for {0} to finish placing…",
    sunkTheirShip: "You sunk their {0}!",
    yourShipSunk: "Your {0} was sunk! {1} shoots again.",
    hitShootAgain: "Hit!",
    theyHitYourShip: "They hit your ship! They shoot again.",
    missOpponentTurn: "Miss — {0}'s turn.",
    theyMissedYourTurn: "They missed! Your turn.",
    newGamePlaceShips: "New game! Place your ships.",
    playerLeftGame: "{0} left the game",
    playerLeftWaiting: "{0} left — waiting for a new opponent",
    youSurrendered: "You surrendered",
    opponentSurrendered: "{0} surrendered",
    opponentDeclinedRematch: "{0} declined the rematch",
    spectatingNames: "Spectating: {0}",
    yourClockRanOut: "⏰ Your clock ran out! You lose.",
    opponentClockRanOut: "⏰ {0}'s clock ran out! You win!",
    confirmSurrender: "Are you sure you want to surrender?",
    yes: "Yes",
    no: "No",
    min: "min",
    shipCarrier: "Carrier",
    shipBattleship: "Battleship",
    shipDestroyer: "Destroyer",
    shipSubmarine: "Submarine",
    shipPatrol: "Patrol",
    boardWater: "Water",
    boardShip: "Ship",
    boardHit: "Hit",
    boardMiss: "Miss",
    boardSunk: "Sunk"
)

// MARK: - Latvian

let stringsLV = I18nStrings(
    appTitle: "Kuģu Kaujas",
    appSubtitle: "Daudzspēlētāju · Reāllaikā",
    profile: "Profils",
    profileInfo: "Profila informācija",
    editDetails: "Rediģēt datus",
    winsLabel: "Uzvaras",
    save: "Saglabāt",
    signOut: "Iziet",
    close: "Aizvērt",
    leaderboard: "Reitings",
    topPlayers: "Labākie spēlētāji",
    loadingLeaderboard: "Ielādē reitingu...",
    gamesPlayed: "spēles aizvadītas",
    rank: "Vieta",
    player: "Spēlētājs",
    totalWins: "Kopējās uzvaras",
    matches: "Spēles",
    displayName: "Rādītais vārds",
    profileUpdated: "Profils veiksmīgi atjaunināts!",
    saving: "Saglabā...",
    saveProfile: "Saglabāt profilu",
    editProfile: "Rediģēt profilu",
    battleships: "KUĢU KAUJAS",
    multiplayerNavalCombat: "Daudzspēlētāju jūras kauja",
    createGame: "Izveidot spēli",
    joinGame: "Pievienoties",
    createNewGame: "Izveidot jaunu spēli",
    roomPinOptional: "Istabas PIN (pēc izvēles)",
    threedigitPin: "3 ciparu PIN",
    timePerPlayer: "Laiks spēlētājam",
    createRoom: "Izveidot istabu",
    back: "Atpakaļ",
    joinGameTitle: "Pievienoties spēlei",
    roomCode: "Istabas kods",
    roomCodePlaceholder: "piem. ABC123",
    pinIfRequired: "PIN (ja nepieciešams)",
    joinRoom: "Pievienoties istabai",
    availableRooms: "Pieejamās istabas",
    refresh: "Atjaunot",
    noRoomsAvailable: "Nav pieejamu istabu",
    waiting: "Gaida",
    placing: "Izvieto",
    battle: "Kauja",
    enterPin: "Ievadiet PIN",
    pinPlaceholder: "PIN",
    incorrectPin: "Nepareizs PIN",
    join: "Pievienoties",
    spectate: "Skatīties",
    spectatorName: "Skatītāja vārds",
    enterYourName: "Ievadiet savu vārdu",
    yourNamePlaceholder: "Jūsu vārds",
    watchGame: "Skatīties spēli",
    createAndJoin: "Izveidot un pievienoties",
    joinGameBtn: "Pievienoties",
    waitingForOpponent: "Gaida pretinieku",
    roomCodeLabel: "Istabas kods",
    pinLabel: "PIN",
    timeLabel: "Laiks",
    roomCodeCopied: "Istabas kods nokopēts!",
    copyCode: "Kopēt kodu",
    joinMyGame: "Pievienojies manai Kuģu Kauju spēlei!",
    shareRoom: "Dalīties ar istabu",
    share: "Dalīties",
    leaveRoom: "Pamest istabu",
    hostBadge: "SAIMNIEKS",
    hostControls: "Saimnieka vadība",
    startGame: "Sākt spēli",
    kickPlayer: "Izmest",
    confirmKick: "Vai tiešām vēlaties izmest šo spēlētāju?",
    hostHint: "Sāciet, kad abi spēlētāji ir gatavi",
    waitingForHost: "Gaida, kad saimnieks sāks…",
    waitingForHostHint: "Saimnieks sāks spēli, kad būs gatavs",
    youSlot: "Jūs",
    readyStatus: "Gatavs",
    waitingFor: "Gaida…",
    notJoined: "Nav pievienojies",
    spectating: "Skatāties",
    playersPlacingShips: "Spēlētāji izvieto kuģus…",
    placeYourShips: "Izvieto savus kuģus",
    horiz: "↔ Horiz",
    vert: "↕ Vert",
    placementHint: "Starp kuģiem jābūt 1 šūnas atstarpei, ieskaitot stūrus",
    cantPlaceThere: "Nevar novietot tur!",
    shipMoved: "Kuģis pārvietots!",
    opponentIsReady: "{0} ir gatavs!",
    opponentPlacing: "Gaida, kamēr {0} izvieto kuģus…",
    lockedHint: "Flote apstiprināta — nospiediet Atsaukt, lai mainītu",
    shipsCount: "Kuģi",
    notReady: "Nav gatavs",
    ready: "Gatavs",
    placeAllFirst: "Vispirms izvietojiet visus kuģus",
    leave: "Iziet",
    namesTurn: "{0} gājiens",
    yourTurnFire: "Jūsu gājiens — šaujiet!",
    extraShotHint: "Trāpiet kuģim, lai šautu vēlreiz!",
    opponentsTurn: "{0} gājiens",
    enemyWaters: "{0} ūdeņi",
    yourFleet: "Jūsu flote",
    spectatorCount: "skatītāj(i)",
    surrender: "Padoties",
    yourHits: "Jūsu trāpījumi",
    theirHits: "Viņu trāpījumi",
    gameOver: "Spēle beigusies",
    victory: "UZVARA!",
    defeat: "ZAUDĒJUMS",
    winsMessage: "{0} uzvar!",
    youSunkEnemy: "Jūs nogremdējāt visu {0} floti!",
    destroyedYourFleet: "{0} iznīcināja jūsu floti.",
    opWantsRematch: "{0} vēlas revanšu!",
    accept: "Pieņemt",
    decline: "Noraidīt",
    waitingForOpponentRematch: "Gaida pretinieku…",
    cancelRematch: "Atcelt pieprasījumu",
    playAgain: "Spēlēt vēlreiz",
    mainMenu: "Galvenā izvēlne",
    chat: "Čats",
    noMessagesYet: "Vēl nav ziņojumu ⛵",
    messagePlaceholder: "Ziņojums…",
    send: "Sūtīt",
    toggleSound: "Ieslēgt/izslēgt skaņas",
    toggleMusic: "Ieslēgt/izslēgt mūziku",
    openChat: "Atvērt čatu",
    closeChat: "Aizvērt čatu",
    you: "Jūs",
    opponent: "Pretinieks",
    ticking: "iet",
    connectionLost: "Savienojums zaudēts",
    reconnecting: "Mēģina atkārtoti savienoties…",
    tapToRetry: "(Pieskarieties ekrānam, lai mēģinātu vēlreiz)",
    reconnectingToGame: "Atkārtoti pieslēdzas...",
    reconnected: "Atkārtoti pieslēgts!",
    couldNotRejoin: "Nevarēja pievienoties atpakaļ",
    opponentLostConnection: "{0} zaudēja savienojumu, gaida...",
    opponentReconnected: "{0} atkārtoti pieslēdzās!",
    opponentDisconnected: "{0} atvienojās",
    gameStartedByHost: "Spēle sākta! Izvietojiet savus kuģus.",
    youWereKicked: "Jūs tikāt izmests no istabas",
    playerKicked: "Spēlētājs tika izmests",
    failedLoadRooms: "Neizdevās ielādēt istabas",
    failedValidatePin: "Neizdevās pārbaudīt PIN",
    roomCodeRequired: "Istabas kods nepieciešams",
    enterNameFirst: "Vispirms ievadiet savu vārdu",
    bothPlayersIn: "Abi spēlētāji klāt! Izvietojiet savus kuģus.",
    errorJoining: "Kļūda pievienojoties spēlei",
    opponentJoined: "{0} pievienojās! Izvietojiet savus kuģus.",
    unknownError: "Nezināma kļūda",
    waitingOpponentPlace: "Gaida, kamēr {0} izvietosies…",
    sunkTheirShip: "Jūs nogremdējāt viņu {0}!",
    yourShipSunk: "Jūsu {0} tika nogremdēts! {1} šauj vēlreiz.",
    hitShootAgain: "Trāpīts!",
    theyHitYourShip: "Viņi trāpīja jūsu kuģim! Viņi šauj vēlreiz.",
    missOpponentTurn: "Garām — {0} gājiens.",
    theyMissedYourTurn: "Viņi netrāpīja! Jūsu gājiens.",
    newGamePlaceShips: "Jauna spēle! Izvietojiet savus kuģus.",
    playerLeftGame: "{0} pameta spēli",
    playerLeftWaiting: "{0} aizgāja — gaida jaunu pretinieku",
    youSurrendered: "Jūs padevāties",
    opponentSurrendered: "{0} padevās",
    opponentDeclinedRematch: "{0} noraidīja revanšu",
    spectatingNames: "Skatāties: {0}",
    yourClockRanOut: "⏰ Jūsu laiks beidzies! Zaudējums.",
    opponentClockRanOut: "⏰ {0} laiks beidzies! Uzvara!",
    confirmSurrender: "Vai tiešām vēlaties padoties?",
    yes: "Jā",
    no: "Nē",
    min: "min",
    shipCarrier: "Gaisa kuģu nesējs",
    shipBattleship: "Karakuģis",
    shipDestroyer: "Iznīcinātājs",
    shipSubmarine: "Zemūdene",
    shipPatrol: "Patrulkuģis",
    boardWater: "Ūdens",
    boardShip: "Kuģis",
    boardHit: "Trāpījums",
    boardMiss: "Garām",
    boardSunk: "Nogremdēts"
)

// MARK: - Russian

let stringsRU = I18nStrings(
    appTitle: "Морской бой",
    appSubtitle: "Мультиплеер · Реальное время",
    profile: "Профиль",
    profileInfo: "Информация профиля",
    editDetails: "Редактировать данные",
    winsLabel: "Победы",
    save: "Сохранить",
    signOut: "Выйти",
    close: "Закрыть",
    leaderboard: "Таблица лидеров",
    topPlayers: "Лучшие игроки",
    loadingLeaderboard: "Загрузка таблицы...",
    gamesPlayed: "игр сыграно",
    rank: "Ранг",
    player: "Игрок",
    totalWins: "Всего побед",
    matches: "Матчи",
    displayName: "Отображаемое имя",
    profileUpdated: "Профиль успешно обновлён!",
    saving: "Сохранение...",
    saveProfile: "Сохранить профиль",
    editProfile: "Редактировать профиль",
    battleships: "МОРСКОЙ БОЙ",
    multiplayerNavalCombat: "Многопользовательский морской бой",
    createGame: "Создать игру",
    joinGame: "Присоединиться",
    createNewGame: "Создать новую игру",
    roomPinOptional: "PIN комнаты (необязательно)",
    threedigitPin: "3-значный PIN",
    timePerPlayer: "Время на игрока",
    createRoom: "Создать комнату",
    back: "Назад",
    joinGameTitle: "Присоединиться к игре",
    roomCode: "Код комнаты",
    roomCodePlaceholder: "напр. ABC123",
    pinIfRequired: "PIN (если требуется)",
    joinRoom: "Войти в комнату",
    availableRooms: "Доступные комнаты",
    refresh: "Обновить",
    noRoomsAvailable: "Нет доступных комнат",
    waiting: "Ожидание",
    placing: "Расстановка",
    battle: "Бой",
    enterPin: "Введите PIN",
    pinPlaceholder: "PIN",
    incorrectPin: "Неверный PIN",
    join: "Войти",
    spectate: "Наблюдать",
    spectatorName: "Имя наблюдателя",
    enterYourName: "Введите ваше имя",
    yourNamePlaceholder: "Ваше имя",
    watchGame: "Смотреть игру",
    createAndJoin: "Создать и войти",
    joinGameBtn: "Присоединиться",
    waitingForOpponent: "Ожидание противника",
    roomCodeLabel: "Код комнаты",
    pinLabel: "PIN",
    timeLabel: "Время",
    roomCodeCopied: "Код комнаты скопирован!",
    copyCode: "Скопировать код",
    joinMyGame: "Присоединяйся к моей игре Морской бой!",
    shareRoom: "Поделиться комнатой",
    share: "Поделиться",
    leaveRoom: "Покинуть комнату",
    hostBadge: "ХОСТ",
    hostControls: "Управление хоста",
    startGame: "Начать игру",
    kickPlayer: "Выгнать",
    confirmKick: "Вы уверены, что хотите выгнать этого игрока?",
    hostHint: "Начните, когда оба игрока готовы",
    waitingForHost: "Ожидание начала от хоста…",
    waitingForHostHint: "Хост начнёт игру, когда будет готов",
    youSlot: "Вы",
    readyStatus: "Готов",
    waitingFor: "Ожидание…",
    notJoined: "Не присоединился",
    spectating: "Наблюдение",
    playersPlacingShips: "Игроки расставляют корабли…",
    placeYourShips: "Расставьте корабли",
    horiz: "↔ Гориз",
    vert: "↕ Верт",
    placementHint: "Между кораблями должен быть зазор в 1 клетку, включая углы",
    cantPlaceThere: "Нельзя поставить сюда!",
    shipMoved: "Корабль перемещён!",
    opponentIsReady: "{0} готов!",
    opponentPlacing: "Ожидание, пока {0} расставит корабли…",
    lockedHint: "Флот подтверждён — нажмите Отменить, чтобы изменить",
    shipsCount: "Корабли",
    notReady: "Не готов",
    ready: "Готов",
    placeAllFirst: "Сначала расставьте все корабли",
    leave: "Выйти",
    namesTurn: "Ход {0}",
    yourTurnFire: "Ваш ход — стреляйте!",
    extraShotHint: "Попадите в корабль для доп. выстрела!",
    opponentsTurn: "Ход {0}",
    enemyWaters: "Воды {0}",
    yourFleet: "Ваш флот",
    spectatorCount: "наблюдатель(ей)",
    surrender: "Сдаться",
    yourHits: "Ваши попадания",
    theirHits: "Их попадания",
    gameOver: "Игра окончена",
    victory: "ПОБЕДА!",
    defeat: "ПОРАЖЕНИЕ",
    winsMessage: "{0} побеждает!",
    youSunkEnemy: "Вы потопили весь флот {0}!",
    destroyedYourFleet: "{0} уничтожил ваш флот.",
    opWantsRematch: "{0} хочет реванш!",
    accept: "Принять",
    decline: "Отклонить",
    waitingForOpponentRematch: "Ожидание противника…",
    cancelRematch: "Отменить запрос",
    playAgain: "Играть снова",
    mainMenu: "Главное меню",
    chat: "Чат",
    noMessagesYet: "Сообщений пока нет ⛵",
    messagePlaceholder: "Сообщение…",
    send: "Отправить",
    toggleSound: "Вкл/выкл звуковые эффекты",
    toggleMusic: "Вкл/выкл музыку",
    openChat: "Открыть чат",
    closeChat: "Закрыть чат",
    you: "Вы",
    opponent: "Противник",
    ticking: "идёт",
    connectionLost: "Соединение потеряно",
    reconnecting: "Попытка переподключения…",
    tapToRetry: "(Нажмите на экран для повторной попытки)",
    reconnectingToGame: "Переподключение...",
    reconnected: "Переподключено!",
    couldNotRejoin: "Не удалось переподключиться",
    opponentLostConnection: "{0} потерял соединение, ожидание...",
    opponentReconnected: "{0} переподключился!",
    opponentDisconnected: "{0} отключился",
    gameStartedByHost: "Игра началась! Расставьте корабли.",
    youWereKicked: "Вас выгнали из комнаты",
    playerKicked: "Игрок был выгнан",
    failedLoadRooms: "Не удалось загрузить комнаты",
    failedValidatePin: "Не удалось проверить PIN",
    roomCodeRequired: "Необходим код комнаты",
    enterNameFirst: "Сначала введите имя",
    bothPlayersIn: "Оба игрока на месте! Расставьте корабли.",
    errorJoining: "Ошибка при подключении к игре",
    opponentJoined: "{0} присоединился! Расставьте корабли.",
    unknownError: "Неизвестная ошибка",
    waitingOpponentPlace: "Ожидание, пока {0} расставит корабли…",
    sunkTheirShip: "Вы потопили их {0}!",
    yourShipSunk: "Ваш {0} потоплен! {1} стреляет ещё.",
    hitShootAgain: "Попадание!",
    theyHitYourShip: "Они попали в ваш корабль! Они стреляют ещё.",
    missOpponentTurn: "Мимо — ход {0}.",
    theyMissedYourTurn: "Они промахнулись! Ваш ход.",
    newGamePlaceShips: "Новая игра! Расставьте корабли.",
    playerLeftGame: "{0} покинул игру",
    playerLeftWaiting: "{0} ушёл — ожидание нового противника",
    youSurrendered: "Вы сдались",
    opponentSurrendered: "{0} сдался",
    opponentDeclinedRematch: "{0} отклонил реванш",
    spectatingNames: "Наблюдение: {0}",
    yourClockRanOut: "⏰ Ваше время вышло! Вы проиграли.",
    opponentClockRanOut: "⏰ Время {0} вышло! Вы победили!",
    confirmSurrender: "Вы уверены, что хотите сдаться?",
    yes: "Да",
    no: "Нет",
    min: "мин",
    shipCarrier: "Авианосец",
    shipBattleship: "Линкор",
    shipDestroyer: "Эсминец",
    shipSubmarine: "Подлодка",
    shipPatrol: "Катер",
    boardWater: "Вода",
    boardShip: "Корабль",
    boardHit: "Попадание",
    boardMiss: "Мимо",
    boardSunk: "Потоплен"
)

func stringsFor(_ lang: Language) -> I18nStrings {
    switch lang {
    case .en: return stringsEN
    case .lv: return stringsLV
    case .ru: return stringsRU
    }
}

func currentStrings() -> I18nStrings {
    let langCode = UserDefaults.standard.string(forKey: "battleships-lang") ?? "en"
    let language = Language(rawValue: langCode) ?? .en
    return stringsFor(language)
}
