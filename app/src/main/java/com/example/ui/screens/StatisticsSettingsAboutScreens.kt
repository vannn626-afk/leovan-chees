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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.content.ChessAcademyCatalog
import com.example.data.local.LessonProgressEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.SavedGameEntity
import com.example.ui.auth.LvChessLogoBadge
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.LocalChessThemeColors

// ============================================================================
// 1. LAYAR STATISTIK & RIWAYAT PERTANDINGAN
// ============================================================================

@Composable
fun StatisticsScreen(
    profile: PlayerProfileEntity,
    completedGames: List<SavedGameEntity>,
    lessonProgress: List<LessonProgressEntity>,
    onBack: () -> Unit,
    onAnalyzeSavedGame: (SavedGameEntity) -> Unit
) {
    val themeColors = LocalChessThemeColors.current
    val winRate = if (profile.gamesPlayed > 0) {
        (profile.gamesWon * 100) / profile.gamesPlayed
    } else 0
    val completedLessons = lessonProgress.count { it.completed }
    val totalPlayMinutes = profile.totalPlayTimeSeconds / 60L

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
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("stats_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = themeColors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Statistik & Riwayat",
                            style = MaterialTheme.typography.headlineMedium,
                            color = themeColors.textPrimary
                        )
                        Text(
                            text = "Profil Pemain: ${profile.playerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.accentPrimary
                        )
                    }
                }
            }

            // Grid Metrik Utama
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMetricBox("Dimainkan", "${profile.gamesPlayed}", Modifier.weight(1f))
                            StatMetricBox("Rasio Menang", "$winRate%", Modifier.weight(1f))
                            StatMetricBox("Menang/Kalah/Seri", "${profile.gamesWon}/${profile.gamesLost}/${profile.gamesDrawn}", Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMetricBox("Teka-Teki", "${profile.puzzlesSolved}", Modifier.weight(1f))
                            StatMetricBox("Rekor Beruntun", "${profile.bestPuzzleStreak}", Modifier.weight(1f))
                            StatMetricBox("Akademi", "$completedLessons/${ChessAcademyCatalog.totalLessonCount}", Modifier.weight(1f))
                        }
                        Text(
                            text = "Total Waktu Bermain: $totalPlayMinutes menit",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.textSecondary
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Riwayat Pertandingan Terakhir",
                    style = MaterialTheme.typography.titleMedium,
                    color = themeColors.accentPrimary
                )
            }

            if (completedGames.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Belum ada pertandingan selesai. Mainkan pertandingan pertama Anda untuk melihat riwayat dan analisis di sini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = themeColors.textSecondary,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(completedGames, key = { it.id }) { game ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${game.whitePlayerName} vs ${game.blackPlayerName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = themeColors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${game.terminationReason} • ${game.totalMoves} langkah",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = themeColors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { onAnalyzeSavedGame(game) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analisis", color = themeColors.accentPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    val themeColors = LocalChessThemeColors.current
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = themeColors.textPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = themeColors.textSecondary
        )
    }
}

// ============================================================================
// 2. LAYAR PENGATURAN APLIKASI
// ============================================================================

