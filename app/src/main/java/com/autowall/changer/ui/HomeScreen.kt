package com.autowall.changer.ui

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.autowall.changer.model.AutoWallSettings
import com.autowall.changer.model.FitMode
import com.autowall.changer.model.INTERVALS
import com.autowall.changer.model.TONES
import com.autowall.changer.model.intervalLabel
import com.autowall.changer.ui.theme.Accent
import com.autowall.changer.ui.theme.Bg
import com.autowall.changer.ui.theme.BgDeep
import com.autowall.changer.ui.theme.Faint
import com.autowall.changer.ui.theme.Ink
import com.autowall.changer.ui.theme.Line
import com.autowall.changer.ui.theme.Muted
import com.autowall.changer.ui.theme.Surface1
import com.autowall.changer.ui.theme.Surface2
import com.autowall.changer.ui.theme.Surface3
import java.io.File
import kotlin.math.roundToInt

private const val MAX_PICK = 50

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
    }

    // Both contracts are remembered so they are registered exactly once.
    val galleryContract = remember { ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK) }
    val filesContract = remember { ActivityResultContracts.OpenMultipleDocuments() }

    val galleryPicker = rememberLauncherForActivityResult(galleryContract) { uris ->
        if (uris.isNotEmpty()) viewModel.importPhotos(uris)
    }
    val filesPicker = rememberLauncherForActivityResult(filesContract) { uris ->
        if (uris.isNotEmpty()) viewModel.importPhotos(uris)
    }

    fun launchGallery(): Boolean = runCatching {
        galleryPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }.isSuccess

    fun launchFiles(): Boolean = runCatching {
        filesPicker.launch(arrayOf("image/*"))
    }.isSuccess

    // Never leave the user with a dead tap: if one picker refuses to open, try the other.
    fun pick(source: PhotoSource) {
        val opened = when (source) {
            PhotoSource.Gallery -> launchGallery()
            PhotoSource.Files -> launchFiles()
        }
        if (opened) return
        val fallbackOpened = when (source) {
            PhotoSource.Gallery -> launchFiles()
            PhotoSource.Files -> launchGallery()
        }
        if (!fallbackOpened) {
            viewModel.reportError(
                "No photo picker available on this device. Install a file manager or Photos app, then try again.",
            )
        }
    }

    var showSourceSheet by remember { mutableStateOf(false) }
    var showTroubleshoot by remember { mutableStateOf(false) }
    val openPicker: () -> Unit = { showSourceSheet = true }

    val current = photos.getOrNull(settings.lastIndex.coerceAtLeast(0)) ?: photos.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Bg, BgDeep))),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = Surface3,
                        contentColor = Ink,
                        shape = RoundedCornerShape(16.dp),
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .systemBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Header(settings.enabled, onToggle = viewModel::setEnabled)
                Spacer(Modifier.height(18.dp))
                HeroCard(
                    photo = current,
                    settings = settings,
                    busy = busy,
                    onRefresh = { viewModel.applyNow() },
                    onAdd = openPicker,
                    onPickGallery = { pick(PhotoSource.Gallery) },
                    onPickFiles = { pick(PhotoSource.Files) },
                )
                Spacer(Modifier.height(16.dp))
                StatStrip(photos.size, settings)
                Spacer(Modifier.height(28.dp))

                SectionLabel("Photos", trailing = if (photos.isEmpty()) null else "${photos.size}")
                Spacer(Modifier.height(12.dp))
                PhotoStrip(
                    photos = photos,
                    activeIndex = settings.lastIndex,
                    onSelect = viewModel::applyNow,
                    onRemove = viewModel::removePhoto,
                    onAdd = openPicker,
                )
                Spacer(Modifier.height(28.dp))

                SectionLabel("Change every")
                Spacer(Modifier.height(12.dp))
                IntervalRow(selected = settings.intervalMinutes, onSelect = viewModel::setInterval)
                Spacer(Modifier.height(28.dp))

                SectionLabel("Look")
                Spacer(Modifier.height(12.dp))
                LookCard(
                    settings = settings,
                    onFit = viewModel::setFitMode,
                    onRadius = viewModel::setCornerRadius,
                    onTone = viewModel::setTone,
                    onToneStrength = viewModel::setToneStrength,
                    onShuffle = viewModel::setShuffle,
                    onLockScreen = viewModel::setLockScreen,
                )
                Spacer(Modifier.height(28.dp))
                Footer(
                    version = appVersion(context),
                    onTroubleshoot = { showTroubleshoot = true },
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (showSourceSheet) {
        SourceSheet(
            pickerAvailable = systemPickerAvailable(context),
            onDismiss = { showSourceSheet = false },
            onGallery = {
                showSourceSheet = false
                pick(PhotoSource.Gallery)
            },
            onFiles = {
                showSourceSheet = false
                pick(PhotoSource.Files)
            },
        )
    }

    if (showTroubleshoot) {
        TroubleshootDialog(
            context = context,
            photoCount = photos.size,
            lastError = lastError,
            onDismiss = { showTroubleshoot = false },
        )
    }
}

