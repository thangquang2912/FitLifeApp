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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.android.material.bottomnavigation.BottomNavigationView // Import mới

class SocialFragment : Fragment(R.layout.fragment_social) {

    private lateinit var adapter: PostAdapter
    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    // Quản lý Listener
    private var userListener: ListenerRegistration? = null
    private var postsListener: ListenerRegistration? = null
    private var notificationBadgeListener: ListenerRegistration? = null

    // Dữ liệu & Filter
    private var allPosts: List<Post> = emptyList()
    private var currentFilterType = FilterType.ALL
    private var hiddenUserIds: MutableSet<String> = mutableSetOf()
    private var followingUserIds: MutableList<String> = mutableListOf()
    private var searchedUserIdByEmail: String? = null
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

    enum class FilterType { ALL, MY_POSTS, MY_LIKES, FOLLOWING }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
        postsListener?.remove()
        notificationBadgeListener?.remove()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchUserData()

        val rvCommunity = view.findViewById<RecyclerView>(R.id.rvCommunity)
        val searchView = view.findViewById<SearchView>(R.id.searchViewPost)
        val btnFilter = view.findViewById<ImageView>(R.id.btnFilter)
        val btnAddPost = view.findViewById<View>(R.id.btnAddPost)
        val btnUserProfile = view.findViewById<ImageView>(R.id.btnUserProfile)
        val btnNotification = view.findViewById<ImageView>(R.id.btnNotification)
        val ivUnreadBadge = view.findViewById<ImageView>(R.id.ivUnreadBadge) // Đảm bảo trong XML có ID này
        val btnMessage = view.findViewById<ImageView>(R.id.btnMessage)

        // --- SETUP ADAPTER (SỬA LỖI BIÊN DỊCH TẠI ĐÂY) ---
        adapter = PostAdapter(
            onLikeClick = { toggleLike(it) },
            onCommentClick = { showCommentsDialog(it) },
            onLikeCountClick = { showLikeListDialog(it) },
            onUserClick = { userId ->
                try {
                    val transaction = requireActivity().supportFragmentManager.beginTransaction()
                    if (userId == currentUid) {
                        transaction.replace(R.id.navHostFragmentContainerView, PersonalProfileFragment())
                    } else {
                        transaction.replace(R.id.navHostFragmentContainerView, UserProfileFragment.newInstance(userId))
                    }
                    transaction.addToBackStack(null)
                    transaction.commit()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            onShareClick = { sharePostContent(it) },
            onBlockClick = { userId, userName -> showBlockConfirmation(userId, userName) },

            // [MỚI] Thêm dòng này để fix lỗi "No value passed for parameter"
            onImageClick = { imageUrl ->
                FullScreenImageDialogFragment.show(parentFragmentManager, imageUrl)
            }
        )

        rvCommunity.layoutManager = LinearLayoutManager(context)
        rvCommunity.adapter = adapter

        // --- EVENTS ---
        btnAddPost.setOnClickListener {
            if (NetworkUtils.checkConnection(requireContext())) {
                CreatePostDialogFragment().show(parentFragmentManager, "CreatePost")
            }
        }

        btnFilter.setOnClickListener { showFilterMenu(it, searchView.query.toString()) }
        loadCurrentUserAvatar(btnUserProfile)

        btnUserProfile.setOnClickListener { currentUid?.let { openUserProfile(it) } }

        btnNotification.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.navHostFragmentContainerView, NotificationFragment())
                .addToBackStack(null)
                .commit()
        }

        if (ivUnreadBadge != null) {
            setupNotificationBadge(ivUnreadBadge)
        }

