package com.anasio.battleships.ui.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Programmatic Java View for the Battleships battle screen.
 * Renders both boards (enemy + player fleet), timers, turn indicator,
 * scoreboard, and action buttons. All styling is done in code — no XML inflation.
 */
public class JavaBattleView extends FrameLayout {

    // ── Constants ──
    private static final int GRID_SIZE = 10;
    private static final int HEADER_SIZE_DP = 18;
    private static final int GAP_DP = 1;

    // ── Callbacks ──
    public interface OnCellClickListener {
        void onCellClick(int row, int col);
    }
    public interface OnSurrenderListener {
        void onSurrender();
    }
    public interface OnBombToggleListener {
        void onBombToggle();
    }
    public interface OnLeaveListener {
        void onLeave();
    }

    private OnCellClickListener cellClickListener;
    private OnSurrenderListener surrenderListener;
    private OnBombToggleListener bombToggleListener;
    private OnLeaveListener leaveListener;

    public void setOnCellClickListener(OnCellClickListener l) { cellClickListener = l; }
    public void setOnSurrenderListener(OnSurrenderListener l) { surrenderListener = l; }
    public void setOnBombToggleListener(OnBombToggleListener l) { bombToggleListener = l; }
    public void setOnLeaveListener(OnLeaveListener l) { leaveListener = l; }

    // ── Color palette (set externally) ──
    private int colorBackground = 0xFF0F172A;
    private int colorSurface = 0xFF1E293B;
    private int colorCard = 0xFF334155;
    private int colorBorder = 0xFF475569;
    private int colorTextPrimary = 0xFFCBD5E1;
    private int colorTextDim = 0xFF94A3B8;
    private int colorPrimary = 0xFF3B82F6;
    private int colorPrimaryDark = 0xFF2563EB;
    private int colorYellow = 0xFFFDE047;
    private int colorGreen = 0xFF22C55E;
    private int colorRed = 0xFFEF4444;
    private int colorOrange = 0xFFF97316;
    private int colorCellWater = 0xFF1E3A5F;
    private int colorCellShip = 0xFF22C55E;
    private int colorCellHit = 0xFFEF4444;
    private int colorCellMiss = 0xFF475569;
    private int colorCellSunk = 0xFF7F1D1D;
    private int colorCellSafe = 0xFF1E293B;

    // ── String labels (set externally for i18n) ──
    private String strYou = "You";
    private String strOpponent = "Opponent";
    private String strYourTurnFire = "Your Turn — Fire!";
    private String strExtraShotHint = "Hit a ship for an extra shot!";
    private String strBomb = "Bomb";
    private String strBombTargetHint = "Select a cell for the bomb attack.";
    private String strEnemyWaters = "{0}'s Waters";
    private String strYourFleet = "Your Fleet";
    private String strSurrender = "Surrender";
    private String strLeave = "Leave";
    private String strYourHits = "Your hits";
    private String strTheirHits = "Their hits";
    private String strNamesTurn = "{0}'s turn";
    private String strSpectatorCount = "spectator(s)";
    private String strBoardWater = "Water";
    private String strBoardShip = "Ship";
    private String strBoardHit = "Hit";
    private String strBoardMiss = "Miss";
    private String strBoardSunk = "Sunk";
    private String strCellHit = "Hit";
    private String strCellMiss = "Miss";
    private String strCellSunk = "Sunk";
    private String strCellShip = "Ship";
    private String strCellWater = "Water";
    private String strCellSafe = "Safe";

    // ── State ──
    private List<List<String>> playerBoard;
    private List<List<String>> opponentBoard;
    private boolean isMyTurn = false;
    private boolean isSpectator = false;
    private boolean bombMode = false;
    private boolean bombUsed = false;
    private boolean interactive = false;
    private boolean spectatorBoardsMode = false;
    private boolean showFleetShips = true;
    private int mySunkCount = 0;
    private int theirSunkCount = 0;
    private int spectatorCount = 0;
    private String turnText = "";
    private int turnColor = 0xFF22C55E;
    private String playerNameStr = "";
    private String opponentNameStr = "";
    private List<String> spectatorPlayerIds;
    private Map<String, String> spectatorPlayerNames = new java.util.HashMap<>();
    private String opponentLastShotKey;
    private String playerLastShotKey;
    private Set<String> opponentExplosionKeys = new HashSet<>();
    private Set<String> playerExplosionKeys = new HashSet<>();

    // Timer state
    private Map<String, Double> playerTimeLeft;
    private Long turnStartedAt;
    private String currentTurnId;
    private String myId;

    // ── UI references ──
    private LinearLayout rootColumn;
    private LinearLayout timerRow;
    private LinearLayout turnPanel;
    private LinearLayout turnMessageColumn;
    private TextView turnIndicator;
    private TextView extraShotHint;
    private TextView bombModeHint;
    private LinearLayout scoreboardRow;
    private LinearLayout enemyBoardSection;
    private LinearLayout fleetBoardSection;
    private LinearLayout enemyBoardPanel;
    private LinearLayout fleetBoardPanel;
    private LinearLayout footerRow;
    private View[][] enemyCells;
    private View[][] fleetCells;
    private TextView[] enemyColHeaders;
    private TextView[] enemyRowHeaders;
    private TextView[] fleetColHeaders;
    private TextView[] fleetRowHeaders;
    private TextView enemyBoardTitle;
    private TextView fleetBoardTitle;
    private LinearLayout enemyGridContainer;
    private LinearLayout fleetGridContainer;

    // Timer views
    private LinearLayout timerPlayerPanel;
    private LinearLayout timerOpponentPanel;
    private TextView timerPlayerLabel;
    private TextView timerPlayerValue;
    private TextView timerOpponentLabel;
    private TextView timerOpponentValue;

