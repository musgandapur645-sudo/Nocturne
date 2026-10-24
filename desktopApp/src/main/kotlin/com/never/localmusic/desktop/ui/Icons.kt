package com.never.localmusic.desktop.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object PlayerIcons {
    val Home: ImageVector = ImageVector.Builder("Home", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(10f, 20f); verticalLineToRelative(-6f); horizontalLineToRelative(4f); verticalLineToRelative(6f)
            horizontalLineToRelative(5f); verticalLineToRelative(-8f); horizontalLineToRelative(3f); lineTo(12f, 3f)
            lineTo(2f, 12f); horizontalLineToRelative(3f); verticalLineToRelative(8f); close()
        }
    }.build()

    val Play: ImageVector = ImageVector.Builder("Play", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(8f, 5f); verticalLineToRelative(14f); lineToRelative(11f, -7f); close()
        }
    }.build()

    val Pause: ImageVector = ImageVector.Builder("Pause", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 19f); horizontalLineToRelative(4f); verticalLineTo(5f); horizontalLineTo(6f); verticalLineToRelative(14f); close()
            moveTo(14f, 5f); verticalLineToRelative(14f); horizontalLineToRelative(4f); verticalLineTo(5f); horizontalLineToRelative(-4f); close()
        }
    }.build()

    val SkipNext: ImageVector = ImageVector.Builder("SkipNext", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 18f); lineToRelative(8.5f, -6f); lineTo(6f, 6f); verticalLineToRelative(12f); close()
            moveTo(16f, 6f); verticalLineToRelative(12f); horizontalLineToRelative(2f); verticalLineTo(6f); horizontalLineToRelative(-2f); close()
        }
    }.build()

    val SkipPrev: ImageVector = ImageVector.Builder("SkipPrev", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 6f); horizontalLineToRelative(2f); verticalLineToRelative(12f); horizontalLineTo(6f); close()
            moveTo(9.5f, 12f); lineToRelative(8.5f, 6f); verticalLineTo(6f); close()
        }
    }.build()

    val Shuffle: ImageVector = ImageVector.Builder("Shuffle", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(10.59f, 9.17f); lineTo(5.41f, 4f); lineTo(4f, 5.41f); lineToRelative(5.17f, 5.17f); lineToRelative(1.42f, -1.41f); close()
            moveTo(14.5f, 4f); lineToRelative(2.04f, 2.04f); lineTo(4f, 18.59f); lineTo(5.41f, 20f); lineTo(17.96f, 7.46f); lineTo(20f, 9.5f); verticalLineTo(4f); horizontalLineToRelative(-5.5f); close()
            moveTo(14.83f, 13.41f); lineToRelative(-1.41f, 1.41f); lineToRelative(3.13f, 3.13f); lineTo(14.5f, 20f); horizontalLineTo(20f); verticalLineToRelative(-5.5f); lineToRelative(-2.04f, 2.04f); lineToRelative(-3.13f, -3.13f); close()
        }
    }.build()

    val Repeat: ImageVector = ImageVector.Builder("Repeat", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(7f, 7f); horizontalLineToRelative(10f); verticalLineToRelative(3f); lineToRelative(4f, -4f); lineToRelative(-4f, -4f); verticalLineToRelative(3f); horizontalLineTo(5f); verticalLineToRelative(6f); horizontalLineToRelative(2f); verticalLineTo(7f); close()
            moveTo(17f, 17f); horizontalLineTo(7f); verticalLineToRelative(-3f); lineToRelative(-4f, 4f); lineToRelative(4f, 4f); verticalLineToRelative(-3f); horizontalLineToRelative(12f); verticalLineToRelative(-6f); horizontalLineToRelative(-2f); verticalLineToRelative(4f); close()
        }
    }.build()

    val Search: ImageVector = ImageVector.Builder("Search", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(15.5f, 14f); horizontalLineToRelative(-0.79f); lineToRelative(-0.28f, -0.27f);
            arcTo(6.471f, 6.471f, 0f, isMoreThanHalf = false, isPositiveArc = false, 16f, 9.5f)
            arcTo(6.5f, 6.5f, 0f, isMoreThanHalf = true, isPositiveArc = false, 9.5f, 16f)
            arcTo(6.471f, 6.471f, 0f, isMoreThanHalf = false, isPositiveArc = false, 14f, 14.43f)
            lineToRelative(0.27f, 0.28f); verticalLineToRelative(0.79f); lineToRelative(5f, 4.99f); lineTo(20.49f, 19f)
            lineToRelative(-4.99f, -5f); close()
            moveTo(9.5f, 14f); arcTo(4.5f, 4.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14f, 9.5f); arcTo(4.5f, 4.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 9.5f, 14f); close()
        }
    }.build()

    val Favorite: ImageVector = ImageVector.Builder("Favorite", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 21.35f); lineToRelative(-1.45f, -1.32f);
            curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
            curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
            curveToRelative(1.74f, 0f, 3.41f, 0.81f, 4.5f, 2.09f)
            curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
            curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
            curveToRelative(0f, 3.78f, -3.4f, 6.86f, -8.55f, 11.54f)
            lineTo(12f, 21.35f); close()
        }
    }.build()

    val FavoriteBorder: ImageVector = ImageVector.Builder("FavoriteBorder", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(16.5f, 3f)
            curveToRelative(-1.74f, 0f, -3.41f, 0.81f, -4.5f, 2.09f)
            curveTo(10.91f, 3.81f, 9.24f, 3f, 7.5f, 3f)
            curveTo(4.42f, 3f, 2f, 5.42f, 2f, 8.5f)
            curveToRelative(0f, 3.78f, 3.4f, 6.86f, 8.55f, 11.54f)
            lineTo(12f, 21.35f)
            lineToRelative(1.45f, -1.32f)
            curveTo(18.6f, 15.36f, 22f, 12.28f, 22f, 8.5f)
            curveTo(22f, 5.42f, 19.58f, 3f, 16.5f, 3f)
            close()
            moveTo(12.1f, 18.55f)
            lineToRelative(-0.1f, 0.1f)
            lineToRelative(-0.1f, -0.1f)
            curveTo(7.14f, 14.24f, 4f, 11.39f, 4f, 8.5f)
            curveTo(4f, 6.5f, 5.5f, 5f, 7.5f, 5f)
            curveToRelative(1.54f, 0f, 3.04f, 0.99f, 3.57f, 2.36f)
            horizontalLineToRelative(1.87f)
            curveTo(13.46f, 5.99f, 14.96f, 5f, 16.5f, 5f)
            curveToRelative(2f, 0f, 3.5f, 1.5f, 3.5f, 3.5f)
            curveToRelative(0f, 2.89f, -3.14f, 5.74f, -7.9f, 10.05f)
            close()
        }
    }.build()

    val Queue: ImageVector = ImageVector.Builder("Queue", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(15f, 6f); horizontalLineTo(3f); verticalLineToRelative(2f); horizontalLineToRelative(12f); verticalLineTo(6f); close()
            moveTo(15f, 10f); horizontalLineTo(3f); verticalLineToRelative(2f); horizontalLineToRelative(12f); verticalLineToRelative(-2f); close()
            moveTo(3f, 16f); horizontalLineToRelative(8f); verticalLineToRelative(-2f); horizontalLineTo(3f); verticalLineToRelative(2f); close()
            moveTo(17f, 6f); verticalLineToRelative(8.18f); arcTo(3f, 3f, 0f, false, false, 15f, 14f); arcTo(3f, 3f, 0f, true, false, 18f, 17f); verticalLineTo(8f); horizontalLineToRelative(3f); verticalLineTo(6f); horizontalLineToRelative(-4f); close()
        }
    }.build()

    val Artists: ImageVector = ImageVector.Builder("Artists", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 12f); arcTo(4f, 4f, 0f, true, false, 8f, 8f); arcTo(4f, 4f, 0f, false, false, 12f, 12f); close()
            moveTo(12f, 14f); curveTo(9.33f, 14f, 4f, 15.34f, 4f, 18f); verticalLineToRelative(2f); horizontalLineToRelative(16f); verticalLineToRelative(-2f); curveTo(20f, 15.34f, 14.67f, 14f, 12f, 14f); close()
        }
    }.build()

    val Albums: ImageVector = ImageVector.Builder("Albums", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 2f); arcTo(10f, 10f, 0f, true, false, 22f, 12f); arcTo(10f, 10f, 0f, false, false, 12f, 2f); close()
            moveTo(12f, 14.5f); arcTo(2.5f, 2.5f, 0f, true, true, 14.5f, 12f); arcTo(2.5f, 2.5f, 0f, false, true, 12f, 14.5f); close()
        }
    }.build()

    val Playlists: ImageVector = ImageVector.Builder("Playlists", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(4f, 10f); horizontalLineToRelative(12f); verticalLineToRelative(2f); horizontalLineTo(4f); close()
            moveTo(4f, 6f); horizontalLineToRelative(12f); verticalLineToRelative(2f); horizontalLineTo(4f); close()
            moveTo(4f, 14f); horizontalLineToRelative(8f); verticalLineToRelative(2f); horizontalLineTo(4f); close()
            moveTo(14f, 14f); verticalLineToRelative(6f); lineToRelative(5f, -3f); close()
        }
    }.build()

    val History: ImageVector = ImageVector.Builder("History", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(13f, 3f); arcTo(9f, 9f, 0f, false, false, 4.38f, 7.37f); lineTo(2f, 5f); verticalLineToRelative(6f); horizontalLineToRelative(6f); lineToRelative(-2.62f, -2.62f); arcTo(7f, 7f, 0f, true, true, 12f, 19f); arcTo(7f, 7f, 0f, false, true, 5.08f, 13f); horizontalLineTo(3.05f); arcTo(9f, 9f, 0f, true, false, 13f, 3f); close()
            moveTo(12f, 8f); verticalLineToRelative(4.5f); lineToRelative(3.5f, 2.1f); lineToRelative(0.75f, -1.23f); lineToRelative(-2.75f, -1.62f); verticalLineTo(8f); horizontalLineTo(12f); close()
        }
    }.build()

    val Folder: ImageVector = ImageVector.Builder("Folder", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(10f, 4f); horizontalLineTo(4f); arcTo(2f, 2f, 0f, false, false, 2f, 6f); verticalLineToRelative(12f); arcTo(2f, 2f, 0f, false, false, 4f, 20f); horizontalLineToRelative(16f); arcTo(2f, 2f, 0f, false, false, 22f, 18f); verticalLineTo(8f); arcTo(2f, 2f, 0f, false, false, 20f, 6f); horizontalLineToRelative(-8f); lineToRelative(-2f, -2f); close()
        }
    }.build()

    val MusicNote: ImageVector = ImageVector.Builder("MusicNote", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 3f); verticalLineToRelative(10.55f); arcTo(4f, 4f, 0f, true, false, 14f, 17f); verticalLineTo(7f); horizontalLineToRelative(4f); verticalLineTo(3f); horizontalLineToRelative(-6f); close()
        }
    }.build()
}