@Composable
fun SettingsScreen(
    profile: PlayerProfileEntity,
    onBack: () -> Unit,
    onSelectTheme: (AppThemeOption) -> Unit,
    onUpdateProfileName: (String, Int) -> Unit,
    onUpdateSettingsFlags: ((PlayerProfileEntity) -> PlayerProfileEntity) -> Unit,
    onResetAllData: () -> Unit,
    isCloudSignedIn: Boolean = false,
    cloudUserEmail: String? = null,
    onSignInOrOutCloud: () -> Unit = {}
) {
    val themeColors = LocalChessThemeColors.current
    val currentTheme = runCatching { AppThemeOption.valueOf(profile.selectedTheme) }
        .getOrDefault(AppThemeOption.CLASSIC_WOOD)

    var nameInput by remember(profile.playerName) { mutableStateOf(profile.playerName) }
    var showResetConfirm by remember { mutableStateOf(false) }

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
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = themeColors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pengaturan",
                        style = MaterialTheme.typography.headlineMedium,
                        color = themeColors.textPrimary
                    )
                }
            }

            // Pemilih Tema Visual
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tema Tampilan & Papan",
                            style = MaterialTheme.typography.titleMedium,
                            color = themeColors.accentPrimary
                        )
                        AppThemeOption.entries.forEach { option ->
                            val isSelected = option == currentTheme
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) themeColors.surfaceElevated else themeColors.backgroundSecondary)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) themeColors.accentPrimary else themeColors.surfaceBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectTheme(option) }
                                    .padding(16.dp)
                                    .testTag("theme_option_${option.name.lowercase()}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = themeColors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = themeColors.textSecondary
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        text = "Aktif",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = themeColors.accentPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Profil & Akun Google Cloud
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Profil Pemain & Akun Cloud",
                            style = MaterialTheme.typography.titleMedium,
                            color = themeColors.accentPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Nama Pemain") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onUpdateProfileName(nameInput, profile.avatarIndex) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Text("Simpan")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isCloudSignedIn) "Terhubung ke Google Cloud" else "Mode Offline",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = themeColors.textPrimary
                                )
                                Text(
                                    text = cloudUserEmail ?: "Masuk untuk sinkronisasi progres & main Online 1v1",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = themeColors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = onSignInOrOutCloud,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("settings_cloud_auth_button")
                            ) {
                                Text(
                                    text = if (isCloudSignedIn) "Keluar" else "Masuk",
                                    color = themeColors.accentPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Preferensi Suara & Permainan
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Preferensi Suara & Papan",
                            style = MaterialTheme.typography.titleMedium,
                            color = themeColors.accentPrimary
                        )

                        SettingToggleRow(
                            title = "Efek Suara",
                            subtitle = "Suara langkah bidak, makan, skak, dan kemenangan",
                            checked = profile.soundEffectsEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateSettingsFlags { it.copy(soundEffectsEnabled = enabled) }
                            }
                        )

                        SettingToggleRow(
                            title = "Indikator Langkah Sah",
                            subtitle = "Tampilkan titik tujuan saat bidak dipilih",
                            checked = profile.showLegalMoveIndicators,
                            onCheckedChange = { enabled ->
                                onUpdateSettingsFlags { it.copy(showLegalMoveIndicators = enabled) }
                            }
                        )

                        SettingToggleRow(
                            title = "Bantuan Pemula Default",
                            subtitle = "Aktifkan penjelasan aturan dan peringatan skak secara otomatis",
                            checked = profile.beginnerAssistanceEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateSettingsFlags { it.copy(beginnerAssistanceEnabled = enabled) }
                            }
                        )

                        SettingToggleRow(
                            title = "Izinkan Urungkan Langkah vs Komputer",
                            subtitle = "Bolehkan membatalkan langkah saat melawan AI",
                            checked = profile.allowUndoVsAi,
                            onCheckedChange = { enabled ->
                                onUpdateSettingsFlags { it.copy(allowUndoVsAi = enabled) }
                            }
                        )

                        Column {
                            Text(
                                text = "Kecepatan Animasi Bidak",
                                style = MaterialTheme.typography.titleSmall,
                                color = themeColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("SUBTLE" to "Cepat", "STANDARD" to "Normal", "DYNAMIC" to "Halus").forEach { (spd, label) ->
                                    FilterChip(
                                        selected = profile.animationIntensity == spd,
                                        onClick = {
                                            onUpdateSettingsFlags { it.copy(animationIntensity = spd) }
                                        },
                                        label = { Text(label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = themeColors.accentPrimary,
                                            selectedLabelColor = themeColors.backgroundPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Reset Data Lokal
            item {
                OutlinedButton(
                    onClick = { showResetConfirm = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("reset_all_data_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = themeColors.statusDanger)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus & Reset Semua Data Lokal", color = themeColors.statusDanger)
                }
            }
        }

        if (showResetConfirm) {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text("Reset Semua Data?") },
                text = {
                    Text("Tindakan ini akan mengembalikan statistik lokal, riwayat pertandingan, dan progres akademi ke awal.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetConfirm = false
                            onResetAllData()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.statusDanger)
                    ) {
                        Text("Reset Sekarang")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val themeColors = LocalChessThemeColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = themeColors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ============================================================================
// 3. LAYAR TENTANG APLIKASI & LISENSI
// ============================================================================

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val themeColors = LocalChessThemeColors.current

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
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("about_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = themeColors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tentang Aplikasi",
                        style = MaterialTheme.typography.headlineMedium,
                        color = themeColors.textPrimary
                    )
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, themeColors.accentPrimary, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LvChessLogoBadge(
                            size = 84.dp,
                            cornerRadius = 20.dp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "LEOVAN CHEESY",
                            style = MaterialTheme.typography.headlineLarge,
                            color = themeColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "BERPIKIR • MELANGKAH • KUASAI",
                            style = MaterialTheme.typography.labelLarge,
                            color = themeColors.accentPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Dikembangkan oleh LEOVAN STUDIO • Versi 1.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = themeColors.textSecondary
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Mesin Catur & Standar Aturan FIDE",
                            style = MaterialTheme.typography.titleMedium,
                            color = themeColors.accentPrimary
                        )
                        Text(
                            text = "LEOVAN CHEESY mengimplementasikan seluruh aturan resmi catur internasional FIDE, termasuk Rokade pendek/panjang, En Passant, promosi pion, deteksi Skakmat, Pat (Stalemate), materi tidak cukup, pengulangan tiga kali (Zobrist 64-bit), serta aturan 50 dan 75 langkah.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = themeColors.textPrimary
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Lisensi Analisis UCI & Open Source",
                            style = MaterialTheme.typography.titleMedium,
                            color = themeColors.accentPrimary
                        )
                        Text(
                            text = "Analisis pasca-permainan menggunakan protokol Universal Chess Interface (UCI) yang kompatibel dengan Stockfish (GNU General Public License v3.0 — GPLv3) beserta mesin evaluasi taktis lokal LEOVAN (PeSTO + Quiescence Search).",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