private enum class PhotoSource { Gallery, Files }

/* ------------------------------------------------------------------ pickers */

private fun systemPickerAvailable(context: Context): Boolean = runCatching {
    ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)
}.getOrDefault(false)

private fun appVersion(context: Context): String = runCatching {
    @Suppress("DEPRECATION")
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "1.1"

@Composable
private fun SourceSheet(
    pickerAvailable: Boolean,
    onDismiss: () -> Unit,
    onGallery: () -> Unit,
    onFiles: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Surface1,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Add photos", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Choose from your gallery, or browse the files and folders on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                    Spacer(Modifier.height(18.dp))
                    if (pickerAvailable) {
                        SourceRow(
                            title = "Photo gallery",
                            subtitle = "Recent shots, albums and cloud libraries",
                            onClick = onGallery,
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    SourceRow(
                        title = "Files & downloads",
                        subtitle = "Browse local storage, SD card and Downloads",
                        onClick = onFiles,
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.titleMedium, color = Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Faint)
        }
        GlyphIcon(Glyph.Plus, tint = Muted, modifier = Modifier.size(16.dp))
    }
}

/* -------------------------------------------------------------- diagnostics */

@Composable
private fun TroubleshootDialog(
    context: Context,
    photoCount: Int,
    lastError: String?,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val report = remember(photoCount, lastError) {
        buildString {
            appendLine("AutoWall ${appVersion(context)}")
            appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Photos stored: $photoCount")
            appendLine("System photo picker: ${if (systemPickerAvailable(context)) "available" else "not available"}")
            append("Last error: ${lastError ?: "none recorded"}")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Surface1) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Troubleshoot", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(10.dp))
                Text(
                    report,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SelectableChip(
                        label = "Copy",
                        selected = true,
                        onClick = { clipboard.setText(AnnotatedString(report)) },
                    )
                    SelectableChip(label = "Close", selected = false, onClick = onDismiss)
                }
            }
        }
    }
}

/* -------------------------------------------------------------------- header */

@Composable
private fun Header(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark()
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "AutoWall",
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
            )
            Text(
                text = if (enabled) "Rotating automatically" else "Rotation paused",
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) Muted else Faint,
            )
        }
        AutoWallSwitch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun BrandMark() {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFFF6F2E7), Color(0xFFBDB5A1)))),
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = Color(0xFF14141A),
                radius = w * 0.085f,
                center = androidx.compose.ui.geometry.Offset(w * 0.33f, h * 0.34f),
            )
            val mountain = Path().apply {
                moveTo(w * 0.16f, h * 0.76f)
                lineTo(w * 0.40f, h * 0.48f)
                lineTo(w * 0.54f, h * 0.62f)
                lineTo(w * 0.66f, h * 0.46f)
                lineTo(w * 0.86f, h * 0.76f)
                close()
            }
            drawPath(mountain, color = Color(0xFF14141A))
        }
    }
}

/* ---------------------------------------------------------------------- hero */

