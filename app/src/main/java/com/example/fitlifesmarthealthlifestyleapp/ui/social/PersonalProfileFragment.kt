package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Post
import com.example.fitlifesmarthealthlifestyleapp.utils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class PersonalProfileFragment : Fragment(R.layout.fragment_personal_profile) {

    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var adapter: PostAdapter

    // Quản lý Listener
    private var userListener: ListenerRegistration? = null
    private var postsListener: ListenerRegistration? = null

    private var pendingSharePostId: String? = null
    private val shareLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        pendingSharePostId?.let { postId ->
            incrementShareCount(postId)
            pendingSharePostId = null
        }
    }

    // Hủy Listener khi thoát màn hình
    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
        postsListener?.remove()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Ánh xạ View
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val ivAvatar = view.findViewById<ImageView>(R.id.ivUserProfile)
        val tvName = view.findViewById<TextView>(R.id.tvUserName)
        val tvEmail = view.findViewById<TextView>(R.id.tvUserEmail)
        val tvDob = view.findViewById<TextView>(R.id.tvUserDob)
        val tvStatPostCount = view.findViewById<TextView>(R.id.tvStatPostCount)
        val tvStatFollowers = view.findViewById<TextView>(R.id.tvStatFollowers)
        val tvStatFollowing = view.findViewById<TextView>(R.id.tvStatFollowing)
        val layoutFollowers = view.findViewById<View>(R.id.layoutFollowers)
        val layoutFollowing = view.findViewById<View>(R.id.layoutFollowing)

        val btnBlockedList = view.findViewById<ImageView>(R.id.btnBlockedList)
        val rvMyPosts = view.findViewById<RecyclerView>(R.id.rvMyPosts)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyPosts)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBarProfile)

        // 2. Load Thông tin cá nhân (Gán vào Listener được quản lý)
        // [FIX] Chỉ gọi 1 lần và quản lý nó
        loadUserInfoAndStats(ivAvatar, tvName, tvEmail, tvDob, tvStatFollowers, tvStatFollowing, layoutFollowers, layoutFollowing)

        // 3. Adapter
        adapter = PostAdapter(
            onLikeClick = { post -> toggleLike(post) },
            onCommentClick = { post -> showCommentsDialog(post) },
            onShareClick = { post -> sharePostContent(post) },
            onLikeCountClick = { userIds -> showLikeListDialog(userIds) },
            onUserClick = { },
            onBlockClick = { _, _ -> },
            // [BỔ SUNG DÒNG NÀY ĐỂ FIX LỖI]
            onImageClick = { imageUrl ->
                FullScreenImageDialogFragment.show(parentFragmentManager, imageUrl)
            }
        )

        rvMyPosts.layoutManager = LinearLayoutManager(context)
        rvMyPosts.adapter = adapter

        // 4. Load bài viết
        loadMyPosts(progressBar, rvMyPosts, tvEmpty, tvStatPostCount)

        // 5. Events
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnBlockedList.setOnClickListener {
            if (NetworkUtils.checkConnection(requireContext())) {
                BlockedUsersDialogFragment().show(childFragmentManager, "BlockedUsersDialog")
            }
        }
    }

    private fun loadUserInfoAndStats(
        iv: ImageView, tvName: TextView, tvEmail: TextView, tvDob: TextView,
        tvFollowers: TextView, tvFollowing: TextView,
        layoutFollowers: View, layoutFollowing: View
    ) {
        if (currentUid == null) return

        // [FIX] Gán vào biến userListener để remove sau này
        userListener = db.collection("users").document(currentUid)
            .addSnapshotListener { doc, e ->
                if (e != null || doc == null || !isAdded) return@addSnapshotListener

                val name = doc.getString("displayName") ?: "User"
                val email = doc.getString("email") ?: ""
                val photoUrl = doc.getString("photoUrl")
                val timestamp = doc.getTimestamp("birthday")
                val followers = doc.get("followers") as? List<String> ?: emptyList()
                val following = doc.get("following") as? List<String> ?: emptyList()

                tvName.text = name
                tvEmail.text = email
                tvFollowers.text = followers.size.toString()
                tvFollowing.text = following.size.toString()

                Glide.with(this).load(photoUrl).placeholder(R.drawable.ic_user).circleCrop().into(iv)

                if (timestamp != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    tvDob.text = "Born: ${sdf.format(timestamp.toDate())}"
                }

                // Cập nhật sự kiện click cho layout Followers/Following với dữ liệu mới nhất
                layoutFollowers.setOnClickListener {
                    UserListDialogFragment("Followers", followers).show(childFragmentManager, "Followers")
                }
                layoutFollowing.setOnClickListener {
                    UserListDialogFragment("Following", following).show(childFragmentManager, "Following")
                }
            }
    }

    private fun loadMyPosts(progressBar: ProgressBar, rv: RecyclerView, tvEmpty: TextView, tvStatPost: TextView?) {
        if (currentUid == null) return

        // [FIX] Gán vào biến postsListener
        postsListener = db.collection("posts")
            .whereEqualTo("userId", currentUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                progressBar.visibility = View.GONE
                if (e != null) return@addSnapshotListener
                if (!isAdded) return@addSnapshotListener

                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                tvStatPost?.text = posts.size.toString()

                if (posts.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rv.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rv.visibility = View.VISIBLE
                    adapter.submitList(posts)
                }
            }
    }

    // ... (Giữ nguyên các hàm toggleLike, showCommentsDialog, share, incrementShareCount...)
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

    private fun sharePostContent(post: Post) {
        if (!NetworkUtils.checkConnection(requireContext())) return
        pendingSharePostId = post.postId
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "https://fit-life-app-dl.vercel.app/post/${post.postId}")
            type = "text/plain"
        }
        shareLauncher.launch(Intent.createChooser(sendIntent, "Share via"))
    }

    private fun incrementShareCount(postId: String) {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) return
        db.collection("posts").document(postId).update("shareCount", FieldValue.increment(1))
    }

    private fun showLikeListDialog(userIds: List<String>) {
        if (userIds.isNotEmpty()) {
            LikeListDialogFragment(userIds).show(childFragmentManager, "LikeListDialog")
        }
    }
}