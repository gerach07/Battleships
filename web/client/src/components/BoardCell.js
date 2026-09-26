import React, { memo, useCallback } from 'react';
import { CELL } from '../constants';

const BoardCell = memo(({ row, col, cell, isYourBoard, isYourTurn, onCellClick, showExplosion, isFocused, isLastShot, isBombHighlighted = false, bombMode = false, onHover }) => {
    const clickable =
        !isYourBoard &&
        isYourTurn &&
        cell !== CELL.HIT &&
        cell !== CELL.MISS &&
        cell !== CELL.SUNK &&
        cell !== CELL.SAFE;

    const handleClick = useCallback(() => {
        if (clickable) onCellClick(row, col);
    }, [row, col, clickable, onCellClick]);

    const handleMouseEnter = useCallback(() => {
        if (onHover) onHover(row, col);
    }, [row, col, onHover]);

    let bg, extraClass = '', label;
    switch (cell) {
        case CELL.SHIP:
            bg = isYourBoard
                ? 'bg-gradient-to-br from-emerald-400/90 to-emerald-700'
                : 'bg-gradient-to-br from-blue-500/80 to-blue-800/90';
            label = isYourBoard ? '⚓' : null;
            break;
        case CELL.HIT:
            bg = 'bg-gradient-to-br from-orange-400 to-red-600';
            extraClass = 'cell-glow-hit';
            label = '🔥';
            break;
        case CELL.MISS:
            bg = 'bg-gradient-to-br from-slate-600/60 to-slate-800/80';
            break;
        case CELL.SUNK:
            bg = 'bg-gradient-to-br from-red-900 to-slate-900';
            extraClass = 'cell-glow-sunk';
            label = '💀';
            break;
        case CELL.SAFE:
            bg = 'bg-slate-800/60';
            break;
        default:
            bg = isYourBoard
                ? 'bg-gradient-to-br from-blue-900/60 to-slate-800/70'
                : 'bg-gradient-to-br from-blue-500/80 to-blue-800/90';
            break;
    }

    return (
        <button
            type="button"
            onClick={handleClick}
            onMouseEnter={handleMouseEnter}
            disabled={!clickable}
            className={`
        aspect-square w-full flex items-center justify-center relative overflow-hidden
        border border-slate-700/50 transition-all duration-75 select-none
        text-[clamp(1rem,4vw,2rem)]
        ${bg}
        ${extraClass}
        ${clickable ? (bombMode ? 'cursor-crosshair cell-target-hover hover:scale-110 hover:z-10 hover:border-amber-400/60' : 'cursor-crosshair cell-target-hover hover:scale-110 hover:z-10 hover:border-cyan-400/60') : 'cursor-default'}
        ${cell === CELL.MISS ? 'after:content-["·"] after:text-slate-400 after:font-black after:text-2xl' : ''}
        ${isFocused ? 'cell-focus-ring' : ''}
        ${isLastShot ? 'animate-shot-pop' : ''}
        ${isBombHighlighted ? 'ring-2 ring-amber-400/80 z-10 scale-110 bg-gradient-to-br from-amber-500/40 to-orange-500/30 border-amber-400/60' : ''}
      `}
            aria-label={`${String.fromCharCode(65 + col)}${row + 1}`}
        >
            {showExplosion && (
                <img
                    src="/assets/ship-sink-explosion.webp"
                    alt=""
                    className="absolute inset-0 w-full h-full object-cover z-10 pointer-events-none"
                />
            )}
            {isBombHighlighted && !label ? '💣' : label}
        </button>
    );
}, (prev, next) =>
    prev.cell === next.cell &&
    prev.isYourBoard === next.isYourBoard &&
    prev.isYourTurn === next.isYourTurn &&
    prev.onCellClick === next.onCellClick &&
    prev.showExplosion === next.showExplosion &&
    prev.isFocused === next.isFocused &&
    prev.isLastShot === next.isLastShot &&
    prev.isBombHighlighted === next.isBombHighlighted &&
    prev.bombMode === next.bombMode &&
    prev.onHover === next.onHover
);

BoardCell.displayName = 'BoardCell';

export default BoardCell;