@Composable
private fun HeroCard(
    photo: File?,
    settings: AutoWallSettings,
    busy: Boolean,
    onRefresh: () -> Unit,
    onAdd: () -> Unit,
    onPickGallery: () -> Unit,
    onPickFiles: () -> Unit,
) {
    val radius = settings.cornerRadiusDp.coerceIn(0, 48).dp
    val shape = RoundedCornerShape(radius)
    Box(modifier = Modifier.fillMaxWidth().height(348.dp)) {
        if (photo == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Surface1)
                    .drawBehind {
                        drawRoundRect(
                            color = Line,
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(10f, 10f),
                                    0f,
                                ),
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx()),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Surface3),
                        contentAlignment = Alignment.Center,
                    ) {
                        GlyphIcon(Glyph.Plus, tint = Ink, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("No photos yet", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pick a few favourites to rotate",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                    Spacer(Modifier.height(18.dp))
                    Surface(
                        onClick = onPickGallery,
                        shape = RoundedCornerShape(50),
                        color = Accent,
                        contentColor = Color(0xFF0B0B0C),
                    ) {
                        Text(
                            "Choose photos",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onPickFiles)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "Browse files instead",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().clip(shape)) {
                if (settings.fitMode == FitMode.FIT) {
                    AsyncImage(
                        model = photo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 0.45f; scaleX = 1.2f; scaleY = 1.2f },
                    )
                }
                AsyncImage(
                    model = photo,
                    contentDescription = "Current wallpaper",
                    contentScale = if (settings.fitMode == FitMode.FIT) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                val tone = settings.tone
                if (tone.argb != 0L && settings.toneStrength > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(tone.argb).copy(alpha = settings.toneStrength)),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.5f to Color.Transparent,
                                1f to Color(0xD9000000),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CURRENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD8D4C8),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = relativeTime(settings.lastAppliedAt),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        Text(
                            text = if (settings.enabled) {
                                "Next change in ${nextChange(settings)}"
                            } else {
                                "Turn on to rotate automatically"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xCCB9B5AA),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .clickable(enabled = !busy, onClick = onRefresh),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            GlyphIcon(
                                Glyph.Refresh,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp),
                                stroke = 1.8.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

/* --------------------------------------------------------------------- stats */

@Composable
private fun StatStrip(photoCount: Int, settings: AutoWallSettings) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Stat("Photos", photoCount.toString(), Modifier.weight(1f))
        Stat("Every", intervalLabel(settings.intervalMinutes), Modifier.weight(1f))
        Stat(
            label = "Status",
            value = if (settings.enabled) "On" else "Off",
            modifier = Modifier.weight(1f),
            accent = settings.enabled,
        )
    }
}

@Composable
private fun Stat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Faint)
        Spacer(Modifier.height(5.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = if (accent) Accent else Ink,
        )
    }
}

/* ------------------------------------------------------------------ photos */

