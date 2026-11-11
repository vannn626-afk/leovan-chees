package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.content.ChessAcademyCatalog
import com.example.data.content.ChessPuzzleCatalog
import com.example.data.local.LessonProgressEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.SavedGameEntity
import com.example.engine.AiDifficulty
import com.example.engine.TimeControlPreset
import com.example.ui.auth.LvChessLogoBadge
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.LocalChessThemeColors
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ColorSelectionChoice

@Composable
fun MainMenuScreen(
    profile: PlayerProfileEntity,
    ongoingGame: SavedGameEntity?,
    lessonProgress: List<LessonProgressEntity>,
    onNavigate: (AppScreen) -> Unit,
    onQuickPlay: (AiDifficulty, ColorSelectionChoice, TimeControlPreset, Boolean) -> Unit,
    onResumeOngoingGame: () -> Unit,
    onCycleTheme: (AppThemeOption) -> Unit
) {
    val themeColors = LocalChessThemeColors.current
    val currentTheme = runCatching { AppThemeOption.valueOf(profile.selectedTheme) }
        .getOrDefault(AppThemeOption.CLASSIC_WOOD)

    val completedLessonsCount = lessonProgress.count { it.completed }
    val totalLessonsCount = ChessAcademyCatalog.totalLessonCount.coerceAtLeast(1)
    val academyProgressFraction = (completedLessonsCount.toFloat() / totalLessonsCount).coerceIn(0f, 1f)

    val todayEpochDay = System.currentTimeMillis() / 86_400_000L
    val dailyPuzzle = ChessPuzzleCatalog.getDailyPuzzle(todayEpochDay)
    val dailySolved = profile.lastDailyPuzzleEpochDay == todayEpochDay

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.backgroundPrimary),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. HEADER BRAND & LOGO LV BIDAK CATUR
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LvChessLogoBadge(
                            size = 56.dp,
                            cornerRadius = 14.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "LEOVAN CHEESY",
                                style = MaterialTheme.typography.headlineMedium,
                                color = themeColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Halo, ${profile.playerName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = themeColors.textSecondary
                            )
                        }
                    }

                    // Tombol Ganti Tema Cepat
                    OutlinedButton(
                        onClick = {
                            val nextTheme = when (currentTheme) {
                                AppThemeOption.CLASSIC_WOOD -> AppThemeOption.ROYAL_BLUE
                                AppThemeOption.ROYAL_BLUE -> AppThemeOption.MONOCHROME
                                AppThemeOption.MONOCHROME -> AppThemeOption.CLASSIC_WOOD
                            }
                            onCycleTheme(nextTheme)
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("main_theme_cycle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Ganti Tema",
                            tint = themeColors.accentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentTheme.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = themeColors.textPrimary
                        )
                    }
                }
            }

            // 2. KARTU UTAMA — MULAI BERMAIN (DENGAN WHITE SPACE LEGA)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "BERPIKIR • MELANGKAH • KUASAI",
                            style = MaterialTheme.typography.labelMedium,
                            color = themeColors.accentPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Siap Bermain Catur?",
                            style = MaterialTheme.typography.headlineSmall,
                            color = themeColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pilih lawan komputer, bermain berdua di satu layar, atau tanding online.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = themeColors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = { onNavigate(AppScreen.PLAY_HUB) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.accentPrimary,
                                contentColor = themeColors.backgroundPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("main_play_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Main Catur",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onQuickPlay(
                                        AiDifficulty.MEDIUM,
                                        ColorSelectionChoice.WHITE,
                                        TimeControlPreset.ALL_PRESETS[6],
                                        profile.beginnerAssistanceEnabled
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("quick_play_button")
                            ) {
                                Text(
                                    text = "Main Cepat 10m",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = themeColors.textPrimary,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    onQuickPlay(
                                        AiDifficulty.BEGINNER,
                                        ColorSelectionChoice.WHITE,
                                        TimeControlPreset.ALL_PRESETS.last(),
                                        true
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("beginner_mode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = themeColors.accentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mode Pemula",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = themeColors.textPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 3. LANJUTKAN PERMAINAN (JIKA ADA)
            if (ongoingGame != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceElevated),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, themeColors.accentPrimary, RoundedCornerShape(16.dp))
                            .clickable { onResumeOngoingGame() }
                            .testTag("continue_game_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LANJUTKAN PERMAINAN",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = themeColors.accentPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${ongoingGame.whitePlayerName} vs ${ongoingGame.blackPlayerName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = themeColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Langkah ke-${ongoingGame.totalMoves} • Ketuk untuk melanjutkan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = themeColors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(themeColors.accentPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Lanjutkan",
                                    tint = themeColors.backgroundPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 4. RINGKASAN HARIAN (TEKA-TEKI & AKADEMI)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(16.dp))
                            .clickable { onNavigate(AppScreen.PUZZLES) }
                            .testTag("daily_puzzle_card")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Teka-Teki Harian",
                                style = MaterialTheme.typography.labelMedium,
                                color = themeColors.accentPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = dailyPuzzle.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = themeColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (dailySolved) "Selesai Hari Ini" else "Rating ${dailyPuzzle.ratingEstimate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (dailySolved) themeColors.statusSuccess else themeColors.textSecondary
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(16.dp))
                            .clickable { onNavigate(AppScreen.CHESS_ACADEMY) }
                            .testTag("academy_progress_card")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Akademi Catur",
                                style = MaterialTheme.typography.labelMedium,
                                color = themeColors.accentPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$completedLessonsCount / $totalLessonsCount Materi",
                                style = MaterialTheme.typography.titleSmall,
                                color = themeColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { academyProgressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = themeColors.accentPrimary,
                                trackColor = themeColors.backgroundSecondary
                            )
                        }
                    }
                }
            }

            // 5. DAFTAR MENU SIMPLE & RAPI
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Jelajahi Fitur",
                        style = MaterialTheme.typography.titleMedium,
                        color = themeColors.textPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    SimpleMenuRowCard(
                        title = "Pengaturan Permainan",
                        subtitle = "Pilih lawan komputer, lokal 1 vs 1, atau ruangan online",
                        icon = Icons.Default.SportsEsports,
                        tag = "nav_play_card",
                        onClick = { onNavigate(AppScreen.PLAY_HUB) }
                    )

                    SimpleMenuRowCard(
                        title = "Akademi Catur",
                        subtitle = "Belajar langkah dasar, taktik, hingga strategi bertahap",
                        icon = Icons.Default.School,
                        tag = "nav_academy_card",
                        onClick = { onNavigate(AppScreen.CHESS_ACADEMY) }
                    )

                    SimpleMenuRowCard(
                        title = "Teka-Teki Taktik",
                        subtitle = "Latih ketajaman skakmat dan kombinasi serangan",
                        icon = Icons.Default.Extension,
                        tag = "nav_puzzles_card",
                        onClick = { onNavigate(AppScreen.PUZZLES) }
                    )

                    SimpleMenuRowCard(
                        title = "Cara Bermain Catur",
                        subtitle = "Panduan visual aturan bidak, rokade, dan remis",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        tag = "nav_how_to_play_card",
                        onClick = { onNavigate(AppScreen.HOW_TO_PLAY) }
                    )

                    SimpleMenuRowCard(
                        title = "Panduan Menjadi Master",
                        subtitle = "Tips praktis dan daftar target latihan catur",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        tag = "nav_improve_card",
                        onClick = { onNavigate(AppScreen.IMPROVE_YOUR_CHESS) }
                    )

                    SimpleMenuRowCard(
                        title = "Statistik & Riwayat",
                        subtitle = "Rekor pertandingan, akurasi, dan arsip PGN",
                        icon = Icons.Default.BarChart,
                        tag = "nav_statistics_card",
                        onClick = { onNavigate(AppScreen.STATISTICS) }
                    )

                    SimpleMenuRowCard(
                        title = "Pengaturan",
                        subtitle = "Tema papan, suara, akun Google, dan bantuan pemula",
                        icon = Icons.Default.Settings,
                        tag = "nav_settings_card",
                        onClick = { onNavigate(AppScreen.SETTINGS) }
                    )

                    SimpleMenuRowCard(
                        title = "Tentang Aplikasi",
                        subtitle = "Informasi LEOVAN STUDIO, mesin catur, dan lisensi",
                        icon = Icons.Default.Info,
                        tag = "nav_about_card",
                        onClick = { onNavigate(AppScreen.ABOUT) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SimpleMenuRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    val themeColors = LocalChessThemeColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeColors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = themeColors.accentPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = themeColors.textPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = themeColors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
