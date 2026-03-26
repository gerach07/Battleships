import React, { memo, useCallback, useMemo, useState, useRef, useEffect } from 'react';
import BoardCell from './BoardCell';
import { GRID_SIZE } from '../constants';
import { useI18n } from '../i18n/I18nContext';

// Module-level constant to avoid re-creating on each render
const COL_HEADERS = Array.from({ length: GRID_SIZE }, (_, i) => String.fromCharCode(65 + i));
const NOOP = () => {};

const GameBoard = memo(({ board, isYourBoard, onCellClick, isYourTurn = false, label, explosionCells = [], lastShot = null }) => {
    const { t } = useI18n();
    const [focusRow, setFocusRow] = useState(0);
    const [focusCol, setFocusCol] = useState(0);
    const [kbActive, setKbActive] = useState(false);
    const focusRef = useRef({ row: 0, col: 0 });

    useEffect(() => { focusRef.current = { row: focusRow, col: focusCol }; }, [focusRow, focusCol]);

    const canKeyboard = !isYourBoard && isYourTurn;

    const handleKeyDown = useCallback((e) => {
        if (!canKeyboard) return;
        switch (e.key) {
            case 'ArrowUp': e.preventDefault(); setFocusRow(r => Math.max(0, r - 1)); setKbActive(true); break;
            case 'ArrowDown': e.preventDefault(); setFocusRow(r => Math.min(GRID_SIZE - 1, r + 1)); setKbActive(true); break;
            case 'ArrowLeft': e.preventDefault(); setFocusCol(c => Math.max(0, c - 1)); setKbActive(true); break;
            case 'ArrowRight': e.preventDefault(); setFocusCol(c => Math.min(GRID_SIZE - 1, c + 1)); setKbActive(true); break;
            case 'Enter': case ' ':
                e.preventDefault();
                onCellClick(focusRef.current.row, focusRef.current.col);
                break;
            default: break;
        }
    }, [canKeyboard, onCellClick]);

    // Create a set of explosion keys for quick lookup
    const explosionSet = useMemo(() => {
        const set = new Set();
        explosionCells.forEach(e => set.add(`${e.row},${e.col}`));
        return set;
    }, [explosionCells]);

    return (
        <div className="w-full space-y-2">
            {label && <h3 className="font-bold text-sm sm:text-base text-white">{label}</h3>}
            <div className="w-full">
                <div
                    className="grid w-full"
                    style={{ gridTemplateColumns: 'clamp(14px,3.5vw,26px) repeat(10, 1fr)', gridTemplateRows: 'clamp(14px,3.5vw,26px) repeat(10, 1fr)', aspectRatio: '11/11' }}
                    tabIndex={canKeyboard ? 0 : -1}
                    onKeyDown={handleKeyDown}
                    onBlur={() => setKbActive(false)}
                    role={canKeyboard ? 'grid' : undefined}
                    aria-label={label}
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
                            {row.map((cell, ci) => (
                                <BoardCell
                                    key={ci}
                                    row={ri}
                                    col={ci}
                                    cell={cell}
                                    isYourBoard={isYourBoard}
                                    isYourTurn={isYourTurn}
                                    onCellClick={onCellClick || NOOP}
                                    showExplosion={explosionSet.has(`${ri},${ci}`)}
                                    isFocused={kbActive && canKeyboard && focusRow === ri && focusCol === ci}
                                    isLastShot={lastShot && lastShot.row === ri && lastShot.col === ci}
                                />
                            ))}
                        </React.Fragment>
                    ))}
                </div>
            </div>
            <div className="flex flex-wrap gap-x-3 gap-y-1 text-[0.65rem] text-slate-400">
                <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-sm bg-blue-500/70 inline-block" /> {t('board.water')}</span>
                {isYourBoard && <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-sm bg-emerald-500 inline-block" /> {t('board.ship')}</span>}
                <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-sm bg-orange-500 inline-block" /> {t('board.hit')}</span>
                <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-sm bg-slate-500/70 inline-block" /> {t('board.miss')}</span>
                <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-sm bg-red-800 inline-block" /> {t('board.sunk')}</span>
            </div>
        </div>
    );
});

GameBoard.displayName = 'GameBoard';

export default GameBoard;