@Composable
private fun PhotoStrip(
    photos: List<File>,
    activeIndex: Int,
    onSelect: (File) -> Unit,
    onRemove: (File) -> Unit,
    onAdd: () -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().height(112.dp),
    ) {
        items(items = photos, key = { it.absolutePath }) { photo ->
            val index = photos.indexOf(photo)
            Box(
                modifier = Modifier
                    .size(width = 82.dp, height = 108.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Surface2)
                    .then(
                        if (index == activeIndex) {
                            Modifier.border(1.5.dp, Ink, RoundedCornerShape(18.dp))
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(photo) },
            ) {
                AsyncImage(
                    model = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .clickable { onRemove(photo) },
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(Glyph.Close, tint = Color.White, modifier = Modifier.size(9.dp), stroke = 1.6.dp)
                }
            }
        }
        item {
            Column(
                modifier = Modifier
                    .size(width = 82.dp, height = 108.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .drawBehind {
                        drawRoundRect(
                            color = Line,
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(8f, 8f),
                                    0f,
                                ),
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                        )
                    }
                    .clickable(onClick = onAdd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                GlyphIcon(Glyph.Plus, tint = Muted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(8.dp))
                Text("Add", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
    }
}

@Composable
private fun IntervalRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        INTERVALS.forEach { minutes ->
            SelectableChip(
                label = intervalLabel(minutes),
                selected = selected == minutes,
                onClick = { onSelect(minutes) },
            )
        }
    }
}

/* --------------------------------------------------------------------- look */

@Composable
private fun LookCard(
    settings: AutoWallSettings,
    onFit: (FitMode) -> Unit,
    onRadius: (Int) -> Unit,
    onTone: (Int) -> Unit,
    onToneStrength: (Float) -> Unit,
    onShuffle: (Boolean) -> Unit,
    onLockScreen: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Surface1,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            LabeledRow(label = "Fit") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SelectableChip(
                        label = "Fill",
                        selected = settings.fitMode == FitMode.FILL,
                        onClick = { onFit(FitMode.FILL) },
                    )
                    SelectableChip(
                        label = "Fit",
                        selected = settings.fitMode == FitMode.FIT,
                        onClick = { onFit(FitMode.FIT) },
                    )
                }
            }
            ThinDivider(Modifier.padding(vertical = 16.dp))

            var radius by remember(settings.cornerRadiusDp) {
                mutableStateOf(settings.cornerRadiusDp.toFloat())
            }
            LabeledRow(label = "Corners", value = "${radius.roundToInt()}dp") {
                Slider(
                    value = radius,
                    onValueChange = { radius = (it / 4f).roundToInt() * 4f },
                    onValueChangeFinished = { onRadius(radius.roundToInt()) },
                    valueRange = 0f..64f,
                    steps = 15,
                    colors = sliderColors(),
                    modifier = Modifier.height(24.dp),
                )
            }

            ThinDivider(Modifier.padding(vertical = 16.dp))
            Text("Tone", style = MaterialTheme.typography.bodyMedium, color = Muted)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TONES.forEachIndexed { index, tone ->
                    val selected = settings.toneIndex == index
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .then(if (selected) Modifier.border(1.5.dp, Ink, CircleShape) else Modifier)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(if (tone.argb == 0L) Surface3 else Color(tone.argb))
                            .clickable { onTone(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (tone.argb == 0L) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(1.dp)
                                    .graphicsLayer { rotationZ = -45f }
                                    .background(Muted),
                            )
                        }
                    }
                }
            }
            if (settings.toneIndex != 0) {
                Spacer(Modifier.height(10.dp))
                var strength by remember(settings.toneStrength) {
                    mutableStateOf(settings.toneStrength)
                }
                LabeledRow(label = "Strength", value = "${(strength * 100).roundToInt()}%") {
                    Slider(
                        value = strength,
                        onValueChange = { strength = it },
                        onValueChangeFinished = { onToneStrength(strength) },
                        valueRange = 0.05f..0.8f,
                        colors = sliderColors(),
                        modifier = Modifier.height(24.dp),
                    )
                }
            }

            ThinDivider(Modifier.padding(vertical = 16.dp))
            ToggleRow(
                title = "Shuffle order",
                subtitle = "Random photo each time",
                checked = settings.shuffle,
                onCheckedChange = onShuffle,
            )
            Spacer(Modifier.height(14.dp))
            ToggleRow(
                title = "Lock screen too",
                subtitle = "Use the same photo on both screens",
                checked = settings.applyToLockScreen,
                onCheckedChange = onLockScreen,
            )
        }
    }
}

@Composable
private fun LabeledRow(
    label: String,
    value: String? = null,
    content: @Composable () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Muted)
            Spacer(Modifier.weight(1f))
            if (value != null) {
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = Faint,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        AutoWallSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Ink,
    activeTrackColor = Ink,
    inactiveTrackColor = Surface3,
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent,
)

@Composable
private fun Footer(version: String, onTroubleshoot: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Line),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Wallpapers rotate in the background. No ads, no accounts, no data leaves your phone.",
            style = MaterialTheme.typography.bodySmall,
            color = Faint,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "AutoWall $version",
                style = MaterialTheme.typography.labelSmall,
                color = Faint,
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onTroubleshoot)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "Troubleshoot",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
    }
}

/* -------------------------------------------------------------------- helper */

private fun relativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return "Preview"
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}

private fun nextChange(settings: AutoWallSettings): String {
    if (settings.lastAppliedAt <= 0L) return intervalLabel(settings.intervalMinutes)
    val due = settings.lastAppliedAt + settings.intervalMinutes * 60_000L
    val remaining = due - System.currentTimeMillis()
    return when {
        remaining <= 0 -> "a moment"
        remaining < 3_600_000 -> "${remaining / 60_000}m"
        remaining < 86_400_000 -> "${remaining / 3_600_000}h"
        else -> "${remaining / 86_400_000}d"
    }
}
