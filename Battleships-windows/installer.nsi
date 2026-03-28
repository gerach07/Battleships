; ═══════════════════════════════════════════════════════════════════════════
; AB Battleships — NSIS Windows Installer Script
; Build with: makensis or the package-nsis.sh script
; ═══════════════════════════════════════════════════════════════════════════

Unicode True

!define APP_NAME         "AB Battleships"
!define APP_EXE          "AB Battleships.exe"
!define APP_ID           "ABBattleships"
!define APP_VERSION      "1.0.0"
!define APP_PUBLISHER    "AB Battleships"
!define APP_URL          "https://abbattleships.web.app"
!define INSTALL_DIR      "$PROGRAMFILES64\${APP_NAME}"
!define REG_UNINST_KEY   "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_ID}"
!define REG_APP_KEY      "Software\${APP_ID}"
!define UNINSTALLER_EXE  "Uninstall ${APP_NAME}.exe"

; ── Include standard NSIS libraries ─────────────────────────────────────────
!include "MUI2.nsh"
!include "FileFunc.nsh"
!include "LogicLib.nsh"
!include "WinVer.nsh"
!include "x64.nsh"

; ── Installer metadata ───────────────────────────────────────────────────────
Name                "${APP_NAME}"
OutFile             "dist/ABBattleships-Setup-${APP_VERSION}.exe"
InstallDir          "${INSTALL_DIR}"
InstallDirRegKey    HKLM "${REG_APP_KEY}" "InstallPath"
RequestExecutionLevel admin
SetCompressor       /SOLID lzma
SetCompressorDictSize 32

; ── MUI Settings ─────────────────────────────────────────────────────────────
!define MUI_ABORTWARNING
!define MUI_ICON              "assets/icon.ico"
!define MUI_UNICON            "assets/icon.ico"
!define MUI_WELCOMEPAGE_TITLE "Welcome to ${APP_NAME} Setup"
!define MUI_WELCOMEPAGE_TEXT  "This installer will install ${APP_NAME} version ${APP_VERSION} on your computer.$\n$\nIt is recommended that you close all other applications before starting setup.$\n$\nClick Next to continue."
!define MUI_FINISHPAGE_RUN         "$INSTDIR\${APP_EXE}"
!define MUI_FINISHPAGE_RUN_TEXT    "Launch ${APP_NAME} now"
!define MUI_FINISHPAGE_SHOWREADME  ""

; ── Installer Pages ──────────────────────────────────────────────────────────
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "resources/LICENSE.rtf"
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

; ── Uninstaller Pages ────────────────────────────────────────────────────────
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

; ── Language ─────────────────────────────────────────────────────────────────
!insertmacro MUI_LANGUAGE "English"

; ═══════════════════════════════════════════════════════════════════════════
; INSTALLER SECTIONS
; ═══════════════════════════════════════════════════════════════════════════

Section "AB Battleships (required)" SecMain
  SectionIn RO   ; read-only (required)

  ; Set output path
  SetOutPath "$INSTDIR"

  ; Copy all files from win-unpacked
  File /r "dist/win-unpacked/*.*"

  ; ── Registry ─────────────────────────────────────────────────────────────

  ; App registration
  WriteRegStr   HKLM "${REG_APP_KEY}" "InstallPath"  "$INSTDIR"
  WriteRegStr   HKLM "${REG_APP_KEY}" "Version"      "${APP_VERSION}"

  ; Add/Remove Programs entry
  WriteRegStr   HKLM "${REG_UNINST_KEY}" "DisplayName"          "${APP_NAME}"
  WriteRegStr   HKLM "${REG_UNINST_KEY}" "DisplayVersion"       "${APP_VERSION}"
  WriteRegStr   HKLM "${REG_UNINST_KEY}" "Publisher"            "${APP_PUBLISHER}"
  WriteRegStr   HKLM "${REG_UNINST_KEY}" "URLInfoAbout"         "${APP_URL}"
  WriteRegStr   HKLM "${REG_UNINST_KEY}" "InstallLocation"      "$INSTDIR"
  WriteRegStr   HKLM "${REG_UNINST_KEY}" "UninstallString"      '"$INSTDIR\${UNINSTALLER_EXE}"'
  WriteRegStr   HKLM "${REG_UNINST_KEY}" "QuietUninstallString" '"$INSTDIR\${UNINSTALLER_EXE}" /S'
  WriteRegDWORD HKLM "${REG_UNINST_KEY}" "NoModify"             1
  WriteRegDWORD HKLM "${REG_UNINST_KEY}" "NoRepair"             1

  ; Calculate installed size
  ${GetSize} "$INSTDIR" "/S=0K" $0 $1 $2
  IntFmt $0 "0x%08X" $0
  WriteRegDWORD HKLM "${REG_UNINST_KEY}" "EstimatedSize" "$0"

  ; ── Protocol handler: abbattleships:// ──────────────────────────────────
  WriteRegStr HKCR "${APP_ID}"                        ""                     "URL:${APP_NAME}"
  WriteRegStr HKCR "${APP_ID}"                        "URL Protocol"         ""
  WriteRegStr HKCR "${APP_ID}\DefaultIcon"            ""                     "$INSTDIR\${APP_EXE},0"
  WriteRegStr HKCR "${APP_ID}\shell\open\command"     ""                     '"$INSTDIR\${APP_EXE}" "%1"'

  ; ── Write uninstaller ───────────────────────────────────────────────────
  WriteUninstaller "$INSTDIR\${UNINSTALLER_EXE}"

SectionEnd

; ─────────────────────────────────────────────────────────────────────────────
Section "Start Menu Shortcut" SecStartMenu
  CreateDirectory "$SMPROGRAMS\${APP_NAME}"
  CreateShortcut  "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk" \
                  "$INSTDIR\${APP_EXE}" "" "$INSTDIR\${APP_EXE}" 0
  CreateShortcut  "$SMPROGRAMS\${APP_NAME}\Uninstall ${APP_NAME}.lnk" \
                  "$INSTDIR\${UNINSTALLER_EXE}"
SectionEnd

; ─────────────────────────────────────────────────────────────────────────────
Section "Desktop Shortcut" SecDesktop
  CreateShortcut "$DESKTOP\${APP_NAME}.lnk" "$INSTDIR\${APP_EXE}" "" "$INSTDIR\${APP_EXE}" 0
SectionEnd

; ═══════════════════════════════════════════════════════════════════════════
; UNINSTALLER SECTION
; ═══════════════════════════════════════════════════════════════════════════

Section "Uninstall"
  ; Remove application files
  RMDir /r "$INSTDIR"

  ; Remove Start Menu
  RMDir /r "$SMPROGRAMS\${APP_NAME}"

  ; Remove Desktop shortcut
  Delete "$DESKTOP\${APP_NAME}.lnk"

  ; Remove registry
  DeleteRegKey HKLM "${REG_UNINST_KEY}"
  DeleteRegKey HKLM "${REG_APP_KEY}"
  DeleteRegKey HKCR "${APP_ID}"

SectionEnd
