package com.libra.app.feature.write.create

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libra.app.core.di.ServiceLocator
import com.libra.app.domain.model.Book
import com.libra.app.ui.theme.LibraLavender
import com.libra.app.ui.theme.LibraLavenderBorder
import com.libra.app.ui.theme.LibraLavenderSurface
import com.libra.app.ui.theme.LibraLavenderText
import com.libra.app.ui.animation.appleBouncy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBookScreen(
    onBack: () -> Unit,
    onBookCreated: (Book) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateBookViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val authorName = remember {
        ServiceLocator.authRepository.currentUser.value?.let { it.displayName.ifBlank { it.username } }.orEmpty()
    }

    LaunchedEffect(Unit) {
        viewModel.attachDraftStorage(SharedPreferencesBookDraftStorage(context))
    }

    // Scroll to top whenever step changes
    LaunchedEffect(uiState.currentStep) {
        scrollState.animateScrollTo(0)
    }

    // Android System Back Button Handling
    BackHandler {
        when {
            uiState.currentStep > 1 -> viewModel.previousStep()
            uiState.hasAnyContent -> viewModel.setDiscardConfirmDialog(true)
            else -> onBack()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Yeni Kitap Oluştur",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Yazar Stüdyosu",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    val onNavBack: () -> Unit = {
                        when {
                            uiState.currentStep > 1 -> viewModel.previousStep()
                            uiState.hasAnyContent -> viewModel.setDiscardConfirmDialog(true)
                            else -> onBack()
                        }
                    }
                    IconButton(
                        onClick = onNavBack,
                        modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onNavBack)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveAsDraft(onBookCreated) },
                        enabled = !uiState.isSavingDraft && !uiState.isPublishing,
                        modifier = Modifier.appleBouncy(
                            enabled = !uiState.isSavingDraft && !uiState.isPublishing,
                            pressedScale = 0.92f,
                            onClick = { viewModel.saveAsDraft(onBookCreated) }
                        )
                    ) {
                        if (uiState.isSavingDraft) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.BookmarkBorder, null, Modifier.size(18.dp), tint = LibraLavenderText)
                            Spacer(Modifier.width(4.dp))
                            Text("Taslağı Kaydet", color = LibraLavenderText, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (uiState.currentStep < 4) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.currentStep > 1) {
                            OutlinedButton(
                                onClick = viewModel::previousStep,
                                modifier = Modifier.appleBouncy(
                                    pressedScale = 0.93f,
                                    onClick = viewModel::previousStep
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Önceki")
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        val canProceed = when (uiState.currentStep) {
                            1 -> uiState.canProceedFromStep1
                            2 -> uiState.canProceedFromStep2
                            else -> true
                        }

                        Button(
                            onClick = viewModel::nextStep,
                            enabled = canProceed,
                            modifier = Modifier.appleBouncy(
                                enabled = canProceed,
                                pressedScale = 0.93f,
                                onClick = viewModel::nextStep
                            ),
                            colors = ButtonDefaults.buttonColors(containerColor = LibraLavender),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("İleri", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Stepper
            CreateBookStepper(
                currentStep = uiState.currentStep,
                onStepClick = viewModel::setStep
            )

            // Restored Draft Notice Banner
            AnimatedVisibility(visible = uiState.hasUnsavedLocalDraft) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = LibraLavenderSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LibraLavenderBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Kaydedilmiş Taslak Bulundu",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = LibraLavenderText
                            )
                            Text(
                                text = "Daha önce üzerinde çalıştığın kitaba devam etmek ister misin?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row {
                            TextButton(onClick = viewModel::restoreDraft) {
                                Text("Devam Et", fontWeight = FontWeight.Bold, color = LibraLavenderText)
                            }
                            TextButton(onClick = viewModel::discardLocalDraft) {
                                Text("Sıfırla", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Error Message Banner
            uiState.errorMessage?.let { error ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::clearError, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "Kapat", Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Scrollable Step Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                        } else {
                            slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                        }
                    },
                    label = "stepAnimation"
                ) { step ->
                    when (step) {
                        1 -> CreateBookStage1BasicInfo(
                            uiState = uiState,
                            onTitleChange = viewModel::updateTitle,
                            onDescriptionChange = viewModel::updateDescription,
                            onDiscoverySummaryChange = viewModel::updateDiscoverySummary,
                            onCategoryChange = viewModel::updateCategory,
                            onSetCover = viewModel::setCoverImage,
                            onRemoveCover = viewModel::removeCoverImage,
                            onOpenAiDialog = viewModel::setAiCoverPromptDialog
                        )
                        2 -> CreateBookStage2Chapters(
                            uiState = uiState,
                            onAddChapter = viewModel::addChapter,
                            onUpdateChapter = viewModel::updateChapter,
                            onDeleteChapter = viewModel::deleteChapter,
                            onMoveUp = viewModel::moveChapterUp,
                            onMoveDown = viewModel::moveChapterDown,
                            onSetEditingChapterId = viewModel::setEditingChapterId
                        )
                        3 -> CreateBookStage3Details(
                            uiState = uiState,
                            onAddTag = viewModel::addTag,
                            onRemoveTag = viewModel::removeTag,
                            onLanguageChange = viewModel::updateLanguage,
                            onGenreChange = viewModel::updateGenre,
                            onAudienceChange = viewModel::updateTargetAudience,
                            onToggleContentWarning = viewModel::toggleContentWarning,
                            onSeriesChange = viewModel::updateSeries
                        )
                        4 -> CreateBookStage4Publish(
                            uiState = uiState,
                            authorName = authorName,
                            onVisibilityChange = viewModel::updateVisibility,
                            onSaveDraft = { viewModel.saveAsDraft(onBookCreated) },
                            onPublish = { viewModel.publishBook(onBookCreated) }
                        )
                    }
                }

                Spacer(Modifier.height(36.dp))
            }
        }
    }

    // Discard Confirmation Dialog
    if (uiState.showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setDiscardConfirmDialog(false) },
            title = { Text("Yazıdan Çıkılsın mı?") },
            text = {
                Text(
                    "Oluşturmakta olduğun kitap henüz yayınlanmadı. " +
                        "Taslağını cihazında saklayabilir veya tamamen silebilirsin."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setDiscardConfirmDialog(false)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LibraLavender)
                ) {
                    Text("Taslağı Sakla ve Çık")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.discardLocalDraft()
                            viewModel.setDiscardConfirmDialog(false)
                            onBack()
                        }
                    ) {
                        Text("Taslağı Sil", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { viewModel.setDiscardConfirmDialog(false) }) {
                        Text("Yazmaya Devam Et")
                    }
                }
            }
        )
    }
}
