; ─────────────────────────────────────────────────────────────────────────────
; AB Battleships — Custom NSIS Installer Script
; Included by electron-builder via "nsis.include" in package.json.
; ─────────────────────────────────────────────────────────────────────────────

; Called after files are installed
!macro customInstall
  ; Register app with Windows (for "Apps & Features" in Settings)
  WriteRegStr HKLM "Software\ABBattleships" "InstallPath" "$INSTDIR"
  WriteRegStr HKLM "Software\ABBattleships" "Version" "${VERSION}"

  ; Register as a mailto-like protocol handler: abbattleships://
  WriteRegStr HKCR "abbattleships" "" "URL:AB Battleships Protocol"
  WriteRegStr HKCR "abbattleships" "URL Protocol" ""
  WriteRegStr HKCR "abbattleships\DefaultIcon" "" "$INSTDIR\${APP_EXECUTABLE_FILENAME},0"
  WriteRegStr HKCR "abbattleships\shell\open\command" "" '"$INSTDIR\${APP_EXECUTABLE_FILENAME}" "%1"'
!macroend

; Called after application is uninstalled
!macro customUninstall
  ; Clean registry entries
  DeleteRegKey HKLM "Software\ABBattleships"
  DeleteRegKey HKCR "abbattleships"
!macroend
