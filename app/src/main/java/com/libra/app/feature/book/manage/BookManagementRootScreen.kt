package com.libra.app.feature.book.manage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libra.app.domain.model.Book

@Composable
fun BookManagementRootScreen(
    book: Book,
    onBack: () -> Unit,
    onPreviewBook: (Book) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onOpenProfile: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: BookManagementViewModel = viewModel(key = "manage-book-${book.id}")
    val state by viewModel.state.collectAsState()

    when (state.activeSubPage) {
        ManagementSubPage.HUB -> {
            BookManagementHubScreen(
                book = book,
                viewModel = viewModel,
                onBack = onBack,
                onPreviewBook = onPreviewBook,
                onDeleteBook = onDeleteBook,
                modifier = modifier
            )
        }
        ManagementSubPage.METADATA -> {
            BookMetadataEditScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSubPage(ManagementSubPage.HUB) },
                modifier = modifier
            )
        }
        ManagementSubPage.CHAPTERS -> {
            ChapterManagementScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSubPage(ManagementSubPage.HUB) },
                onOpenStudio = { chapter -> viewModel.selectChapterForStudio(chapter) },
                modifier = modifier
            )
        }
        ManagementSubPage.STUDIO -> {
            WritingStudioScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSubPage(ManagementSubPage.HUB) },
                modifier = modifier
            )
        }
        ManagementSubPage.PUBLISHING, ManagementSubPage.PRIVACY -> {
            PublishingSettingsScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSubPage(ManagementSubPage.HUB) },
                modifier = modifier
            )
        }
        ManagementSubPage.ANALYTICS -> {
            BookAnalyticsScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSubPage(ManagementSubPage.HUB) },
                modifier = modifier
            )
        }
        ManagementSubPage.COMMENTS -> {
            BookCommentsModerationScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSubPage(ManagementSubPage.HUB) },
                onOpenProfile = onOpenProfile,
                modifier = modifier
            )
        }
        ManagementSubPage.DRAFTS -> {
            BookDraftsScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSubPage(ManagementSubPage.HUB) },
                modifier = modifier
            )
        }
    }
}
