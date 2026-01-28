package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.DeepLinkViewModel
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Post
import com.example.fitlifesmarthealthlifestyleapp.utils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SocialFragment : Fragment(R.layout.fragment_social) {

    private lateinit var adapter: PostAdapter
    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    // Dữ liệu bài viết
    private var allPosts: List<Post> = emptyList()

    // Bộ lọc hiện tại
    private var currentFilterType = FilterType.ALL

    // Danh sách hỗ trợ lọc (Block & Following)
    private var hiddenUserIds: MutableSet<String> = mutableSetOf()
    private var followingUserIds: MutableList<String> = mutableListOf()

    // DeepLink & Share
    private lateinit var deepLinkViewModel: DeepLinkViewModel
    private var targetPostId: String? = null
    private var pendingSharePostId: String? = null

    private val shareLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        pendingSharePostId?.let { postId ->
            incrementShareCount(postId)
            pendingSharePostId = null
        }
    }

    // Enum các loại lọc
    enum class FilterType { ALL, MY_POSTS, MY_LIKES, FOLLOWING }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Lấy dữ liệu User (Danh sách chặn & Danh sách Following)
        fetchUserData()

        // Ánh xạ View
        val rvCommunity = view.findViewById<RecyclerView>(R.id.rvCommunity)
        val searchView = view.findViewById<SearchView>(R.id.searchViewPost)
        val btnFilter = view.findViewById<ImageView>(R.id.btnFilter)
        val btnAddPost = view.findViewById<View>(R.id.btnAddPost)
        val btnUserProfile = view.findViewById<ImageView>(R.id.btnUserProfile)
        val btnNotification = view.findViewById<ImageView>(R.id.btnNotification)
        val btnMessage = view.findViewById<ImageView>(R.id.btnMessage)

        // 2. Setup Adapter
        adapter = PostAdapter(
            onLikeClick = { post -> toggleLike(post) },
            onCommentClick = { post -> showCommentsDialog(post) },
            onLikeCountClick = { userIds -> showLikeListDialog(userIds) },

            // [QUAN TRỌNG] Xử lý click User để tránh Crash
            onUserClick = { userId ->
                try {
                    // DÙNG CÁI NÀY ĐỂ TRÁNH CRASH:
                    val transaction = requireActivity().supportFragmentManager.beginTransaction()

                    if (userId == currentUid) {
                        transaction.replace(R.id.navHostFragmentContainerView, PersonalProfileFragment())
                    } else {
                        // Gọi newInstance để truyền ID an toàn
                        transaction.replace(R.id.navHostFragmentContainerView, UserProfileFragment.newInstance(userId))
                    }
                    transaction.addToBackStack(null)
                    transaction.commit()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            onShareClick = { post -> sharePostContent(post) },
            onBlockClick = { userId, userName -> showBlockConfirmation(userId, userName) }
        )

        rvCommunity.layoutManager = LinearLayoutManager(context)
        rvCommunity.adapter = adapter

        // 3. Setup Events Buttons
        btnAddPost.setOnClickListener {
            if (NetworkUtils.checkConnection(requireContext())) {
                CreatePostDialogFragment().show(parentFragmentManager, "CreatePost")
            }
        }

        btnFilter.setOnClickListener { v ->
            showFilterMenu(v, searchView.query.toString())
        }

        // Load Avatar Header
        loadCurrentUserAvatar(btnUserProfile)

        // Click Avatar của mình -> Vào trang cá nhân
        btnUserProfile.setOnClickListener {
            if (currentUid != null) {
                openUserProfile(currentUid)
            }
        }

        btnNotification.setOnClickListener { Toast.makeText(context, "Notifications", Toast.LENGTH_SHORT).show() }
        btnMessage.setOnClickListener { Toast.makeText(context, "Messages", Toast.LENGTH_SHORT).show() }

        // Search Listener
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterPosts(newText, currentFilterType)
                return true
            }
        })

        // 4. ViewModel DeepLink
        deepLinkViewModel = ViewModelProvider(requireActivity())[DeepLinkViewModel::class.java]
        deepLinkViewModel.targetPostId.observe(viewLifecycleOwner) { id ->
            if (id != null) {
                targetPostId = id
                if (allPosts.isNotEmpty()) filterPostsWithDeepLink()
            }
        }

        // 5. Lắng nghe bài viết Realtime
        listenToPosts()
    }

    // --- HÀM ĐIỀU HƯỚNG AN TOÀN ---
    private fun openUserProfile(userId: String) {
        try {
            // Sử dụng requireActivity().supportFragmentManager để thao tác với container của Activity chính
            val transaction = requireActivity().supportFragmentManager.beginTransaction()

            if (userId == currentUid) {
                // Vào trang cá nhân của mình
                transaction.replace(R.id.navHostFragmentContainerView, PersonalProfileFragment())
            } else {
                // Vào trang người khác
                transaction.replace(R.id.navHostFragmentContainerView, UserProfileFragment.newInstance(userId))
            }
            transaction.addToBackStack(null)
            transaction.commit()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to open profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCurrentUserAvatar(imageView: ImageView) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user?.photoUrl != null) {
            Glide.with(this).load(user.photoUrl).circleCrop().into(imageView)
        }
    }

    // --- LẤY DỮ LIỆU USER (BLOCK & FOLLOWING) ---
    private fun fetchUserData() {
        if (currentUid == null) return

        db.collection("users").document(currentUid)
            .addSnapshotListener { document, e ->
                if (e != null || document == null) return@addSnapshotListener

                // 1. Block List (Người mình chặn + Người chặn mình)
                hiddenUserIds.clear()
                val myBlockedList = document.get("blockedUsers") as? List<String>
                if (myBlockedList != null) hiddenUserIds.addAll(myBlockedList)

                val blockedMeList = document.get("blockedBy") as? List<String>
                if (blockedMeList != null) hiddenUserIds.addAll(blockedMeList)

                // 2. Following List (Người mình đang theo dõi)
                followingUserIds.clear()
                val myFollowing = document.get("following") as? List<String>
                if (myFollowing != null) followingUserIds.addAll(myFollowing)

                // Refresh lại list bài viết sau khi có dữ liệu mới
                val searchView = view?.findViewById<SearchView>(R.id.searchViewPost)
                filterPosts(searchView?.query.toString(), currentFilterType)
            }
    }

    // --- LOGIC LỌC BÀI VIẾT ---
    private fun filterPosts(query: String?, filterType: FilterType) {
        val searchText = query?.lowercase()?.trim() ?: ""

        val filteredList = allPosts.filter { post ->
            // 1. Lọc theo tên người đăng hoặc caption
            val matchText = if (searchText.isEmpty()) true else
                (post.userName.lowercase().contains(searchText) || post.caption.lowercase().contains(searchText))

            // 2. Lọc theo Loại
            val matchFilter = when (filterType) {
                FilterType.ALL -> true
                FilterType.MY_POSTS -> post.userId == currentUid
                FilterType.MY_LIKES -> post.likedBy.contains(currentUid)
                FilterType.FOLLOWING -> followingUserIds.contains(post.userId) // [MỚI]
            }

            // 3. Ẩn bài của người bị chặn
            val isNotHidden = !hiddenUserIds.contains(post.userId)

            matchText && matchFilter && isNotHidden
        }
        adapter.submitList(filteredList)
    }

    private fun showFilterMenu(anchor: View, currentQuery: String) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "All Posts")
        popup.menu.add(0, 2, 0, "My Posts")
        popup.menu.add(0, 3, 0, "Liked by Me")
        popup.menu.add(0, 4, 0, "Following") // [MỚI] Option Following

        popup.setOnMenuItemClickListener { item ->
            currentFilterType = when (item.itemId) {
                2 -> FilterType.MY_POSTS
                3 -> FilterType.MY_LIKES
                4 -> FilterType.FOLLOWING
                else -> FilterType.ALL
            }
            filterPosts(currentQuery, currentFilterType)

            // Đổi màu icon Filter để biết đang lọc
            val iconColor = if (currentFilterType == FilterType.ALL) R.color.black else R.color.orange_primary
            if (anchor is ImageView) {
                anchor.setColorFilter(resources.getColor(iconColor, null))
            }
            true
        }
        popup.show()
    }

    // --- CÁC HÀM KHÁC (LOAD BÀI, LIKE, SHARE...) ---
    private fun listenToPosts() {
        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                allPosts = snapshot?.toObjects(Post::class.java) ?: emptyList()

                if (targetPostId != null) {
                    filterPostsWithDeepLink()
                } else {
                    val searchView = view?.findViewById<SearchView>(R.id.searchViewPost)
                    filterPosts(searchView?.query.toString(), currentFilterType)
                }
            }
    }

    private fun filterPostsWithDeepLink() {
        val id = targetPostId ?: return
        val targetPost = allPosts.find { it.postId == id }
        if (targetPost != null) {
            adapter.submitList(listOf(targetPost))
            deepLinkViewModel.clearPostId()
            targetPostId = null
        } else {
            if (allPosts.isNotEmpty()) {
                deepLinkViewModel.clearPostId()
                targetPostId = null
                filterPosts("", currentFilterType)
            }
        }
    }

    private fun sharePostContent(post: Post) {
        if (!NetworkUtils.checkConnection(requireContext())) return
        pendingSharePostId = post.postId
        val deepLink = "https://fit-life-app-dl.vercel.app/post/${post.postId}"
        val shareMessage = "${post.caption}\n$deepLink"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            type = "text/plain"
        }
        shareLauncher.launch(Intent.createChooser(sendIntent, "Share via"))
    }

    private fun incrementShareCount(postId: String) {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) return
        db.collection("posts").document(postId).update("shareCount", FieldValue.increment(1))
    }

    private fun toggleLike(post: Post) {
        if (currentUid == null || !NetworkUtils.checkConnection(requireContext())) return
        val postRef = db.collection("posts").document(post.postId)
        if (post.likedBy.contains(currentUid)) {
            postRef.update("likeCount", FieldValue.increment(-1), "likedBy", FieldValue.arrayRemove(currentUid))
        } else {
            postRef.update("likeCount", FieldValue.increment(1), "likedBy", FieldValue.arrayUnion(currentUid))
        }
    }

    private fun showCommentsDialog(post: Post) {
        if (NetworkUtils.checkConnection(requireContext())) {
            val dialog = CommentsDialogFragment()
            val bundle = Bundle()
            bundle.putString("postId", post.postId)
            dialog.arguments = bundle
            dialog.show(parentFragmentManager, "CommentsDialog")
        }
    }

    private fun showLikeListDialog(userIds: List<String>) {
        if (userIds.isNotEmpty()) {
            val dialog = LikeListDialogFragment(userIds)
            dialog.show(childFragmentManager, "LikeListDialog")
        }
    }

    private fun showBlockConfirmation(userId: String, userName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Block User")
            .setMessage("Block $userName? Their posts will be hidden.")
            .setPositiveButton("Block") { _, _ -> performBlockUser(userId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performBlockUser(targetUserId: String) {
        if (!NetworkUtils.checkConnection(requireContext()) || currentUid == null) return
        val batch = db.batch()
        val myRef = db.collection("users").document(currentUid)
        batch.update(myRef, "blockedUsers", FieldValue.arrayUnion(targetUserId))
        val targetRef = db.collection("users").document(targetUserId)
        batch.update(targetRef, "blockedBy", FieldValue.arrayUnion(currentUid))
        batch.commit()
            .addOnSuccessListener { Toast.makeText(context, "Blocked user", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(context, "Error blocking", Toast.LENGTH_SHORT).show() }
    }
}