    // Scoreboard dots
    private View[] myHitDots;
    private View[] theirHitDots;
    private TextView myHitsLabel;
    private TextView theirHitsLabel;

    // Footer buttons
    private TextView bombButton;
    private TextView surrenderButton;
    private TextView leaveButton;
    private TextView spectatorText;

    // Legend views
    private LinearLayout enemyLegend;
    private LinearLayout fleetLegend;

    // Timer ticking
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            refreshTimerDisplay();
            timerHandler.postDelayed(this, 250);
        }
    };
    private boolean timerRunning = false;

    public JavaBattleView(Context context) {
        super(context);
        init(context);
    }

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private float sp(int sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

    private GradientDrawable roundedRect(int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(fillColor);
        gd.setStroke(dp(1), strokeColor);
        gd.setCornerRadius(dp(radiusDp));
        return gd;
    }

    private GradientDrawable gradientRect(int startColor, int endColor, int strokeColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{startColor, endColor});
        gd.setStroke(dp(1), strokeColor);
        gd.setCornerRadius(dp(radiusDp));
        return gd;
    }

    // ══════════════════════════════════════════
    // INIT — Build entire UI programmatically
    // ══════════════════════════════════════════

    private void init(Context ctx) {
        ScrollView scrollView = new ScrollView(ctx);
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);

        rootColumn = new LinearLayout(ctx);
        rootColumn.setOrientation(LinearLayout.VERTICAL);
        rootColumn.setLayoutParams(new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int pad = dp(20);
        rootColumn.setPadding(pad, dp(8), pad, dp(8));

        // 1. Timer row
        buildTimerRow(ctx);
        rootColumn.addView(timerRow);

        // 2. Turn status and action buttons
        turnPanel = new LinearLayout(ctx);
        turnPanel.setOrientation(LinearLayout.HORIZONTAL);
        turnPanel.setGravity(Gravity.CENTER_VERTICAL);
        turnPanel.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams turnPanelLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        turnPanelLp.topMargin = dp(12);
        turnPanel.setLayoutParams(turnPanelLp);

        turnMessageColumn = new LinearLayout(ctx);
        turnMessageColumn.setOrientation(LinearLayout.VERTICAL);
        turnMessageColumn.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        turnIndicator = new TextView(ctx);
        turnIndicator.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        turnIndicator.setTypeface(null, Typeface.BOLD);
        turnIndicator.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        turnMessageColumn.addView(turnIndicator);

        extraShotHint = new TextView(ctx);
        extraShotHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        extraShotHint.setTextColor(colorTextDim);
        extraShotHint.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(3);
        extraShotHint.setLayoutParams(hintLp);
        turnMessageColumn.addView(extraShotHint);
        turnPanel.addView(turnMessageColumn);

        surrenderButton = new TextView(ctx);
        surrenderButton.setText("🏳️");
        surrenderButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        surrenderButton.setGravity(Gravity.CENTER);
        surrenderButton.setContentDescription(strSurrender);
        surrenderButton.setOnClickListener(v -> {
            if (surrenderListener != null) surrenderListener.onSurrender();
        });
        LinearLayout.LayoutParams surrenderLp = new LinearLayout.LayoutParams(dp(40), dp(38));
        surrenderLp.leftMargin = dp(4);
        surrenderButton.setLayoutParams(surrenderLp);
        turnPanel.addView(surrenderButton);

        bombButton = new TextView(ctx);
        bombButton.setText("💣");
        bombButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        bombButton.setGravity(Gravity.CENTER);
        bombButton.setContentDescription(strBomb);
        bombButton.setOnClickListener(v -> {
            if (bombToggleListener != null) bombToggleListener.onBombToggle();
        });
        LinearLayout.LayoutParams bombActionLp = new LinearLayout.LayoutParams(dp(40), dp(38));
        bombActionLp.leftMargin = dp(6);
        bombButton.setLayoutParams(bombActionLp);
        turnPanel.addView(bombButton);
        rootColumn.addView(turnPanel);

        // 3. Scoreboard
        buildScoreboard(ctx);
        scoreboardRow.setPadding(dp(12), dp(9), dp(12), dp(9));
        LinearLayout.LayoutParams scoreLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scoreLp.topMargin = dp(10);
        scoreboardRow.setLayoutParams(scoreLp);
        rootColumn.addView(scoreboardRow);

        // 5. Enemy board section
        enemyBoardSection = new LinearLayout(ctx);
        enemyBoardSection.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams enemyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        enemyLp.topMargin = dp(8);
        enemyBoardSection.setLayoutParams(enemyLp);

        enemyBoardTitle = new TextView(ctx);
        enemyBoardTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        enemyBoardTitle.setTypeface(null, Typeface.BOLD);
        enemyBoardTitle.setTextColor(colorTextPrimary);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.bottomMargin = dp(4);
        enemyBoardTitle.setLayoutParams(titleLp);
        enemyBoardSection.addView(enemyBoardTitle);

        bombModeHint = new TextView(ctx);
        bombModeHint.setText(strBombTargetHint);
        bombModeHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        bombModeHint.setTextColor(colorOrange);
        bombModeHint.setGravity(Gravity.CENTER);
        bombModeHint.setVisibility(View.GONE);
        enemyBoardSection.addView(bombModeHint);

        // Enemy board panel (bordered container)
        enemyBoardPanel = new LinearLayout(ctx);
        enemyBoardPanel.setOrientation(LinearLayout.VERTICAL);
        enemyBoardPanel.setPadding(dp(8), dp(8), dp(8), dp(8));
        enemyBoardPanel.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        enemyGridContainer = new LinearLayout(ctx);
        enemyGridContainer.setOrientation(LinearLayout.VERTICAL);
        enemyGridContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        enemyGridContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        enemyBoardPanel.addView(enemyGridContainer);

        enemyLegend = new LinearLayout(ctx);
        enemyLegend.setOrientation(LinearLayout.HORIZONTAL);
        enemyLegend.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams legendLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        legendLp.topMargin = dp(4);
        enemyLegend.setLayoutParams(legendLp);
        enemyBoardPanel.addView(enemyLegend);

        enemyBoardSection.addView(enemyBoardPanel);
        rootColumn.addView(enemyBoardSection);

        // 6. Fleet board section
        fleetBoardSection = new LinearLayout(ctx);
        fleetBoardSection.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams fleetLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fleetLp.topMargin = dp(8);
        fleetBoardSection.setLayoutParams(fleetLp);

        fleetBoardTitle = new TextView(ctx);
        fleetBoardTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        fleetBoardTitle.setTypeface(null, Typeface.BOLD);
        fleetBoardTitle.setTextColor(colorTextPrimary);
        LinearLayout.LayoutParams fTitleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fTitleLp.bottomMargin = dp(4);
        fleetBoardTitle.setLayoutParams(fTitleLp);
        fleetBoardSection.addView(fleetBoardTitle);

        fleetBoardPanel = new LinearLayout(ctx);
        fleetBoardPanel.setOrientation(LinearLayout.VERTICAL);
        fleetBoardPanel.setPadding(dp(8), dp(8), dp(8), dp(8));
        fleetBoardPanel.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        fleetGridContainer = new LinearLayout(ctx);
        fleetGridContainer.setOrientation(LinearLayout.VERTICAL);
        fleetGridContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        fleetGridContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        fleetBoardPanel.addView(fleetGridContainer);

        fleetLegend = new LinearLayout(ctx);
        fleetLegend.setOrientation(LinearLayout.HORIZONTAL);
        fleetLegend.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fLegendLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fLegendLp.topMargin = dp(4);
        fleetLegend.setLayoutParams(fLegendLp);
        fleetBoardPanel.addView(fleetLegend);

        fleetBoardSection.addView(fleetBoardPanel);
        rootColumn.addView(fleetBoardSection);

        // 7. Footer row
        buildFooter(ctx);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerLp.topMargin = dp(8);
        footerLp.bottomMargin = dp(12);
        footerRow.setLayoutParams(footerLp);
        rootColumn.addView(footerRow);

        scrollView.addView(rootColumn);
        addView(scrollView);

        // Initialize cell arrays (will be rebuilt on first update)
        enemyCells = new View[GRID_SIZE][GRID_SIZE];
        fleetCells = new View[GRID_SIZE][GRID_SIZE];
        enemyColHeaders = new TextView[GRID_SIZE];
        enemyRowHeaders = new TextView[GRID_SIZE];
        fleetColHeaders = new TextView[GRID_SIZE];
        fleetRowHeaders = new TextView[GRID_SIZE];
    }

    // ══════════════════════════════════════════
    // TIMER ROW
    // ══════════════════════════════════════════

    private void buildTimerRow(Context ctx) {
        timerRow = new LinearLayout(ctx);
        timerRow.setOrientation(LinearLayout.HORIZONTAL);
        timerRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Player timer
        timerPlayerPanel = new LinearLayout(ctx);
        timerPlayerPanel.setOrientation(LinearLayout.VERTICAL);
        timerPlayerPanel.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        timerPlayerPanel.setPadding(dp(4), dp(8), dp(4), dp(8));
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, dp(68), 1f);
        pLp.rightMargin = dp(4);
        timerPlayerPanel.setLayoutParams(pLp);

        timerPlayerLabel = new TextView(ctx);
        timerPlayerLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        timerPlayerLabel.setTextColor(colorTextDim);
        timerPlayerLabel.setTypeface(null, Typeface.BOLD);
        timerPlayerLabel.setGravity(Gravity.CENTER);
        timerPlayerLabel.setMaxLines(1);
        timerPlayerLabel.setEllipsize(TextUtils.TruncateAt.END);
        timerPlayerPanel.addView(timerPlayerLabel);

        timerPlayerValue = new TextView(ctx);
        timerPlayerValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        timerPlayerValue.setTypeface(null, Typeface.BOLD);
        timerPlayerValue.setTextColor(Color.WHITE);
        timerPlayerValue.setGravity(Gravity.CENTER);
        timerPlayerPanel.addView(timerPlayerValue);

        timerRow.addView(timerPlayerPanel);

        // Opponent timer
        timerOpponentPanel = new LinearLayout(ctx);
        timerOpponentPanel.setOrientation(LinearLayout.VERTICAL);
        timerOpponentPanel.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        timerOpponentPanel.setPadding(dp(4), dp(8), dp(4), dp(8));
        LinearLayout.LayoutParams oLp = new LinearLayout.LayoutParams(0, dp(68), 1f);
        oLp.leftMargin = dp(4);
        timerOpponentPanel.setLayoutParams(oLp);

        timerOpponentLabel = new TextView(ctx);
        timerOpponentLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        timerOpponentLabel.setTextColor(colorTextDim);
        timerOpponentLabel.setTypeface(null, Typeface.BOLD);
        timerOpponentLabel.setGravity(Gravity.CENTER);
        timerOpponentLabel.setMaxLines(1);
        timerOpponentLabel.setEllipsize(TextUtils.TruncateAt.END);
        timerOpponentPanel.addView(timerOpponentLabel);

        timerOpponentValue = new TextView(ctx);
        timerOpponentValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        timerOpponentValue.setTypeface(null, Typeface.BOLD);
        timerOpponentValue.setTextColor(colorTextPrimary);
        timerOpponentValue.setGravity(Gravity.CENTER);
        timerOpponentPanel.addView(timerOpponentValue);

        timerRow.addView(timerOpponentPanel);
    }

    // ══════════════════════════════════════════
    // SCOREBOARD
    // ══════════════════════════════════════════

    private void buildScoreboard(Context ctx) {
        scoreboardRow = new LinearLayout(ctx);
        scoreboardRow.setOrientation(LinearLayout.HORIZONTAL);
        scoreboardRow.setGravity(Gravity.CENTER);

        // My hits
        LinearLayout myHitsContainer = new LinearLayout(ctx);
        myHitsContainer.setOrientation(LinearLayout.HORIZONTAL);
        myHitsContainer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams myLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        myHitsContainer.setLayoutParams(myLp);
        myHitsContainer.setGravity(Gravity.CENTER);

        myHitsLabel = new TextView(ctx);
        myHitsLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        myHitsLabel.setTypeface(null, Typeface.BOLD);
        myHitsLabel.setTextColor(colorGreen);
        myHitsContainer.addView(myHitsLabel);

        myHitDots = new View[5];
        for (int i = 0; i < 5; i++) {
            View dot = new View(ctx);
            int size = dp(10);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(size, size);
            dotLp.leftMargin = dp(2);
            dot.setLayoutParams(dotLp);
            myHitDots[i] = dot;
            myHitsContainer.addView(dot);
        }
        scoreboardRow.addView(myHitsContainer);

        TextView divider = new TextView(ctx);
        divider.setText("│");
        divider.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        divider.setTextColor(setAlpha(colorTextDim, 0x4D));
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dividerLp.leftMargin = dp(12);
        dividerLp.rightMargin = dp(12);
        divider.setLayoutParams(dividerLp);
        scoreboardRow.addView(divider);

        // Their hits
        LinearLayout theirHitsContainer = new LinearLayout(ctx);
        theirHitsContainer.setOrientation(LinearLayout.HORIZONTAL);
        theirHitsContainer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams theirLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        theirHitsContainer.setLayoutParams(theirLp);
        theirHitsContainer.setGravity(Gravity.CENTER);

        theirHitsLabel = new TextView(ctx);
        theirHitsLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        theirHitsLabel.setTypeface(null, Typeface.BOLD);
        theirHitsLabel.setTextColor(colorRed);
        theirHitsContainer.addView(theirHitsLabel);

        theirHitDots = new View[5];
        for (int i = 0; i < 5; i++) {
            View dot = new View(ctx);
            int size = dp(10);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(size, size);
            dotLp.leftMargin = dp(2);
            dot.setLayoutParams(dotLp);
            theirHitDots[i] = dot;
            theirHitsContainer.addView(dot);
        }
        scoreboardRow.addView(theirHitsContainer);
    }

    // ══════════════════════════════════════════
    // FOOTER
    // ══════════════════════════════════════════

    private void buildFooter(Context ctx) {
        footerRow = new LinearLayout(ctx);
        footerRow.setOrientation(LinearLayout.HORIZONTAL);
        footerRow.setGravity(Gravity.CENTER_VERTICAL);

        spectatorText = new TextView(ctx);
        spectatorText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        spectatorText.setTextColor(colorTextDim);
        spectatorText.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        footerRow.addView(spectatorText);

        LinearLayout buttonsContainer = new LinearLayout(ctx);
        buttonsContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonsContainer.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        leaveButton = new TextView(ctx);
        leaveButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        leaveButton.setGravity(Gravity.CENTER);
        leaveButton.setPadding(dp(12), dp(10), dp(12), dp(10));
        leaveButton.setOnClickListener(v -> {
            if (leaveListener != null) leaveListener.onLeave();
        });
        leaveButton.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)));
        buttonsContainer.addView(leaveButton);

        footerRow.addView(buttonsContainer);
    }

    // ══════════════════════════════════════════
    // GRID BUILDING
    // ══════════════════════════════════════════

    private void buildGrid(Context ctx, LinearLayout container, View[][] cells,
                           TextView[] colHeaders, TextView[] rowHeaders,
                           boolean isEnemy) {
        container.removeAllViews();

        // Calculate cell size based on available width
        int availableWidth = getWidth() - dp(20) * 2 - dp(8) * 2; // padding from root + panel padding
        if (availableWidth <= 0) availableWidth = dp(320); // fallback
        int headerSizePx = dp(HEADER_SIZE_DP);
        int gapPx = dp(GAP_DP);
        int totalGap = gapPx * (GRID_SIZE + 1);
        int cellSize = Math.max(dp(20), (availableWidth - headerSizePx - totalGap) / GRID_SIZE);

        // Column headers (A-J)
        LinearLayout headerRow = new LinearLayout(ctx);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);

        // Spacer for row header column
        View headerSpacer = new View(ctx);
        headerSpacer.setLayoutParams(new LinearLayout.LayoutParams(headerSizePx, dp(1)));
        headerRow.addView(headerSpacer);

        for (int col = 0; col < GRID_SIZE; col++) {
            TextView colHeader = new TextView(ctx);
            colHeader.setText(String.valueOf((char) ('A' + col)));
            colHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            colHeader.setTextColor(colorTextDim);
            colHeader.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(cellSize, ViewGroup.LayoutParams.WRAP_CONTENT);
            colLp.leftMargin = gapPx;
            colHeader.setLayoutParams(colLp);
            colHeaders[col] = colHeader;
            headerRow.addView(colHeader);
        }
        container.addView(headerRow);

        // Data rows
        for (int r = 0; r < GRID_SIZE; r++) {
            LinearLayout dataRow = new LinearLayout(ctx);
            dataRow.setOrientation(LinearLayout.HORIZONTAL);
            dataRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.topMargin = gapPx;
            dataRow.setLayoutParams(rowLp);

            // Row header (1-10)
            TextView rowHeader = new TextView(ctx);
            rowHeader.setText(String.valueOf(r + 1));
            rowHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            rowHeader.setTextColor(colorTextDim);
            rowHeader.setGravity(Gravity.CENTER);
            rowHeader.setLayoutParams(new LinearLayout.LayoutParams(headerSizePx, cellSize));
            rowHeaders[r] = rowHeader;
            dataRow.addView(rowHeader);

            // Cells
            for (int col = 0; col < GRID_SIZE; col++) {
                TextView cell = new TextView(ctx);
                LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(cellSize, cellSize);
                cellLp.leftMargin = gapPx;
                cell.setLayoutParams(cellLp);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(TypedValue.COMPLEX_UNIT_PX, cellSize * 0.45f);

                final int row = r;
                final int c = col;
                if (isEnemy) {
                    cell.setOnClickListener(v -> {
                        if (interactive && cellClickListener != null) {
                            cellClickListener.onCellClick(row, c);
                        }
                    });
                }

                cells[r][col] = cell;
                dataRow.addView(cell);
            }
            container.addView(dataRow);
        }
    }

    // ══════════════════════════════════════════
    // LEGEND
    // ══════════════════════════════════════════

    private void rebuildLegend(Context ctx, LinearLayout container, boolean showShip) {
        container.removeAllViews();

        addLegendItem(ctx, container, colorCellWater, strBoardWater);
        if (showShip) addLegendItem(ctx, container, colorCellShip, strBoardShip);
        addLegendItem(ctx, container, colorCellHit, strBoardHit);
        addLegendItem(ctx, container, colorCellMiss, strBoardMiss);
        addLegendItem(ctx, container, colorCellSunk, strBoardSunk);
    }

    private void addLegendItem(Context ctx, LinearLayout container, int color, String label) {
        LinearLayout item = new LinearLayout(ctx);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        itemLp.rightMargin = dp(8);
        item.setLayoutParams(itemLp);

        View swatch = new View(ctx);
        int swatchSize = dp(10);
        swatch.setLayoutParams(new LinearLayout.LayoutParams(swatchSize, swatchSize));
        GradientDrawable swatchBg = new GradientDrawable();
        swatchBg.setShape(GradientDrawable.RECTANGLE);
        swatchBg.setColor(color);
        swatchBg.setCornerRadius(dp(2));
        swatch.setBackground(swatchBg);
        item.addView(swatch);

        TextView text = new TextView(ctx);
        text.setText(label);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        text.setTextColor(colorTextDim);
        LinearLayout.LayoutParams txtLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        txtLp.leftMargin = dp(3);
        text.setLayoutParams(txtLp);
        item.addView(text);

        container.addView(item);
    }

    // ══════════════════════════════════════════
    // PUBLIC UPDATE METHODS
    // ══════════════════════════════════════════

    /**
     * Update the color palette from the current theme.
     */
    public void updateColors(int background, int surface, int card, int border,
                             int textPrimary, int textDim, int primary, int primaryDark,
                             int yellow, int green, int red, int orange,
                             int cellWater, int cellShip, int cellHit, int cellMiss,
                             int cellSunk, int cellSafe) {
        this.colorBackground = background;
        this.colorSurface = surface;
        this.colorCard = card;
        this.colorBorder = border;
        this.colorTextPrimary = textPrimary;
        this.colorTextDim = textDim;
        this.colorPrimary = primary;
        this.colorPrimaryDark = primaryDark;
        this.colorYellow = yellow;
        this.colorGreen = green;
        this.colorRed = red;
        this.colorOrange = orange;
        this.colorCellWater = cellWater;
        this.colorCellShip = cellShip;
        this.colorCellHit = cellHit;
        this.colorCellMiss = cellMiss;
        this.colorCellSunk = cellSunk;
        this.colorCellSafe = cellSafe;
        setBackgroundColor(background);
        enemyBoardPanel.setBackground(roundedRect(surface, border, 14));
        fleetBoardPanel.setBackground(roundedRect(surface, border, 14));
        scoreboardRow.setBackground(roundedRect(surface, border, 14));
        timerPlayerPanel.setBackground(gradientRect(card, card, border, 8));
        timerOpponentPanel.setBackground(gradientRect(card, card, border, 8));
        bombModeHint.setTextColor(orange);
    }

    /**
     * Update all localised strings.
     */
    public void updateStrings(String you, String opponent,
                              String yourTurnFire, String extraShotHint,
                              String bomb, String bombTargetHint,
                              String enemyWaters, String yourFleet,
                              String surrender, String leave,
                              String yourHits, String theirHits,
                              String namesTurn, String spectatorCount,
                              String boardWater, String boardShip,
                              String boardHit, String boardMiss, String boardSunk,
                              String cellHit, String cellMiss, String cellSunk,
                              String cellShip, String cellWater, String cellSafe) {
        this.strYou = you;
        this.strOpponent = opponent;
        this.strYourTurnFire = yourTurnFire;
        this.strExtraShotHint = extraShotHint;
        this.strBomb = bomb;
        this.strBombTargetHint = bombTargetHint;
        this.strEnemyWaters = enemyWaters;
        this.strYourFleet = yourFleet;
        this.strSurrender = surrender;
        this.strLeave = leave;
        this.strYourHits = yourHits;
        this.strTheirHits = theirHits;
        this.strNamesTurn = namesTurn;
        this.strSpectatorCount = spectatorCount;
        this.strBoardWater = boardWater;
        this.strBoardShip = boardShip;
        this.strBoardHit = boardHit;
        this.strBoardMiss = boardMiss;
        this.strBoardSunk = boardSunk;
        this.strCellHit = cellHit;
        this.strCellMiss = cellMiss;
        this.strCellSunk = cellSunk;
        this.strCellShip = cellShip;
        this.strCellWater = cellWater;
        this.strCellSafe = cellSafe;
        bombButton.setContentDescription(strBomb);
        bombModeHint.setText(strBombTargetHint);
    }

    /**
     * Update the board data and refresh all cells.
     */
    public void updateBoards(List<List<String>> playerBoard, List<List<String>> opponentBoard,
                             boolean isInteractive) {
        spectatorBoardsMode = false;
        this.playerBoard = playerBoard;
        this.opponentBoard = opponentBoard;
        this.interactive = isInteractive;
        this.showFleetShips = true;
        enemyBoardSection.setVisibility(View.VISIBLE);
        fleetBoardSection.setVisibility(View.VISIBLE);
        enemyBoardTitle.setText("🎯 " + strEnemyWaters.replace("{0}", opponentNameStr));
        fleetBoardTitle.setText(strYourFleet);
        refreshBoards();
    }

    public void updateSpectatorBoards(List<String> playerIds, List<String> names,
                                      List<List<List<String>>> boards) {
        spectatorBoardsMode = true;
        spectatorPlayerIds = playerIds;
        spectatorPlayerNames.clear();
        if (playerIds != null && names != null) {
            for (int i = 0; i < Math.min(playerIds.size(), names.size()); i++) {
                spectatorPlayerNames.put(playerIds.get(i), names.get(i));
            }
        }
        interactive = false;
        showFleetShips = false;
        opponentBoard = boards != null && !boards.isEmpty() ? boards.get(0) : null;
        playerBoard = boards != null && boards.size() > 1 ? boards.get(1) : null;

        boolean hasFirstBoard = opponentBoard != null;
        boolean hasSecondBoard = playerBoard != null;
        enemyBoardSection.setVisibility(hasFirstBoard ? View.VISIBLE : View.GONE);
        fleetBoardSection.setVisibility(hasSecondBoard ? View.VISIBLE : View.GONE);
        enemyBoardTitle.setText(hasFirstBoard && names != null && !names.isEmpty()
                ? names.get(0) : "");
        fleetBoardTitle.setText(hasSecondBoard && names != null && names.size() > 1
                ? names.get(1) : "");
        refreshBoards();
    }

    private void refreshBoards() {
        if (enemyCells[0][0] == null || enemyGridContainer.getChildCount() == 0) {
            buildGrid(getContext(), enemyGridContainer, enemyCells, enemyColHeaders, enemyRowHeaders, true);
            buildGrid(getContext(), fleetGridContainer, fleetCells, fleetColHeaders, fleetRowHeaders, false);
            rebuildLegend(getContext(), enemyLegend, false);
            rebuildLegend(getContext(), fleetLegend, showFleetShips);
        }

        updateBoardCells(opponentBoard, enemyCells, false);
        updateBoardCells(playerBoard, fleetCells, showFleetShips);
    }

    private void updateBoardCells(List<List<String>> board, View[][] cells, boolean showShips) {
        if (board == null || board.size() < GRID_SIZE) return;
        for (int r = 0; r < GRID_SIZE; r++) {
            if (board.get(r) == null || board.get(r).size() < GRID_SIZE) continue;
            for (int c = 0; c < GRID_SIZE; c++) {
                updateCellView((TextView) cells[r][c], board.get(r).get(c), showShips);
            }
        }
    }

    private void updateCellView(TextView cell, String state, boolean showShips) {
        int bgColor;
        int borderClr;
        String emoji;
        String accessLabel;

        switch (state) {
            case "H": // HIT
                bgColor = colorCellHit;
                borderClr = setAlpha(colorBorder, 0x4D);
                emoji = "🔥";
                accessLabel = strCellHit;
                break;
            case "M": // MISS
                bgColor = colorCellMiss;
                borderClr = setAlpha(colorBorder, 0x4D);
                emoji = "·";
                accessLabel = strCellMiss;
                break;
            case "X": // SUNK
                bgColor = colorCellSunk;
                borderClr = setAlpha(colorRed, 0x99);
                emoji = "💀";
                accessLabel = strCellSunk;
                break;
            case "Z": // SAFE
                bgColor = colorCellSafe;
                borderClr = setAlpha(colorBorder, 0x4D);
                emoji = "";
                accessLabel = strCellSafe;
                break;
            case "S": // SHIP
                if (showShips) {
                    bgColor = colorCellShip;
                    borderClr = setAlpha(colorBorder, 0x4D);
                    emoji = "⚓";
                    accessLabel = strCellShip;
                } else {
                    bgColor = colorCellWater;
                    borderClr = setAlpha(colorBorder, 0x4D);
                    emoji = "";
                    accessLabel = strCellWater;
                }
                break;
            default: // WATER
                bgColor = colorCellWater;
                borderClr = setAlpha(colorBorder, 0x4D);
                emoji = "";
                accessLabel = strCellWater;
                break;
        }

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(bgColor);
        bg.setStroke(dp(1) / 2, borderClr);
        bg.setCornerRadius(dp(3));
        cell.setBackground(bg);
        cell.setText(emoji);
        cell.setContentDescription(accessLabel);
    }

    private int setAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    /**
     * Update turn info.
     */
    public void updateTurn(boolean isMyTurn, String turnText, int turnColor,
                           boolean isSpectator, String opponentName, String playerName) {
        this.isMyTurn = isMyTurn;
        this.turnText = turnText;
        this.turnColor = turnColor;
        this.isSpectator = isSpectator;
        this.opponentNameStr = opponentName;
        this.playerNameStr = playerName;

        turnIndicator.setText(turnText);
        turnIndicator.setTextColor(turnColor);
        GradientDrawable turnBg = roundedRect(setAlpha(turnColor, 0x1A), setAlpha(turnColor, 0x4D), 14);
        turnPanel.setBackground(turnBg);

        extraShotHint.setText(strExtraShotHint);
        extraShotHint.setTextColor(isMyTurn ? setAlpha(colorGreen, 0x99) : colorTextDim);
        extraShotHint.setAlpha(isMyTurn ? 1f : 0f);

        // Board titles
        String enemyLabel = "🎯 " + strEnemyWaters.replace("{0}", opponentName);
        enemyBoardTitle.setText(enemyLabel);
        fleetBoardTitle.setText(strYourFleet);
        bombModeHint.setVisibility(bombMode ? View.VISIBLE : View.GONE);

        // Visibility
        scoreboardRow.setVisibility(isSpectator ? View.GONE : View.VISIBLE);
        bombButton.setVisibility(isSpectator ? View.GONE : View.VISIBLE);
        surrenderButton.setVisibility(isSpectator ? View.GONE : View.VISIBLE);
        leaveButton.setVisibility(isSpectator ? View.VISIBLE : View.GONE);
    }

    /**
     * Update timers.
     */
    public void updateTimers(Map<String, Double> timeLeft, Long turnStartedAt,
                             String currentTurn, String myId) {
        this.playerTimeLeft = timeLeft;
        this.turnStartedAt = turnStartedAt;
        this.currentTurnId = currentTurn;
        this.myId = myId;

        boolean hasTimers = timeLeft != null && !timeLeft.isEmpty();
        timerRow.setVisibility(hasTimers ? View.VISIBLE : View.GONE);

        if (hasTimers) {
            refreshTimerDisplay();
            startTimerTick();
        } else {
            stopTimerTick();
        }
    }

    private void refreshTimerDisplay() {
        if (playerTimeLeft == null || playerTimeLeft.isEmpty()) return;

        String[] ids = playerTimeLeft.keySet().toArray(new String[0]);
        // Order: my timer first
        String firstId = null, secondId = null;
        if (spectatorBoardsMode && spectatorPlayerIds != null) {
            for (String id : spectatorPlayerIds) {
                if (playerTimeLeft.containsKey(id)) {
                    if (firstId == null) firstId = id;
                    else if (!id.equals(firstId)) secondId = id;
                }
            }
        } else if (!isSpectator && myId != null) {
            for (String id : ids) {
                if (id.equals(myId)) firstId = id;
                else secondId = id;
            }
        }
        if (firstId == null && ids.length > 0) firstId = ids[0];
        if (secondId == null && ids.length > 1) secondId = ids[1];

        updateTimerPanel(timerPlayerPanel, timerPlayerLabel, timerPlayerValue,
                firstId, firstId != null && firstId.equals(myId));
        updateTimerPanel(timerOpponentPanel, timerOpponentLabel, timerOpponentValue,
                secondId, false);
    }

        private void updateTimerPanel(LinearLayout panel, TextView label, TextView value,
                      String playerId, boolean isMe) {
        if (playerId == null || playerTimeLeft == null) return;

        boolean isActive = playerId.equals(currentTurnId);
        Double stored = playerTimeLeft.get(playerId);
        double storedVal = stored != null ? stored : 0.0;

        double live;
        if (isActive && turnStartedAt != null && turnStartedAt > 0) {
            live = Math.max(0.0, storedVal - (System.currentTimeMillis() - turnStartedAt) / 1000.0);
        } else {
            live = storedVal;
        }

        boolean isCritical = isActive && live <= 10;
        boolean isLow = isActive && live <= 30;

        // Label
        String labelText;
        if (spectatorBoardsMode && spectatorPlayerNames.containsKey(playerId)) {
            labelText = spectatorPlayerNames.get(playerId);
        } else {
            labelText = isMe ? (playerNameStr.isEmpty() ? strYou : playerNameStr)
                    : (opponentNameStr.isEmpty() ? strOpponent : opponentNameStr);
        }
        label.setText(labelText);

        // Time value
        value.setText(formatTime(live));

        // Background
        int startColor, endColor, borderColor;
        if (isCritical) {
            startColor = 0xFFB91C1C; endColor = 0xFF991B1B; borderColor = colorRed;
        } else if (isLow) {
            startColor = 0xFFD97706; endColor = 0xFFB45309; borderColor = colorOrange;
        } else if (isActive) {
            startColor = colorPrimaryDark; endColor = colorPrimary; borderColor = colorPrimary;
        } else {
            startColor = colorCard; endColor = colorCard; borderColor = colorBorder;
        }
        panel.setBackground(gradientRect(startColor, endColor, borderColor, 8));

        // Value color
        if (isCritical) value.setTextColor(colorYellow);
        else if (isActive) value.setTextColor(Color.WHITE);
        else value.setTextColor(colorTextPrimary);

    }

    private String formatTime(double secs) {
        int s = Math.max(0, (int) Math.ceil(secs));
        return s / 60 + ":" + String.format("%02d", s % 60);
    }

    private void startTimerTick() {
        if (!timerRunning) {
            timerRunning = true;
            timerHandler.postDelayed(timerTick, 250);
        }
    }

    private void stopTimerTick() {
        timerRunning = false;
        timerHandler.removeCallbacks(timerTick);
    }

    /**
     * Update sunk scoreboard dots.
     */
    public void updateScoreboard(int mySunk, int theirSunk) {
        this.mySunkCount = mySunk;
        this.theirSunkCount = theirSunk;

        myHitsLabel.setText(strYourHits);
        myHitsLabel.setTextColor(colorGreen);
        theirHitsLabel.setText(strTheirHits);
        theirHitsLabel.setTextColor(colorRed);

        for (int i = 0; i < 5; i++) {
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            if (i < mySunk) {
                dotBg.setColor(colorGreen);
                dotBg.setStroke(dp(1) / 2, colorGreen);
            } else {
                dotBg.setColor(colorCard);
                dotBg.setStroke(dp(1) / 2, colorBorder);
            }
            myHitDots[i].setBackground(dotBg);
        }

        for (int i = 0; i < 5; i++) {
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            if (i < theirSunk) {
                dotBg.setColor(colorRed);
                dotBg.setStroke(dp(1) / 2, colorRed);
            } else {
                dotBg.setColor(colorCard);
                dotBg.setStroke(dp(1) / 2, colorBorder);
            }
            theirHitDots[i].setBackground(dotBg);
        }
    }

    /**
     * Update bomb button state.
     */
    public void updateBombState(boolean bombUsed, boolean bombMode, boolean isMyTurn) {
        this.bombUsed = bombUsed;
        this.bombMode = bombMode;

        int bombColor;
        if (bombMode) bombColor = colorOrange;
        else if (bombUsed || !isMyTurn) bombColor = colorTextDim;
        else bombColor = colorTextPrimary;

        bombButton.setText("💣");
        bombButton.setTextColor(bombColor);
        bombButton.setEnabled(isMyTurn && !bombUsed);
        bombButton.setBackground(roundedRect(setAlpha(colorCard, 0x33), setAlpha(bombColor, 0x80), 12));

        surrenderButton.setText("🏳️");
        surrenderButton.setContentDescription(strSurrender);
        surrenderButton.setTextColor(colorRed);
        surrenderButton.setBackground(roundedRect(setAlpha(colorCard, 0x33), setAlpha(colorBorder, 0x80), 12));

        leaveButton.setText("← " + strLeave);
        leaveButton.setTextColor(colorTextPrimary);
        leaveButton.setBackground(roundedRect(0x00000000, setAlpha(colorBorder, 0x80), 8));

        bombModeHint.setVisibility(bombMode ? View.VISIBLE : View.GONE);
    }

    /**
     * Update spectator count display.
     */
    public void updateSpectatorCount(int count) {
        this.spectatorCount = count;
        if (count > 0) {
            spectatorText.setText("👁 " + count + " " + strSpectatorCount);
            spectatorText.setVisibility(View.VISIBLE);
        } else {
            spectatorText.setVisibility(View.INVISIBLE);
        }
    }

    public void updateShotEffects(String opponentLastShotKey, String playerLastShotKey,
                                  Set<String> opponentExplosionKeys, Set<String> playerExplosionKeys) {
        if (opponentLastShotKey != null && !opponentLastShotKey.equals(this.opponentLastShotKey)) {
            animateCell(enemyCells, opponentLastShotKey, false);
        }
        if (playerLastShotKey != null && !playerLastShotKey.equals(this.playerLastShotKey)) {
            animateCell(fleetCells, playerLastShotKey, false);
        }
        animateNewExplosions(enemyCells, this.opponentExplosionKeys, opponentExplosionKeys);
        animateNewExplosions(fleetCells, this.playerExplosionKeys, playerExplosionKeys);
        this.opponentLastShotKey = opponentLastShotKey;
        this.playerLastShotKey = playerLastShotKey;
        this.opponentExplosionKeys = opponentExplosionKeys == null
                ? new HashSet<>() : new HashSet<>(opponentExplosionKeys);
        this.playerExplosionKeys = playerExplosionKeys == null
                ? new HashSet<>() : new HashSet<>(playerExplosionKeys);
    }

    private void animateNewExplosions(View[][] cells, Set<String> previous, Set<String> current) {
        if (current == null) return;
        for (String key : current) {
            if (!previous.contains(key)) animateCell(cells, key, true);
        }
    }

    private void animateCell(View[][] cells, String key, boolean explosion) {
        String[] coordinates = key.split(",");
        if (coordinates.length != 2) return;
        try {
            int row = Integer.parseInt(coordinates[0]);
            int col = Integer.parseInt(coordinates[1]);
            if (row < 0 || row >= GRID_SIZE || col < 0 || col >= GRID_SIZE || cells[row][col] == null) return;
            View cell = cells[row][col];
            cell.animate().cancel();
            cell.setScaleX(1f);
            cell.setScaleY(1f);
            cell.setAlpha(1f);
            if (explosion) {
                cell.animate().alpha(0.45f).setDuration(100).withEndAction(() ->
                        cell.animate().alpha(1f).setDuration(350).start()).start();
            } else {
                cell.animate().scaleX(1.18f).scaleY(1.18f).setDuration(110).withEndAction(() ->
                        cell.animate().scaleX(1f).scaleY(1f).setDuration(160).start()).start();
            }
        } catch (NumberFormatException ignored) {
        }
    }

    /**
     * Called when the view is about to be detached.
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopTimerTick();
    }

    /**
     * Request a grid rebuild when size changes (e.g. rotation, though app is portrait-locked).
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && oldw != w) {
            // Rebuild grids with new cell sizes
            post(() -> {
                buildGrid(getContext(), enemyGridContainer, enemyCells, enemyColHeaders, enemyRowHeaders, true);
                buildGrid(getContext(), fleetGridContainer, fleetCells, fleetColHeaders, fleetRowHeaders, false);
                rebuildLegend(getContext(), enemyLegend, false);
                rebuildLegend(getContext(), fleetLegend, true);
                refreshBoards();
            });
        }
    }
}
