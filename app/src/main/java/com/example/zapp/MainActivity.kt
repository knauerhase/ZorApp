package com.example.zapp

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.zapp.ui.theme.ZappTheme
import java.io.File

data class ImageItem(
    val name: String,
    val source: Any, // File or Uri
    val sizeBytes: Long = 0,
    val isPicked: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZappTheme(darkTheme = true) {
                ZappImageApp()
            }
        }
    }
}

@Composable
fun ZoomableImageContainer(
    item: ImageItem,
    imageLoader: ImageLoader,
    isFillMode: Boolean,
    modifier: Modifier = Modifier,
    onScaleChanged: (Float) -> Unit = {}
) {
    var scale by remember(item.name) { mutableFloatStateOf(1f) }
    var offset by remember(item.name) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 200),
        label = "zoomScale"
    )

    LaunchedEffect(animatedScale) {
        onScaleChanged(animatedScale)
    }

    val panModifier = if (scale > 1.05f) {
        Modifier.pointerInput(item.name) {
            detectTransformGestures { _, pan, zoom, _ ->
                val newScale = (scale * zoom).coerceIn(1f, 5f)
                if (newScale > 1.05f) {
                    val maxOffsetX = (containerSize.width * (newScale - 1f)) / 2f
                    val maxOffsetY = (containerSize.height * (newScale - 1f)) / 2f
                    val newOffsetX = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                    val newOffsetY = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                    scale = newScale
                    offset = Offset(newOffsetX, newOffsetY)
                } else {
                    scale = 1f
                    offset = Offset.Zero
                }
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(item.name) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            offset = Offset.Zero
                        }
                    }
                )
            }
            .then(panModifier),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = item.source,
            imageLoader = imageLoader,
            contentDescription = item.name,
            contentScale = if (isFillMode) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    translationX = offset.x
                    translationY = offset.y
                }
        )

        // Subdued Zoom Level badge when zoomed in
        if (animatedScale > 1.05f) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Text(
                    text = String.format("%.1fx", animatedScale),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38EF7D),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZappImageApp() {
    val context = LocalContext.current
    val imageItems = remember { mutableStateListOf<ImageItem>() }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf("Scanning storage...") }
    var isFillMode by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { imageItems.size })
    var currentZoomScale by remember { mutableFloatStateOf(1f) }

    // Keep selectedIndex in sync with pagerState
    LaunchedEffect(pagerState.currentPage) {
        if (imageItems.isNotEmpty() && pagerState.currentPage in imageItems.indices) {
            selectedIndex = pagerState.currentPage
            currentZoomScale = 1f
        }
    }

    // Keep pagerState in sync when selectedIndex changes
    LaunchedEffect(selectedIndex) {
        if (imageItems.isNotEmpty() && selectedIndex in imageItems.indices && pagerState.currentPage != selectedIndex) {
            pagerState.scrollToPage(selectedIndex)
            currentZoomScale = 1f
        }
    }

    // Custom Coil ImageLoader configured for animated GIF & static JPG/PNG
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    // Function to scan local image directories (/sdcard/zapp and /sdcard/zorapp)
    fun scanZappDirectory() {
        imageItems.clear()
        val dirs = listOf(
            File(Environment.getExternalStorageDirectory(), "zapp"),
            File(Environment.getExternalStorageDirectory(), "zorapp")
        )
        dirs.firstOrNull()?.let { if (!it.exists()) it.mkdirs() }

        val foundFiles = mutableListOf<File>()
        dirs.forEach { dir ->
            if (dir.exists()) {
                val files = dir.listFiles { file ->
                    val ext = file.extension.lowercase()
                    ext in listOf("jpg", "jpeg", "png", "gif")
                }
                if (files != null) foundFiles.addAll(files)
            }
        }

        if (foundFiles.isNotEmpty()) {
            foundFiles.distinctBy { it.name.lowercase() }.sortedBy { it.name.lowercase() }.forEach { file ->
                imageItems.add(
                    ImageItem(
                        name = file.name,
                        source = file,
                        sizeBytes = file.length()
                    )
                )
            }
            statusMessage = "Loaded ${imageItems.size} image(s)"
            if (selectedIndex >= imageItems.size) {
                selectedIndex = 0
            }
        } else {
            statusMessage = "No images found. Add JPG/GIF/PNG or pick a file."
        }
    }

    // Storage permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        scanZappDirectory()
    }

    // System file picker for JPG/PNG/GIF
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = queryFileName(context, uri) ?: "Picked_Image_${System.currentTimeMillis()}"
            val newItem = ImageItem(
                name = fileName,
                source = uri,
                isPicked = true
            )
            imageItems.add(0, newItem)
            selectedIndex = 0
            statusMessage = "Opened: $fileName"
        }
    }

    // Request permissions and scan on launch
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
        scanZappDirectory()
    }

    if (isFullScreen && imageItems.isNotEmpty() && selectedIndex in imageItems.indices) {
        val currentItem = imageItems[selectedIndex]
        BackHandler { isFullScreen = false }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Horizontal Pager for swiping between photos, with double-tap & pinch zoom
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = (currentZoomScale <= 1.05f),
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = imageItems[page]
                ZoomableImageContainer(
                    item = item,
                    imageLoader = imageLoader,
                    isFillMode = isFillMode,
                    onScaleChanged = { currentZoomScale = it }
                )
            }

            // TOP-LEFT: Dedicated tactile "Return to Menu" button (Option 2)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = BorderStroke(1.5.dp, Color(0xFF64748B)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .clickable { isFullScreen = false }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "← Return to Menu",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // TOP-RIGHT: Dedicated Scale Toggle button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = BorderStroke(1.5.dp, Color(0xFF64748B)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .clickable { isFillMode = !isFillMode }
            ) {
                Text(
                    text = if (isFillMode) "Scale: Fill (Crop)" else "Scale: Fit (Full)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFillMode) Color(0xFF38EF7D) else Color(0xFFD0BCFF),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            // CENTER-LEFT: Previous Image Arrow Button
            if (selectedIndex > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, Color(0xFF64748B)),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp)
                        .size(52.dp)
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(selectedIndex - 1)
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "‹",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }

            // CENTER-RIGHT: Next Image Arrow Button
            if (selectedIndex < imageItems.size - 1) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, Color(0xFF64748B)),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp)
                        .size(52.dp)
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(selectedIndex + 1)
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "›",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }

            // BOTTOM-CENTER: Subdued file info caption
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "${currentItem.name}  •  ${selectedIndex + 1} of ${imageItems.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.cat_avatar),
                                    contentDescription = "ZorApp Cat",
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, Color(0xFF6C63FF), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("ZorApp Image Viewer", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }

                            // Air-Gapped Offline Badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Offline ZorApp", fontSize = 11.sp, color = Color(0xFFE2E8F0))
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
                )
            }
        ) { innerPadding ->
            // 2-Column Layout optimized for 800x480 Cadillac CTS & Pixel 11 Pro XL
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF090D16))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // LEFT COLUMN: Directory, Actions, & Image List
                ElevatedCard(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    ) {
                        // Actions row: Pick File + Rescan
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { filePickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                            ) {
                                Text("Pick File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { scanZappDirectory() },
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Rescan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = statusMessage,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Image List
                        if (imageItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No JPG, GIF, or PNG found.\nTap 'Pick File' or copy files to /sdcard/zorapp.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 16.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(imageItems) { index, item ->
                                    val isSelected = index == selectedIndex
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF38475E) else Color(0xFF0F172A))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.dp,
                                                color = if (isSelected) Color(0xFF818CF8) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedIndex = index
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            }
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Thumbnail
                                        AsyncImage(
                                            model = item.source,
                                            imageLoader = imageLoader,
                                            contentDescription = item.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (item.isPicked) "Picked File" else formatFileSize(item.sizeBytes),
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // RIGHT COLUMN: Full-screen Image Viewer
                Card(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    if (imageItems.isNotEmpty() && selectedIndex in imageItems.indices) {
                        val currentItem = imageItems[selectedIndex]
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                        ) {
                            // Image display viewport - Swiping flips photos, clicking opens Fullscreen
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    val item = imageItems[page]
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { isFullScreen = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = item.source,
                                            imageLoader = imageLoader,
                                            contentDescription = item.name,
                                            contentScale = if (isFillMode) ContentScale.Crop else ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                // Tap for Fullscreen chip (top-left)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .clickable { isFullScreen = true }
                                ) {
                                    Text(
                                        text = "⛶ Fullscreen",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }

                                // Quick tactile overlay chip in top-right of the screen
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .clickable { isFillMode = !isFillMode }
                                ) {
                                    Text(
                                        text = if (isFillMode) "Mode: Fill (Tap to Fit)" else "Mode: Fit (Tap to Fill)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isFillMode) Color(0xFF38EF7D) else Color(0xFFD0BCFF),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Controls bar: Previous, Scale Mode, Fullscreen, Name info, Next
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (selectedIndex > 0) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(selectedIndex - 1)
                                            }
                                        }
                                    },
                                    enabled = selectedIndex > 0,
                                    modifier = Modifier.height(44.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Prev", fontSize = 12.sp)
                                }

                                // Dedicated tactile scaling button
                                FilledTonalButton(
                                    onClick = { isFillMode = !isFillMode },
                                    modifier = Modifier.height(44.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isFillMode) "Scale: Fill" else "Scale: Fit",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Dedicated tactile fullscreen button
                                FilledTonalButton(
                                    onClick = { isFullScreen = true },
                                    modifier = Modifier.height(44.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "⛶ Full",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = currentItem.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${selectedIndex + 1} of ${imageItems.size}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (selectedIndex < imageItems.size - 1) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(selectedIndex + 1)
                                            }
                                        }
                                    },
                                    enabled = selectedIndex < imageItems.size - 1,
                                    modifier = Modifier.height(44.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                                ) {
                                    Text("Next", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No Image Selected",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Pick a file or place images in /sdcard/zorapp",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.2f MB", mb)
}

private fun queryFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
    }
    if (name == null) {
        name = uri.path?.let { path ->
            val cut = path.lastIndexOf('/')
            if (cut != -1) path.substring(cut + 1) else path
        }
    }
    return name
}
