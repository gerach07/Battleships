import React, { useState, useCallback, useMemo, useRef, memo } from 'react';
import { GRID_SIZE, CELL, SHIPS } from '../constants';
import { createEmptyBoard, canPlaceShipOnBoard, generateRandomPlacement } from '../utils/gameHelpers';
import { playSound } from '../utils/sounds';
import { useI18n } from '../i18n/I18nContext';

/** Column header letters — hoisted to avoid re-creating every render */
const COL_HEADERS = Array.from({ length: GRID_SIZE }, (_, i) => String.fromCharCode(65 + i));

const ShipPlacement = memo(({ onShipPlaced, locked = false }) => {
    const { t } = useI18n();
    const [selected, setSelected] = useState(0);
    const [direction, setDirection] = useState('horizontal');
    const [board, setBoard] = useState(createEmptyBoard);
    const [placed, setPlaced] = useState(new Set());
    const [placements, setPlacements] = useState([]); // {shipId,row,col,length,direction,cells}[]
    const [hovered, setHovered] = useState(null);
    const [msg, setMsg] = useState('');
    const [dragState, setDragState] = useState(null); // { shipId, direction, length, offsetInShip, originalPlacement, targetRow, targetCol, hasMoved }
    const gridRef = useRef(null);
    /** Cached grid geometry — computed once on drag start to avoid reflow on every pointermove */
    const gridGeomRef = useRef(null);
    const canPlace = useCallback(
        (row, col, len, dir) => canPlaceShipOnBoard(board, row, col, len, dir),
        [board]
    );

    const preview = useMemo(() => {
        if (dragState && dragState.targetRow != null) {
            const ship = SHIPS[dragState.shipId];
            const { valid, cells } = canPlace(dragState.targetRow, dragState.targetCol, ship.length, dragState.direction);
            if (valid) return new Set(cells.map(c => `${c.row},${c.col}`));
            // Show raw outline even when invalid (for red preview)
            const raw = new Set();
            for (let i = 0; i < ship.length; i++) {
                const r = dragState.direction === 'horizontal' ? dragState.targetRow : dragState.targetRow + i;
                const c = dragState.direction === 'horizontal' ? dragState.targetCol + i : dragState.targetCol;
                if (r >= 0 && r < GRID_SIZE && c >= 0 && c < GRID_SIZE) raw.add(`${r},${c}`);
            }
            return raw;
        }
        if (!hovered || placed.has(selected)) return new Set();
        const { valid, cells } = canPlace(hovered.row, hovered.col, SHIPS[selected].length, direction);
        return valid ? new Set(cells.map(c => `${c.row},${c.col}`)) : new Set();
    }, [hovered, placed, selected, direction, canPlace, dragState]);

    const dragIsValid = useMemo(() => {
        if (!dragState || dragState.targetRow == null) return false;
        const ship = SHIPS[dragState.shipId];
        return canPlace(dragState.targetRow, dragState.targetCol, ship.length, dragState.direction).valid;
    }, [dragState, canPlace]);

    /** Re-sync parent with current placement state (call after any change) */
    const notifyParent = useCallback((newPlacements) => {
        onShipPlaced(newPlacements);
    }, [onShipPlaced]);

    const handleCellClick = useCallback((ri, ci) => {
        // Clicking a placed ship cell → un-place that ship
        const existing = placements.find(p => p.cells.some(c => c.row === ri && c.col === ci));
        if (existing) {
            const newPlacements = placements.filter(p => p.shipId !== existing.shipId);
            const newPlaced = new Set(placed); newPlaced.delete(existing.shipId);
            const newBoard = createEmptyBoard();
            newPlacements.forEach(p => p.cells.forEach(c => { newBoard[c.row][c.col] = CELL.SHIP; }));
            setBoard(newBoard); setPlaced(newPlaced); setPlacements(newPlacements);
            setSelected(existing.shipId);
            notifyParent(newPlacements);
            setMsg(t('ship.removed', SHIPS[existing.shipId].name));
            return;
        }

        if (placed.has(selected)) { setMsg(t('ship.alreadyPlaced', SHIPS[selected].name)); return; }
        const ship = SHIPS[selected];
        const { valid, cells } = canPlace(ri, ci, ship.length, direction);
        if (!valid) { setMsg(t('ship.cantPlace')); return; }
        const nb = board.map(r => [...r]);
        cells.forEach(c => { nb[c.row][c.col] = CELL.SHIP; });
        setBoard(nb);
        const ns = new Set(placed); ns.add(selected); setPlaced(ns);
        const newPlacements = [...placements, { shipId: selected, row: ri, col: ci, length: ship.length, direction, cells }];
        setPlacements(newPlacements);
        notifyParent(newPlacements);
        playSound('place');
        setMsg(t('ship.placed', ship.name));
        const next = SHIPS.findIndex(s => !ns.has(s.id));
        if (next !== -1) setSelected(next);
    }, [placed, selected, direction, board, canPlace, placements, notifyParent, t]);

    const handleRandomize = useCallback(() => {
        const result = generateRandomPlacement();
        if (!result) return;
        // Build placements with cell positions
        const newPlacements = result.placements.map((p, i) => {
            const cells = [];
            if (p.direction === 'horizontal') {
                for (let j = 0; j < p.length; j++) cells.push({ row: p.row, col: p.col + j });
            } else {
                for (let j = 0; j < p.length; j++) cells.push({ row: p.row + j, col: p.col });
            }
            return { shipId: SHIPS[i].id, row: p.row, col: p.col, length: p.length, direction: p.direction, cells };
        });
        setBoard(result.board);
        setPlacements(newPlacements);
        setSelected(0);
        const all = new Set(SHIPS.map(s => s.id)); setPlaced(all);
        notifyParent(newPlacements);
        setMsg(t('ship.randomised'));
    }, [notifyParent, t]);

    const handleClear = useCallback(() => {
        setBoard(createEmptyBoard()); setPlaced(new Set()); setPlacements([]); setSelected(0); setMsg('');
        notifyParent([]);
    }, [notifyParent]);

    /** Calculate grid cell from screen coordinates using cached grid geometry */
    const getCellFromPointer = useCallback((clientX, clientY) => {
        const geom = gridGeomRef.current;
        if (!geom) return null;
        const col = Math.floor((clientX - geom.left - geom.headerW) / geom.cellW);
        const row = Math.floor((clientY - geom.top - geom.headerH) / geom.cellH);
        if (row < 0 || row >= GRID_SIZE || col < 0 || col >= GRID_SIZE) return null;
        return { row, col };
    }, []);

    /** Start dragging a placed ship (pointer down on ship cell) */
    const handleDragStart = useCallback((ri, ci, e) => {
        if (locked) return;
        const existing = placements.find(p => p.cells.some(c => c.row === ri && c.col === ci));
        if (!existing) return;
        e.preventDefault();

        // Cache grid geometry once at drag start to avoid reflow on every pointermove
        const grid = gridRef.current;
        if (grid) {
            const rect = grid.getBoundingClientRect();
            const cols = window.getComputedStyle(grid).gridTemplateColumns.split(' ');
            const rows = window.getComputedStyle(grid).gridTemplateRows.split(' ');
            const headerW = parseFloat(cols[0]);
            const headerH = parseFloat(rows[0]);
            gridGeomRef.current = {
                left: rect.left, top: rect.top,
                headerW, headerH,
                cellW: (rect.width - headerW) / GRID_SIZE,
                cellH: (rect.height - headerH) / GRID_SIZE,
            };
        }

        const ship = SHIPS[existing.shipId];
        const offsetInShip = existing.direction === 'horizontal' ? ci - existing.col : ri - existing.row;
        // Remove ship from board temporarily
        const newPlacements = placements.filter(p => p.shipId !== existing.shipId);
        const newBoard = createEmptyBoard();
        newPlacements.forEach(p => p.cells.forEach(c => { newBoard[c.row][c.col] = CELL.SHIP; }));
        const newPlaced = new Set(placed); newPlaced.delete(existing.shipId);
        setBoard(newBoard); setPlacements(newPlacements); setPlaced(newPlaced);
        setSelected(existing.shipId);
        setDragState({
            shipId: existing.shipId, direction: existing.direction, length: ship.length,
            offsetInShip, originalPlacement: existing,
            targetRow: existing.row, targetCol: existing.col, hasMoved: false,
        });
        setMsg(t('ship.dragToReposition', ship.name));
    }, [locked, placements, placed, t]);

    /** Pointer move during drag (stable callback — uses functional updater) */
    const handlePointerMove = useCallback((e) => {
        const cell = getCellFromPointer(e.clientX, e.clientY);
        if (!cell) return;
        setDragState(prev => {
            if (!prev) return null;
            const tr = prev.direction === 'horizontal' ? cell.row : cell.row - prev.offsetInShip;
            const tc = prev.direction === 'horizontal' ? cell.col - prev.offsetInShip : cell.col;
            if (tr === prev.targetRow && tc === prev.targetCol) return prev;
            return { ...prev, targetRow: tr, targetCol: tc, hasMoved: true };
        });
    }, [getCellFromPointer]);

    /** Finalize ship drop — place at target or restore to original */
    const handlePointerUp = useCallback(() => {
        if (!dragState) return;
        const { shipId, direction: dir, length, originalPlacement, targetRow, targetCol, hasMoved } = dragState;
        const ship = SHIPS[shipId];
        setDragState(null);
        gridGeomRef.current = null;

        if (!hasMoved) {
            // No drag movement — treat as click-to-remove (existing behaviour)
            notifyParent(placements);
            setMsg(t('ship.removed', ship.name));
            return;
        }

        const { valid, cells } = canPlace(targetRow, targetCol, length, dir);
        if (valid) {
            const nb = board.map(r => [...r]);
            cells.forEach(c => { nb[c.row][c.col] = CELL.SHIP; });
            setBoard(nb);
            const ns = new Set(placed); ns.add(shipId); setPlaced(ns);
            const np = [...placements, { shipId, row: targetRow, col: targetCol, length, direction: dir, cells }];
            setPlacements(np); notifyParent(np);
            playSound('place');
            setMsg(t('ship.moved', ship.name));
        } else {
            // Restore to original position
            const { valid: ov, cells: oc } = canPlace(originalPlacement.row, originalPlacement.col, length, dir);
            if (ov) {
                const nb = board.map(r => [...r]);
                oc.forEach(c => { nb[c.row][c.col] = CELL.SHIP; });
                setBoard(nb);
                const ns = new Set(placed); ns.add(shipId); setPlaced(ns);
                const np = [...placements, originalPlacement];
                setPlacements(np); notifyParent(np);
            }
            setMsg(t('ship.cantPlaceReturn'));
        }
    }, [dragState, canPlace, board, placed, placements, notifyParent, t]);

    /** Handle drag cancel (e.g. system interrupt) — restore ship */
    const handleDragCancel = useCallback(() => {
        if (!dragState) return;
        const { shipId, originalPlacement, direction: dir, length } = dragState;
        setDragState(null);
        gridGeomRef.current = null;
        const { valid, cells } = canPlace(originalPlacement.row, originalPlacement.col, length, dir);
        if (valid) {
            const nb = board.map(r => [...r]);
            cells.forEach(c => { nb[c.row][c.col] = CELL.SHIP; });
            setBoard(nb);
            const ns = new Set(placed); ns.add(shipId); setPlaced(ns);
            const np = [...placements, originalPlacement];
            setPlacements(np); notifyParent(np);
        }
        setMsg('');
    }, [dragState, canPlace, board, placed, placements, notifyParent]);

    return (
        <div className={`space-y-3 ${locked ? 'pointer-events-none' : ''}`}>

            {/* Controls */}
            <div className={`glass-card p-3 sm:p-4 space-y-3 transition-all ${locked ? 'border-green-600/30 opacity-60' : ''}`}>

                {/* Ship selector */}
                <div className="grid grid-cols-5 gap-1.5">
                    {SHIPS.map(ship => {
                        const isDone = placed.has(ship.id);
                        const isActive = selected === ship.id && !isDone;
                        return (
                            <button
                                key={ship.id}
                                onClick={() => {
                                    if (isDone) {
                                        setMsg(t('ship.clickToMove', ship.name));
                                    } else {
                                        setSelected(ship.id); setMsg(t('ship.selected', ship.name, ship.length));
                                    }
                                }}
                                className={`flex flex-col items-center justify-center p-2 sm:p-2.5 rounded-xl transition-all duration-300 text-center
                  ${isDone ? 'bg-emerald-800/70 text-emerald-300 cursor-pointer hover:bg-emerald-700/70 hover:scale-105 ring-2 ring-emerald-600/60 shadow-lg shadow-emerald-900/30' :
                                        isActive ? 'bg-violet-600 text-white ring-2 ring-yellow-400 scale-110 shadow-xl shadow-violet-900/40' :
                                            'bg-slate-700/80 hover:bg-slate-600 hover:scale-105 text-slate-100 shadow-md'}`}
                            >
                                <span className="text-lg sm:text-xl">{isDone ? '✅' : ship.emoji}</span>
                                <span className="text-[0.6rem] sm:text-[0.68rem] font-bold mt-1 leading-none">{ship.name}</span>
                                <span className="text-[0.52rem] sm:text-[0.62rem] text-slate-300 mt-0.5 font-semibold">{ship.length}×</span>
                            </button>
                        );
                    })}
                </div>

                {/* Direction + action buttons */}
                <div className="flex flex-wrap gap-2.5 items-center justify-between">
                    <div className="flex rounded-xl overflow-hidden border border-slate-600 bg-slate-700/80 shadow-sm">
                        {['horizontal', 'vertical'].map(d => (
                            <button
                                key={d}
                                onClick={() => setDirection(d)}
                                className={`px-4 sm:px-5 py-2 text-xs sm:text-sm font-bold transition-all ${direction === d ? 'bg-blue-600 text-white shadow-inner' : 'text-slate-300 hover:bg-slate-600'}`}
                            >
                                {d === 'horizontal' ? t('placement.horiz') : t('placement.vert')}
                            </button>
                        ))}
                    </div>
                    <div className="flex gap-2">
                        <button 
                            onClick={handleRandomize} 
                            disabled={locked} 
                            className="px-4 py-2 bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-400 hover:to-orange-400 disabled:opacity-40 disabled:cursor-not-allowed text-white text-xs sm:text-sm font-bold rounded-xl transition-all hover:scale-105 hover:shadow-lg shadow-amber-900/30 active:scale-95"
                        >
                            {t('placement.random')}
                        </button>
                        <button 
                            onClick={handleClear} 
                            disabled={placed.size === 0 || locked} 
                            className="px-4 py-2 bg-slate-600 hover:bg-slate-500 disabled:opacity-40 disabled:cursor-not-allowed text-white text-xs sm:text-sm font-semibold rounded-xl transition-all hover:scale-105 active:scale-95"
                        >
                            {t('placement.clear')}
                        </button>
                    </div>
                </div>

                {msg && (
                    <p className={`text-xs sm:text-sm text-center rounded-xl px-4 py-2 font-semibold shadow-sm ${msg.startsWith('❌') ? 'bg-red-900/50 text-red-300 border border-red-500/30' :
                        msg.startsWith('✅') || msg.startsWith('🎲') || msg.startsWith('↩️') ? 'bg-emerald-900/50 text-emerald-300 border border-emerald-500/30' :
                            'bg-slate-700/60 text-slate-300 border border-slate-600/40'
                        }`}>{msg}</p>
                )}

                {/* Progress */}
                <div>
                    <div className="flex justify-between text-[0.65rem] text-slate-400 mb-2 font-medium">
                        <span>{t('placement.placed', placed.size)}</span>
                        <span>{t('placement.remaining', 5 - placed.size)}</span>
                    </div>
                    <div className="w-full bg-slate-700/60 rounded-full h-2 shadow-inner">
                        <div className="bg-gradient-to-r from-emerald-500 to-green-400 h-2 rounded-full transition-all duration-500 shadow-lg shadow-emerald-500/40" style={{ width: `${(placed.size / 5) * 100}%` }} />
                    </div>
                </div>
            </div>

            {/* Placement board */}
            <div className={`glass-card p-4 sm:p-5 transition-all duration-300 ${locked ? 'border-green-600/40 shadow-green-900/20 shadow-lg' : ''}`}>
                {locked
                    ? <p className="text-green-300 text-[0.68rem] mb-3 text-center font-bold">{t('placement.lockedHint')}</p>
                    : <p className="text-slate-400 text-[0.68rem] mb-3 text-center font-medium">{t('placement.placeHint')}</p>
                }
                <div
                    ref={gridRef}
                    className="grid w-full"
                    style={{ gridTemplateColumns: 'clamp(14px,3.5vw,26px) repeat(10, 1fr)', gridTemplateRows: 'clamp(14px,3.5vw,26px) repeat(10, 1fr)', aspectRatio: '11/11', ...(dragState ? { touchAction: 'none' } : {}) }}
                >
                    <div />
                    {COL_HEADERS.map((letter, c) => (
                        <div key={c} className="flex items-center justify-center text-blue-300 font-bold text-[clamp(0.45rem,1.1vw,0.65rem)]">
                            {letter}
                        </div>
                    ))}
                    {board.map((row, ri) => (
                        <React.Fragment key={ri}>
                            <div className="flex items-center justify-center text-blue-300 font-bold text-[clamp(0.45rem,1.1vw,0.65rem)]">
                                {ri + 1}
                            </div>
                            {row.map((cell, ci) => {
                                const isShip = cell === CELL.SHIP;
                                const isPrev = !locked && preview.has(`${ri},${ci}`);
                                const isDragPrev = isPrev && !!dragState;
                                const prevClass = isDragPrev
                                    ? (dragIsValid ? 'bg-green-400 scale-105 z-10' : 'bg-red-400/70 scale-105 z-10')
                                    : 'bg-yellow-400 hover:bg-yellow-300 cursor-crosshair scale-105 z-10';
                                return (
                                    <button
                                        key={ci}
                                        type="button"
                                        className={`aspect-square w-full border border-slate-800/40 transition-all duration-75
                      ${isShip ? (locked ? 'bg-emerald-500 cursor-default' : 'bg-emerald-500 hover:bg-red-400 cursor-grab active:cursor-grabbing') :
                                                isPrev ? prevClass :
                                                    locked ? 'bg-blue-500/70 cursor-default' :
                                                        'bg-blue-500/70 hover:bg-blue-400 cursor-crosshair'}
                    `}
                                        title={!locked && isShip ? t('placement.dragHint') : ''}
                                        onClick={() => !locked && !dragState && handleCellClick(ri, ci)}
                                        onPointerDown={(e) => isShip && !locked && handleDragStart(ri, ci, e)}
                                        onMouseEnter={() => !locked && !dragState && setHovered({ row: ri, col: ci })}
                                        onMouseLeave={() => !locked && !dragState && setHovered(null)}
                                    />
                                );
                            })}
                        </React.Fragment>
                    ))}
                </div>
                {/* Transparent overlay captures all pointer events during drag */}
                {dragState && (
                    <div
                        ref={el => { if (el) try { el.setPointerCapture(1); } catch {} }}
                        className="fixed inset-0 z-[9999] cursor-grabbing"
                        style={{ touchAction: 'none' }}
                        onPointerMove={handlePointerMove}
                        onPointerUp={handlePointerUp}
                        onPointerCancel={handleDragCancel}
                        onLostPointerCapture={handleDragCancel}
                    />
                )}
            </div>
        </div>
    );
});

ShipPlacement.displayName = 'ShipPlacement';

export default ShipPlacement;
