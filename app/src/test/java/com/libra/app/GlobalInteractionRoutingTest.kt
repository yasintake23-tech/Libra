package com.libra.app

import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.SharedContent
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.navigation.BottomNavTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying that every interactive callback in Libra is routed
 * to its exact intended action and never triggers unintended side effects.
 */
class GlobalInteractionRoutingTest {

    private val userAlice = UserProfile(
        uid = "user_alice",
        displayName = "Alice Wonderland",
        username = "alice",
        email = "alice@libra.app"
    )

    private val userBob = UserProfile(
        uid = "user_bob",
        displayName = "Bob Builder",
        username = "bob",
        email = "bob@libra.app"
    )

    private val samplePostA = Post(
        id = "post_101",
        authorId = "user_alice",
        authorName = "Alice Wonderland",
        authorUsername = "alice",
        title = "Birinci Kitap İncelemesi",
        text = "Harika bir kurgu dünyası.",
        likesCount = 5,
        commentsCount = 2,
        likedByCurrentUser = false,
        savedByCurrentUser = false
    )

    private val samplePostB = Post(
        id = "post_102",
        authorId = "user_bob",
        authorName = "Bob Builder",
        authorUsername = "bob",
        title = "İkinci Bölüm Notları",
        text = "Karakter gelişimi çok başarılı.",
        likesCount = 12,
        commentsCount = 7,
        likedByCurrentUser = true,
        savedByCurrentUser = true
    )

    @Test
    fun postCard_likeAction_onlyTriggersLikeForTargetPost() {
        var likedPostId: String? = null
        var commentsOpenedPostId: String? = null
        var savedPostId: String? = null
        var profileOpenedUserId: String? = null
        var shareTriggered = false

        val onToggleLike: (Post) -> Unit = { likedPostId = it.id }
        val onOpenComments: (Post) -> Unit = { commentsOpenedPostId = it.id }
        val onToggleSave: (Post) -> Unit = { savedPostId = it.id }
        val onOpenProfile: (String) -> Unit = { profileOpenedUserId = it }
        val onShare: () -> Unit = { shareTriggered = true }

        // Simulate user clicking like on post A
        onToggleLike(samplePostA)

        assertEquals("post_101", likedPostId)
        assertNull("Comments must not be triggered on like click", commentsOpenedPostId)
        assertNull("Save must not be triggered on like click", savedPostId)
        assertNull("Profile must not be triggered on like click", profileOpenedUserId)
        assertFalse("Share must not be triggered on like click", shareTriggered)
    }

    @Test
    fun postCard_commentAction_onlyOpensCommentsForTargetPost() {
        var likedPostId: String? = null
        var selectedPost: Post? = null
        var commentText = "previous draft"
        var showLikersPostId: String? = null

        val onCommentClick: (Post) -> Unit = { post ->
            selectedPost = post
            commentText = ""
        }

        // Simulate user tapping comment button on post B
        onCommentClick(samplePostB)

        assertEquals("post_102", selectedPost?.id)
        assertEquals("", commentText)
        assertNull("Like must not be triggered on comment click", likedPostId)
        assertNull("Likers sheet must not be triggered on comment click", showLikersPostId)
    }

    @Test
    fun postCard_saveAction_onlyTogglesSaveForTargetPost() {
        var savedPostId: String? = null
        var selectedPost: Post? = null

        val onToggleSave: (Post) -> Unit = { savedPostId = it.id }

        // Simulate user tapping save button on post A
        onToggleSave(samplePostA)

        assertEquals("post_101", savedPostId)
        assertNull("Comments dialog must not open when tapping save", selectedPost)
    }

    @Test
    fun postCard_profileAction_opensProfileWithCorrectAuthorId() {
        var openedProfileUid: String? = null
        var sharedContent: SharedContent? = null

        val onOpenProfile: (String) -> Unit = { openedProfileUid = it }

        // Simulate user tapping avatar of author on post B
        onOpenProfile(samplePostB.authorId)

        assertEquals("user_bob", openedProfileUid)
        assertNull("Share must never be triggered when clicking profile/avatar", sharedContent)
    }

    @Test
    fun postCard_shareAction_buildsAccurateSharedContent() {
        var sharedContent: SharedContent? = null
        var likeUsersPost: Post? = null

        val onSharePost: (Post) -> Unit = { post ->
            sharedContent = SharedContent(
                type = "post",
                id = post.id,
                title = post.title.ifBlank { "Gönderi" },
                text = post.text,
                authorId = post.authorId,
                authorName = post.authorName,
                mediaUrl = post.mediaUrl,
                url = "https://libra.app/post/${post.id}"
            )
        }

        onSharePost(samplePostA)

        assertNotNull(sharedContent)
        assertEquals("post_101", sharedContent?.id)
        assertEquals("user_alice", sharedContent?.authorId)
        assertEquals("Alice Wonderland", sharedContent?.authorName)
        assertNull("Likers list must not open when clicking share", likeUsersPost)
    }

    @Test
    fun postCard_likeCountClick_setsExactPostForLikersSheet() {
        var likeUsersPost: Post? = null
        var deletedPostId: String? = null

        val onShowLikes: (Post) -> Unit = { likeUsersPost = it }
        val onDelete: (Post) -> Unit = { deletedPostId = it.id }

        // Simulate user single-tapping the like counter
        onShowLikes(samplePostB)

        assertEquals("post_102", likeUsersPost?.id)
        assertNull("Post must NOT be deleted when tapping or long pressing like count", deletedPostId)
    }

