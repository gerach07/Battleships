import Foundation

func createEmptyBoard() -> Board {
    Array(repeating: Array(repeating: CellState.WATER, count: GRID_SIZE), count: GRID_SIZE)
}

func canPlaceShipOnBoard(
    board: Board, row: Int, col: Int, length: Int, dir: String
) -> (Bool, [(Int, Int)]) {
    var cells: [(Int, Int)] = []
    if dir == "horizontal" {
        guard col + length <= GRID_SIZE else { return (false, []) }
        for i in 0..<length {
            guard board[row][col + i] == CellState.WATER else { return (false, []) }
            cells.append((row, col + i))
        }
    } else {
        guard row + length <= GRID_SIZE else { return (false, []) }
        for i in 0..<length {
            guard board[row + i][col] == CellState.WATER else { return (false, []) }
            cells.append((row + i, col))
        }
    }
    let cellSet = Set(cells.map { "\($0.0),\($0.1)" })
    for c in cells {
        for dr in -1...1 {
            for dc in -1...1 {
                if dr == 0 && dc == 0 { continue }
                let nr = c.0 + dr, nc = c.1 + dc
                if nr < 0 || nr >= GRID_SIZE || nc < 0 || nc >= GRID_SIZE { continue }
                if cellSet.contains("\(nr),\(nc)") { continue }
                if board[nr][nc] == CellState.SHIP { return (false, []) }
            }
        }
    }
    return (true, cells)
}

func generateRandomPlacement() -> (Board, [PlacedShip])? {
    for _ in 0..<500 {
        var board = Array(repeating: Array(repeating: CellState.WATER, count: GRID_SIZE), count: GRID_SIZE)
        var placements: [PlacedShip] = []
        var failed = false
        for ship in SHIPS {
            var placed = false
            for _ in 0..<150 {
                let dir = Bool.random() ? "horizontal" : "vertical"
                let r = Int.random(in: 0..<GRID_SIZE)
                let c = Int.random(in: 0..<GRID_SIZE)
                let (valid, cells) = canPlaceShipOnBoard(board: board, row: r, col: c, length: ship.length, dir: dir)
                if valid {
                    for (cr, cc) in cells { board[cr][cc] = CellState.SHIP }
                    placements.append(PlacedShip(shipId: ship.id, row: r, col: c, length: ship.length, direction: dir, cells: cells))
                    placed = true
                    break
                }
            }
            if !placed { failed = true; break }
        }
        if !failed { return (board, placements) }
    }
    return nil
}

func getSurroundingKeys(shipCells: [(Int, Int)]) -> Set<String> {
    let shipSet = Set(shipCells.map { "\($0.0),\($0.1)" })
    var ring = Set<String>()
    for c in shipCells {
        for dr in -1...1 {
            for dc in -1...1 {
                let nr = c.0 + dr, nc = c.1 + dc
                if nr < 0 || nr >= GRID_SIZE || nc < 0 || nc >= GRID_SIZE { continue }
                let key = "\(nr),\(nc)"
                if !shipSet.contains(key) { ring.insert(key) }
            }
        }
    }
    return ring
}

func overlayBoard(_ board: Board, sunkSet: Set<String>) -> Board {
    if sunkSet.isEmpty { return board }
    return board.enumerated().map { (ri, row) in
        row.enumerated().map { (ci, cell) in
            let key = "\(ri),\(ci)"
            if sunkSet.contains(key) { return CellState.SUNK }
            if sunkSet.contains("\(key)_safe") && (cell == CellState.WATER || cell == CellState.MISS) { return CellState.SAFE }
            return cell
        }
    }
}

func parseBoardFromJson(_ json: [[Any]]) -> Board {
    var board = createEmptyBoard()
    for i in 0..<min(GRID_SIZE, json.count) {
        let row = json[i]
        for j in 0..<min(GRID_SIZE, row.count) {
            if let cell = row[j] as? String {
                board[i][j] = cell
            }
        }
    }
    return board
}

func parseTimeLeft(_ dict: [String: Any]) -> [String: Double] {
    var result: [String: Double] = [:]
    for (key, val_) in dict {
        if let d = val_ as? Double { result[key] = d }
        else if let i = val_ as? Int { result[key] = Double(i) }
    }
    return result
}
