package com.never.localmusic.desktop

import java.io.File

/**
 * Thin entry point for the shared desktop player shell.
 *
 * The shared shell calls this to ask the desktop app for a folder chosen
 * through the platform file picker. Keeping this separate from
 * [DesktopLocalPlayer] keeps the desktop player module from depending on
 * the desktop UI module.
 */
object DesktopMain {
    // Replaced during compose app startup; see DesktopLocalPlayer.openMusicFolder.
    var chooseFolder: (() -> File?)? = null

    /** Pick a settings.json to import; null when cancelled. */
    var chooseSettingsFileToOpen: (() -> File?)? = null

    /** Pick where to write settings.json; [defaultName] seeds the dialog. */
    var chooseSettingsFileToSave: ((String) -> File?)? = null
}