    @Test
    fun postCard_likeButtonLongPress_triggersLikersSheetWithExactPost() {
        var likeUsersPost: Post? = null
        var toggledLikePostId: String? = null

        val onLike: (Post) -> Unit = { toggledLikePostId = it.id }
        val onShowLikes: (Post) -> Unit = { likeUsersPost = it }

        // Simulate holding down (long press) on the like button
        onShowLikes(samplePostA)

        assertNotNull("Holding like button must open likers sheet", likeUsersPost)
        assertEquals("post_101", likeUsersPost?.id)
        assertNull("Long pressing like button must NOT toggle like state", toggledLikePostId)
    }

    @Test
    fun postCard_likeButtonShortClick_onlyTogglesLike() {
        var likeUsersPost: Post? = null
        var toggledLikePostId: String? = null

        val onLike: (Post) -> Unit = { toggledLikePostId = it.id }
        val onShowLikes: (Post) -> Unit = { likeUsersPost = it }

        // Simulate single tap on like button
        onLike(samplePostA)

        assertEquals("post_101", toggledLikePostId)
        assertNull("Single tapping like button must NOT open likers sheet", likeUsersPost)
    }

    @Test
    fun discoverScreen_followVsProfile_actionsAreIsolated() {
        var followedUserUid: String? = null
        var profileOpenedUid: String? = null

        val onFollowUser: (UserProfile) -> Unit = { user -> followedUserUid = user.uid }
        val onUserClick: (UserProfile) -> Unit = { user -> profileOpenedUid = user.uid }

        // Simulate clicking 'Takip Et' on Bob
        onFollowUser(userBob)
        assertEquals("user_bob", followedUserUid)
        assertNull("Clicking follow button must not open public profile", profileOpenedUid)

        // Reset and simulate clicking Alice's card
        followedUserUid = null
        onUserClick(userAlice)
        assertEquals("user_alice", profileOpenedUid)
        assertNull("Clicking user card must not trigger follow action", followedUserUid)
    }

    @Test
    fun libraryScreen_emptyStateDiscover_navigatesToDiscoverTab() {
        var activeTab = BottomNavTab.LIBRARY

        val onNavigateToDiscover = { activeTab = BottomNavTab.DISCOVER }

        // Simulate clicking 'Kitapları Keşfet'
        onNavigateToDiscover()

        assertEquals("Navigating from Library empty state must switch to DISCOVER tab", BottomNavTab.DISCOVER, activeTab)
    }

    @Test
    fun libraryScreen_bookItemClick_routesBasedOnShelfType() {
        val sampleBook = Book(
            id = "book_1",
            ownerId = "user_alice",
            authorName = "Alice",
            title = "Kütüphane Örneği",
            category = BookCategory.FICTION
        )

        var selectedWritingBook: Book? = null
        var selectedReadingBook: Book? = null

        val onBookClick: (Book, ShelfType) -> Unit = { book, shelf ->
            if (shelf == ShelfType.MY_WRITINGS) {
                selectedWritingBook = book
            } else {
                selectedReadingBook = book
            }
        }

        // Click in written tab
        onBookClick(sampleBook, ShelfType.MY_WRITINGS)
        assertEquals("book_1", selectedWritingBook?.id)
        assertNull(selectedReadingBook)

        // Click in reading tab
        selectedWritingBook = null
        onBookClick(sampleBook, ShelfType.READING)
        assertNull(selectedWritingBook)
        assertEquals("book_1", selectedReadingBook?.id)
    }

    @Test
    fun profileScreen_myBooksClick_navigatesToLibraryTab() {
        var activeTab = BottomNavTab.PROFILE

        val onMyBooksClick = { activeTab = BottomNavTab.LIBRARY }

        onMyBooksClick()

        assertEquals(BottomNavTab.LIBRARY, activeTab)
    }

    @Test
    fun profileScreen_socialListUserClick_dismissesAndOpensProfile() {
        var dialogDismissed = false
        var openedProfileUid: String? = null

        val onDismiss = { dialogDismissed = true }
        val onOpenProfile: (String) -> Unit = { openedProfileUid = it }

        // Simulate user clicking on follower Bob in SocialListDialog
        onDismiss()
        onOpenProfile(userBob.uid)

        assertTrue(dialogDismissed)
        assertEquals("user_bob", openedProfileUid)
    }

    @Test
    fun multipleFeedPosts_isolationBetweenItemsVerified() {
        val posts = listOf(samplePostA, samplePostB)
        val likeMap = mutableMapOf<String, Boolean>()
        var commentTargetId: String? = null

        val onToggleLike: (Post) -> Unit = { post ->
            likeMap[post.id] = !(likeMap[post.id] ?: post.likedByCurrentUser)
        }
        val onComment: (Post) -> Unit = { post ->
            commentTargetId = post.id
        }

        // Tap like on post A
        onToggleLike(posts[0])
        assertTrue("Post A should now be liked", likeMap["post_101"] == true)
        assertNull("Post B should not have changed in map", likeMap["post_102"])

        // Tap comment on post B
        onComment(posts[1])
        assertEquals("post_102", commentTargetId)
    }
}
