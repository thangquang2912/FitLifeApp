package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    private var allPosts: List<Post> = emptyList()
    private var currentFilterType = FilterType.ALL

    // Danh sách ID những người dùng bị chặn
    private var blockedUserIds: MutableList<String> = mutableListOf()

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
    private var hiddenUserIds: MutableSet<String> = mutableSetOf()
    enum class FilterType { ALL, MY_POSTS, MY_LIKES }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Lấy danh sách chặn trước
        fetchBlockedUsers()

        // Ánh xạ View
        val rvCommunity = view.findViewById<RecyclerView>(R.id.rvCommunity)
        val searchView = view.findViewById<SearchView>(R.id.searchViewPost)
        val btnFilter = view.findViewById<ImageView>(R.id.btnFilter)
        val btnAddPost = view.findViewById<ImageView>(R.id.btnAddPost)
        val btnUserProfile = view.findViewById<ImageView>(R.id.btnUserProfile) // Đã ánh xạ ở đây
        // val btnNotification... (các nút khác)

        // 2. Setup Adapter
        adapter = PostAdapter(
            onLikeClick = { post -> toggleLike(post) },
            onCommentClick = { post ->
                if (NetworkUtils.checkConnection(requireContext())) {
                    val dialog = CommentsDialogFragment()
                    val bundle = Bundle()
                    bundle.putString("postId", post.postId)
                    dialog.arguments = bundle
                    dialog.show(parentFragmentManager, "CommentsDialog")
                }
            },
            onLikeCountClick = { userIds ->
                if (userIds.isNotEmpty()) {
                    val dialog = LikeListDialogFragment(userIds)
                    dialog.show(childFragmentManager, "LikeListDialog")
                }
            },
            onUserClick = { userId -> },
            onShareClick = { post -> sharePostContent(post) },
            onBlockClick = { userId, userName ->
                showBlockConfirmation(userId, userName)
            }
        )

        rvCommunity.layoutManager = LinearLayoutManager(context)
        rvCommunity.adapter = adapter

        // 3. Setup Events

        // Nút Đăng bài
        btnAddPost.setOnClickListener {
            if (NetworkUtils.checkConnection(requireContext())) {
                val dialog = CreatePostDialogFragment()
                dialog.show(parentFragmentManager, "CreatePost")
            }
        }

        // Nút Filter
        btnFilter.setOnClickListener { view ->
            showFilterMenu(view, searchView.query.toString())
        }

        // --- SỬA LỖI Ở ĐÂY ---
        // Load avatar
        loadCurrentUserAvatar(btnUserProfile)

        // Sự kiện Click User Profile
        btnUserProfile.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.navHostFragmentContainerView, PersonalProfileFragment()) // ID chính xác từ file xml bạn gửi
                .addToBackStack(null)
                .commit()
        }

        // Search
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
                if (allPosts.isNotEmpty()) {
                    filterPostsWithDeepLink()
                }
            }
        }

        // 5. Lắng nghe bài viết
        listenToPosts()
    }

    private fun loadCurrentUserAvatar(imageView: ImageView) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user?.photoUrl != null) {
            Glide.with(this).load(user.photoUrl).circleCrop().into(imageView)
        }
    }

    // --- LOGIC BLOCK USER ---
    private fun fetchBlockedUsers() {
        if (currentUid == null) return

        db.collection("users").document(currentUid)
            .addSnapshotListener { document, e ->
                if (e != null) return@addSnapshotListener

                hiddenUserIds.clear()

                // Lấy danh sách mình chặn
                val myBlockedList = document?.get("blockedUsers") as? List<String>
                if (myBlockedList != null) hiddenUserIds.addAll(myBlockedList)

                // [MỚI] Lấy danh sách người chặn mình
                val blockedMeList = document?.get("blockedBy") as? List<String>
                if (blockedMeList != null) hiddenUserIds.addAll(blockedMeList)

                // Refresh lại giao diện
                val searchView = view?.findViewById<SearchView>(R.id.searchViewPost)
                filterPosts(searchView?.query.toString(), currentFilterType)
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

        // A. Thêm Target vào danh sách 'blockedUsers' của Mình
        val myRef = db.collection("users").document(currentUid)
        batch.update(myRef, "blockedUsers", FieldValue.arrayUnion(targetUserId))

        // B. Thêm Mình vào danh sách 'blockedBy' của Target
        val targetRef = db.collection("users").document(targetUserId)
        batch.update(targetRef, "blockedBy", FieldValue.arrayUnion(currentUid))

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(context, "Đã chặn người dùng. Họ sẽ không thấy bài viết của bạn nữa.", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- LOGIC POSTS & FILTER ---
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

    private fun filterPosts(query: String?, filterType: FilterType) {
        val searchText = query?.lowercase()?.trim() ?: ""

        val filteredList = allPosts.filter { post ->
            val matchName = if (searchText.isEmpty()) true else post.userName.lowercase().contains(searchText)

            val matchFilter = when (filterType) {
                FilterType.ALL -> true
                FilterType.MY_POSTS -> post.userId == currentUid
                FilterType.MY_LIKES -> post.likedBy.contains(currentUid)
            }

            // [QUAN TRỌNG] Kiểm tra xem User có nằm trong danh sách cần ẩn không
            // hiddenUserIds bao gồm cả: Người mình chặn VÀ Người chặn mình
            val isNotHidden = !hiddenUserIds.contains(post.userId)

            matchName && matchFilter && isNotHidden
        }
        adapter.submitList(filteredList)
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

    // --- ACTIONS ---
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
        db.collection("posts").document(postId)
            .update("shareCount", FieldValue.increment(1))
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

    private fun showFilterMenu(anchor: View, currentQuery: String) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "All Posts")
        popup.menu.add(0, 2, 0, "My Posts")
        popup.menu.add(0, 3, 0, "Liked by Me")

        popup.setOnMenuItemClickListener { item ->
            currentFilterType = when (item.itemId) {
                2 -> FilterType.MY_POSTS
                3 -> FilterType.MY_LIKES
                else -> FilterType.ALL
            }
            filterPosts(currentQuery, currentFilterType)
            val iconColor = if (currentFilterType == FilterType.ALL) R.color.black else R.color.orange_primary
            (anchor as ImageView).setColorFilter(resources.getColor(iconColor, null))
            true
        }
        popup.show()
    }
}