        btnMessage.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.navHostFragmentContainerView, CommunityChatFragment())
                .addToBackStack(null)
                .commit()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText?.trim() ?: ""

                // Kiểm tra nếu là định dạng Email (dùng Patterns.EMAIL_ADDRESS)
                if (query.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(query).matches()) {
                    searchUserByEmail(query) // Gọi hàm tìm UID
                } else {
                    searchedUserIdByEmail = null // Reset nếu không phải email
                    filterPosts(query, currentFilterType)
                }
                return true
            }
        })

        // ViewModel DeepLink
        deepLinkViewModel = ViewModelProvider(requireActivity())[DeepLinkViewModel::class.java]
        deepLinkViewModel.targetPostId.observe(viewLifecycleOwner) { id ->
            if (id != null) {
                targetPostId = id
                // [FIX] Kiểm tra nếu danh sách bài viết đã có dữ liệu thì thực hiện lọc ngay
                if (allPosts.isNotEmpty()) {
                    filterPostsWithDeepLink()
                }
            }
        }

        listenToPosts()
    }
    private fun searchUserByEmail(email: String) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Lấy UID của user đầu tiên khớp email
                    searchedUserIdByEmail = documents.documents[0].id
                } else {
                    searchedUserIdByEmail = "not_found" // Gán giá trị giả nếu không thấy
                }
                // Gọi lọc lại danh sách sau khi đã có kết quả UID
                filterPosts(email, currentFilterType)
            }
    }
    private fun setupNotificationBadge(badge: ImageView) {
        if (currentUid == null) return
        notificationBadgeListener = db.collection("users").document(currentUid)
            .collection("notifications")
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null || !isAdded) return@addSnapshotListener
                badge.visibility = if (!snapshots.isEmpty) View.VISIBLE else View.GONE
            }
    }

    private fun openUserProfile(userId: String) {
        try {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.navHostFragmentContainerView, PersonalProfileFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadCurrentUserAvatar(imageView: ImageView) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val url = document.getString("photoUrl")

                    if (!url.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(url)
                            .circleCrop()
                            .placeholder(R.drawable.ic_user_social)
                            .error(R.drawable.ic_user_social)
                            .into(imageView)
                    }
                }
            }
            .addOnFailureListener {
                Log.e("AvatarCheck", "Lỗi lấy dữ liệu user", it)
            }
    }

    // --- FETCH DATA ---
    private fun fetchUserData() {
        if (currentUid == null) return
        userListener = db.collection("users").document(currentUid)
            .addSnapshotListener { document, e ->
                if (e != null || document == null) return@addSnapshotListener

                hiddenUserIds.clear()
                val myBlockedList = document.get("blockedUsers") as? List<String>
                if (myBlockedList != null) hiddenUserIds.addAll(myBlockedList)
                val blockedMeList = document.get("blockedBy") as? List<String>
                if (blockedMeList != null) hiddenUserIds.addAll(blockedMeList)

                followingUserIds.clear()
                val myFollowing = document.get("following") as? List<String>
                if (myFollowing != null) followingUserIds.addAll(myFollowing)

                if (view != null) {
                    val searchView = view?.findViewById<SearchView>(R.id.searchViewPost)
                    filterPosts(searchView?.query.toString(), currentFilterType)
                }
            }
    }

    private fun listenToPosts() {
        postsListener = db.collection("posts")
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

    // --- LOGIC CŨ (GIỮ NGUYÊN) ---
    private fun showBlockConfirmation(userId: String, userName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Block User")
            .setMessage("Block $userName? This will also unfollow them.")
            .setPositiveButton("Block") { _, _ -> performBlockUser(userId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performBlockUser(targetUserId: String) {
        if (!NetworkUtils.checkConnection(requireContext()) || currentUid == null) return
        userListener?.remove()
        val batch = db.batch()
        val myRef = db.collection("users").document(currentUid)
        val targetRef = db.collection("users").document(targetUserId)
        batch.update(myRef, "blockedUsers", FieldValue.arrayUnion(targetUserId))
        batch.update(targetRef, "blockedBy", FieldValue.arrayUnion(currentUid))
        batch.update(myRef, "following", FieldValue.arrayRemove(targetUserId))
        batch.update(targetRef, "followers", FieldValue.arrayRemove(currentUid))
        batch.commit().addOnSuccessListener {
            if (isAdded) {
                Toast.makeText(context, "Blocked", Toast.LENGTH_SHORT).show()
                fetchUserData()
            }
        }
    }

    private fun filterPosts(query: String?, filterType: FilterType) {
        val searchText = query?.lowercase()?.trim() ?: ""
        val filteredList = allPosts.filter { post ->

            // LOGIC THAY ĐỔI TẠI ĐÂY:
            val matchText = if (searchText.isEmpty()) {
                true
            } else if (searchedUserIdByEmail != null) {
                // Nếu đã tìm thấy UID từ email, chỉ hiện bài viết của UID đó
                post.userId == searchedUserIdByEmail
            } else {
                // Ngược lại, tìm theo tên hoặc nội dung bài viết như cũ
                post.userName.lowercase().contains(searchText) || post.caption.lowercase().contains(searchText)
            }

            val matchFilter = when (filterType) {
                FilterType.ALL -> true
                FilterType.MY_POSTS -> post.userId == currentUid
                FilterType.MY_LIKES -> post.likedBy.contains(currentUid)
                FilterType.FOLLOWING -> followingUserIds.contains(post.userId)
            }
            matchText && matchFilter && !hiddenUserIds.contains(post.userId)
        }
        adapter.submitList(filteredList)
    }

    private fun showFilterMenu(anchor: View, currentQuery: String) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "All Posts")
        popup.menu.add(0, 2, 0, "My Posts")
        popup.menu.add(0, 3, 0, "Liked by Me")
        popup.menu.add(0, 4, 0, "Following")
        popup.setOnMenuItemClickListener { item ->
            currentFilterType = when (item.itemId) {
                2 -> FilterType.MY_POSTS
                3 -> FilterType.MY_LIKES
                4 -> FilterType.FOLLOWING
                else -> FilterType.ALL
            }
            filterPosts(currentQuery, currentFilterType)
            val iconColor = if (currentFilterType == FilterType.ALL) R.color.black else R.color.orange_primary
            if (anchor is ImageView) anchor.setColorFilter(resources.getColor(iconColor, null))
            true
        }
        popup.show()
    }

    private fun filterPostsWithDeepLink() {
        val id = targetPostId ?: return

        // Tìm vị trí của bài viết trong danh sách tổng
        val index = allPosts.indexOfFirst { it.postId == id }

        if (index != -1) {
            val rvCommunity = view?.findViewById<RecyclerView>(R.id.rvCommunity)

            // Cập nhật lại toàn bộ danh sách (hoặc giữ nguyên filter hiện tại)
            adapter.submitList(allPosts) {
                // [QUAN TRỌNG] Cuộn đến vị trí bài viết sau khi dữ liệu đã được nạp vào Adapter
                rvCommunity?.post {
                    // Cuộn bài viết lên đầu màn hình
                    (rvCommunity.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(index, 0)

                    // Sau khi cuộn xong thì xóa ID để tránh bị cuộn lại lần sau
                    deepLinkViewModel.clearPostId()
                    targetPostId = null
                }
            }
        } else {
            // Nếu không tìm thấy bài viết (có thể do đã bị xóa hoặc bị Block)
            deepLinkViewModel.clearPostId()
            targetPostId = null
            Toast.makeText(context, "Post no longer exists", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePostContent(post: Post) {
        pendingSharePostId = post.postId
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, "https://fit-life-app-dl.vercel.app/post/${post.postId}")
            type = "text/plain"
        }
        shareLauncher.launch(Intent.createChooser(intent, "Share via"))
    }

    private fun incrementShareCount(postId: String) {
        db.collection("posts").document(postId).update("shareCount", FieldValue.increment(1))
    }

    private fun toggleLike(post: Post) {
        if (currentUid == null) return
        val postRef = db.collection("posts").document(post.postId)

        if (post.likedBy.contains(currentUid)) {
            // Unlike
            postRef.update("likeCount", FieldValue.increment(-1), "likedBy", FieldValue.arrayRemove(currentUid))
        } else {
            // [FIX] Like và Gửi thông báo
            postRef.update("likeCount", FieldValue.increment(1), "likedBy", FieldValue.arrayUnion(currentUid))
                .addOnSuccessListener {
                    // Lấy info của mình để gửi sang cho người kia biết ai like
                    db.collection("users").document(currentUid).get().addOnSuccessListener { myDoc ->
                        NotificationHelper.sendNotification(
                            context = requireContext(),
                            recipientId = post.userId,
                            senderId = currentUid,
                            senderName = myDoc.getString("displayName") ?: "Someone",
                            senderAvatar = myDoc.getString("photoUrl") ?: "",
                            postId = post.postId,
                            type = "LIKE"
                        )
                    }
                }
        }
    }
    private fun showCommentsDialog(post: Post) {
        val dialog = CommentsDialogFragment()
        val bundle = Bundle()
        bundle.putString("postId", post.postId)
        dialog.arguments = bundle
        dialog.show(parentFragmentManager, "CommentsDialog")
    }

    private fun showLikeListDialog(userIds: List<String>) {
        if (userIds.isNotEmpty()) {
            val dialog = LikeListDialogFragment(userIds)
            dialog.show(childFragmentManager, "LikeListDialog")
        }
    }
}