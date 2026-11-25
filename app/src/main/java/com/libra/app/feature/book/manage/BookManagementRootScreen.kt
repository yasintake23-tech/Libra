package com.libra.app.feature.book.manage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    initialSubPage: ManagementSubPage = ManagementSubPage.HUB,
    modifier: Modifier = Modifier
) {
    val viewModel: BookManagementViewModel = viewModel(key = "manage-book-${book.id}")
    val state by viewModel.state.collectAsState()

    LaunchedEffect(book.id, initialSubPage) {
        viewModel.initialize(book)
        viewModel.navigateToSubPage(initialSubPage)
    }

    val handleSubPageBack: (ManagementSubPage) -> Unit = { current ->
        when {
            current == ManagementSubPage.STUDIO && initialSubPage == ManagementSubPage.CHAPTERS -> {
                viewModel.navigateToSubPage(ManagementSubPage.CHAPTERS)
            }
            initialSubPage != ManagementSubPage.HUB -> {
                onBack()
            }
            else -> {
                viewModel.navigateToSubPage(ManagementSubPage.HUB)
            }
        }
    }

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
                onBack = { handleSubPageBack(ManagementSubPage.METADATA) },
                modifier = modifier
            )
        }
        ManagementSubPage.CHAPTERS -> {
            ChapterManagementScreen(
                viewModel = viewModel,
                onBack = { handleSubPageBack(ManagementSubPage.CHAPTERS) },
                onOpenStudio = { chapter -> viewModel.selectChapterForStudio(chapter) },
                modifier = modifier
            )
        }
        ManagementSubPage.STUDIO -> {
            WritingStudioScreen(
                viewModel = viewModel,
                onBack = { handleSubPageBack(ManagementSubPage.STUDIO) },
                modifier = modifier
            )
        }
        ManagementSubPage.PUBLISHING, ManagementSubPage.PRIVACY -> {
            PublishingSettingsScreen(
                viewModel = viewModel,
                onBack = { handleSubPageBack(ManagementSubPage.PUBLISHING) },
                modifier = modifier
            )
        }
        ManagementSubPage.ANALYTICS -> {
            BookAnalyticsScreen(
                viewModel = viewModel,
                onBack = { handleSubPageBack(ManagementSubPage.ANALYTICS) },
                modifier = modifier
            )
        }
        ManagementSubPage.COMMENTS -> {
            BookCommentsModerationScreen(
                viewModel = viewModel,
                onBack = { handleSubPageBack(ManagementSubPage.COMMENTS) },
                onOpenProfile = onOpenProfile,
                modifier = modifier
            )
        }
        ManagementSubPage.DRAFTS -> {
            BookDraftsScreen(
                viewModel = viewModel,
                onBack = { handleSubPageBack(ManagementSubPage.DRAFTS) },
                modifier = modifier
            )
        }
    }
}
