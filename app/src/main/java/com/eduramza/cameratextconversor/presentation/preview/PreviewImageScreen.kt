package com.eduramza.cameratextconversor.presentation.preview

import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.eduramza.cameratextconversor.R
import com.eduramza.cameratextconversor.data.analytics.ConstantsAnalytics.Companion
import com.eduramza.cameratextconversor.data.analytics.ConstantsAnalytics.Companion.CONTENT_BUTTON
import com.eduramza.cameratextconversor.data.analytics.FirebaseAnalyticsLogger
import com.eduramza.cameratextconversor.data.analytics.FirebaseAnalyticsLoggerImpl
import com.eduramza.cameratextconversor.loadBitmap
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewImageScreen(
    imageUri: List<Uri>,
    shouldShowActions: Boolean,
    navigateBack: () -> Unit,
    navigateToAnalyzer: (uri: List<Uri>) -> Unit
) {

    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var padding by remember { mutableStateOf(PaddingValues()) }

    var firstVisibleItemIndex by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()

    val analytics: FirebaseAnalyticsLogger = FirebaseAnalyticsLoggerImpl()
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex
        }.collectLatest {
            firstVisibleItemIndex = it
        }
    }

    LaunchedEffect(imageUri) {
        bitmap = imageUri.map {
            loadBitmap(context, it)
        }
    }

    val cropActivityResultLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                navigateToAnalyzer(listOf(uri))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = { Text(text = stringResource(id = R.string.title_preview)) },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            analytics.trackSelectContent(
                                id = Companion.Preview.ID_BACK,
                                itemName = Companion.Preview.ITEM_NAME_BACK,
                                contentType = CONTENT_BUTTON,
                                area = Companion.Preview.AREA
                            )
                        }
                        navigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        padding = paddingValues
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(imageUri) { _, item: Uri ->
                    AsyncImage(
                        model = item,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (shouldShowActions) {
                scope.launch {
                    analytics.trackScreenView(
                        screenName = Companion.Preview.SCREEN_NAME,
                        area = Companion.Preview.AREA
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .align(Alignment.BottomCenter)
                        .height(100.dp)
                ) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                analytics.trackSelectContent(
                                    id = Companion.Preview.ID_CROP,
                                    itemName = Companion.Preview.ITEM_NAME_CROP,
                                    contentType = CONTENT_BUTTON,
                                    area = Companion.Preview.AREA
                                )
                            }
                            launchCropActivity(
                                imageUri[firstVisibleItemIndex],
                                cropActivityResultLauncher
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .background(color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = stringResource(id = R.string.button_crop_image),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                analytics.trackSelectContent(
                                    id = Companion.Preview.ID_ANALYZE,
                                    itemName = Companion.Preview.ITEM_NAME_ANALYZE,
                                    contentType = CONTENT_BUTTON,
                                    area = Companion.Preview.AREA
                                )
                            }
                            navigateToAnalyzer(imageUri)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .background(color = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = stringResource(id = R.string.button_analyzer_image),
                            fontSize = MaterialTheme.typography.titleMedium.fontSize
                        )
                    }

                }
            } else {
                scope.launch {
                    analytics.trackScreenView(
                        screenName = Companion.Preview.SCREEN_NAME,
                        area = Companion.Preview.AREA
                    )
                }
            }
        }
    }
}

fun launchCropActivity(
    imageUri: Uri,
    launcher: ManagedActivityResultLauncher<CropImageContractOptions, CropImageView.CropResult>
) {
    val cropOptions = CropImageContractOptions(imageUri, CropImageOptions())
    launcher.launch(cropOptions)
}

@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun previewEditImageScreen() {
    PreviewImageScreen(
        imageUri = listOf(Uri.parse("")),
        shouldShowActions = false,
        navigateBack = {},
        navigateToAnalyzer = {}
    )
}