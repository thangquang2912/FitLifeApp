package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

class UserProfileFragment : Fragment(R.layout.fragment_user_profile) {

    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    private var targetUserId: String? = null

    private var btnFollow: Button? = null
    private lateinit var adapter: PostAdapter

    // Biến cờ trạng thái Follow (để xử lý Logic)
    private var isFollowingUser: Boolean = false

    // Views stats
    private var tvStatPostCount: TextView? = null
    private var tvStatFollowersCount: TextView? = null
    private var tvStatFollowingCount: TextView? = null

    // [QUAN TRỌNG] Quản lý 3 Listeners để cập nhật Realtime
    private var currentUserListener: ListenerRegistration? = null // Lắng nghe mình (để đổi màu nút Follow)
    private var targetUserListener: ListenerRegistration? = null  // Lắng nghe người kia (để nhảy số Follower)
    private var postsListener: ListenerRegistration? = null       // Lắng nghe bài viết (để nhảy số Post)

    private var pendingSharePostId: String? = null
    private val shareLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        pendingSharePostId?.let { postId ->
            db.collection("posts").document(postId).update("shareCount", FieldValue.increment(1))
            pendingSharePostId = null
        }
    }

    companion object {
        private const val ARG_USER_ID = "targetUserId"
        fun newInstance(userId: String): UserProfileFragment {
            val fragment = UserProfileFragment()
            val args = Bundle()
            args.putString(ARG_USER_ID, userId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { targetUserId = it.getString(ARG_USER_ID) }
    }

    // [QUAN TRỌNG] Hủy lắng nghe khi thoát để tránh Crash và tốn pin
    override fun onDestroyView() {
        super.onDestroyView()
        currentUserListener?.remove()
        targetUserListener?.remove()
        postsListener?.remove()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (targetUserId.isNullOrEmpty()) {
            parentFragmentManager.popBackStack()
            return
        }

        try {
            // 1. Ánh xạ View
            val ivAvatar = view.findViewById<ImageView>(R.id.ivOtherAvatar)
            val tvName = view.findViewById<TextView>(R.id.tvOtherName)
            val tvEmail = view.findViewById<TextView>(R.id.tvOtherEmail)
            val tvDob = view.findViewById<TextView>(R.id.tvOtherDob)

            tvStatPostCount = view.findViewById(R.id.tvStatPostCount)
            tvStatFollowersCount = view.findViewById(R.id.tvStatFollowersCount)
            tvStatFollowingCount = view.findViewById(R.id.tvStatFollowingCount)

            btnFollow = view.findViewById(R.id.btnFollow)
            val btnMessage = view.findViewById<View>(R.id.btnMessage)
            val btnBack = view.findViewById<ImageView>(R.id.btnBack)
            val rvPosts = view.findViewById<RecyclerView>(R.id.rvOtherPosts)

            // 2. Events
            btnBack.setOnClickListener {
                if (isAdded) parentFragmentManager.popBackStack()
            }

            btnMessage.setOnClickListener {
                Toast.makeText(context, "Chat feature coming soon!", Toast.LENGTH_SHORT).show()
            }

            btnFollow?.setOnClickListener { toggleFollow() }

            // 3. Setup RecyclerView
            setupRecyclerView(rvPosts)

            // 4. Load Data (Tất cả đều là Realtime)
            listenToTargetUserInfo(ivAvatar, tvName, tvEmail, tvDob) // <-- Hàm mới
            listenToCurrentUserFollowStatus()                        // <-- Hàm check follow
            listenToTargetUserPosts()                                // <-- Hàm load bài viết

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView(rv: RecyclerView) {
        adapter = PostAdapter(
            onLikeClick = { post -> toggleLike(post) },
            onCommentClick = { post -> showCommentsDialog(post) },
            onLikeCountClick = { userIds ->
                if (userIds.isNotEmpty()) LikeListDialogFragment(userIds).show(childFragmentManager, "Likes")
            },
            onUserClick = { },
            onShareClick = { post -> sharePost(post) },
            onBlockClick = { userId, userName -> showBlockConfirmation(userId, userName) }
        )
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = adapter
    }

    // --- 1. LẮNG NGHE THÔNG TIN NGƯỜI KHÁC (REALTIME) ---
    // Hàm này giúp số Followers/Following nhảy ngay lập tức
    private fun listenToTargetUserInfo(iv: ImageView, tvName: TextView, tvEmail: TextView, tvDob: TextView) {
        if (targetUserId == null) return

        targetUserListener = db.collection("users").document(targetUserId!!)
            .addSnapshotListener { doc, e ->
                if (e != null || doc == null || !isAdded) return@addSnapshotListener

                // Cập nhật thông tin cơ bản
                tvName.text = doc.getString("displayName") ?: "User"
                tvEmail.text = doc.getString("email") ?: ""
                val photoUrl = doc.getString("photoUrl")
                try {
                    Glide.with(this).load(photoUrl).placeholder(R.drawable.ic_user).circleCrop().into(iv)
                } catch (e: Exception) { }

                val timestamp = doc.getTimestamp("birthday")
                if (timestamp != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    tvDob.text = "Born: ${sdf.format(timestamp.toDate())}"
                }

                // [QUAN TRỌNG] Cập nhật số liệu thống kê Realtime
                val followers = doc.get("followers") as? List<String> ?: emptyList()
                val following = doc.get("following") as? List<String> ?: emptyList()

                tvStatFollowersCount?.text = followers.size.toString()
                tvStatFollowingCount?.text = following.size.toString()
            }
    }

    // --- 2. LẮNG NGHE TRẠNG THÁI FOLLOW CỦA MÌNH ---
    private fun listenToCurrentUserFollowStatus() {
        if (currentUid == null || targetUserId == null) return

        currentUserListener = db.collection("users").document(currentUid)
            .addSnapshotListener { doc, e ->
                if (e != null || doc == null || !isAdded || context == null) return@addSnapshotListener

                val following = doc.get("following") as? List<String> ?: emptyList()
                isFollowingUser = following.contains(targetUserId)

                // Cập nhật giao diện nút Follow
                if (isFollowingUser) {
                    btnFollow?.text = "Following"
                    btnFollow?.setBackgroundColor(Color.parseColor("#E0E0E0"))
                    btnFollow?.setTextColor(Color.BLACK)
                } else {
                    btnFollow?.text = "Follow"
                    btnFollow?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.orange_primary))
                    btnFollow?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
            }
    }

    // --- 3. LẮNG NGHE BÀI VIẾT (REALTIME) ---
    private fun listenToTargetUserPosts() {
        if (targetUserId == null) return
        postsListener = db.collection("posts")
            .whereEqualTo("userId", targetUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || !isAdded) return@addSnapshotListener
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()

                tvStatPostCount?.text = posts.size.toString()
                adapter.submitList(posts)
            }
    }

    // --- CÁC HÀM XỬ LÝ SỰ KIỆN ---
    private fun toggleFollow() {
        val ctx = context ?: return
        if (currentUid == null || targetUserId == null) return
        if (!NetworkUtils.checkConnection(ctx)) return

        btnFollow?.isEnabled = false // Khóa nút tạm thời

        val myRef = db.collection("users").document(currentUid)
        val targetRef = db.collection("users").document(targetUserId!!)
        val batch = db.batch()

        if (!isFollowingUser) {
            // Chưa follow -> Follow
            // Khi lệnh này chạy xong, Firestore thay đổi -> listenToTargetUserInfo tự động cập nhật số Follower lên +1
            batch.update(myRef, "following", FieldValue.arrayUnion(targetUserId))
            batch.update(targetRef, "followers", FieldValue.arrayUnion(currentUid))
        } else {
            // Đang follow -> Unfollow
            batch.update(myRef, "following", FieldValue.arrayRemove(targetUserId))
            batch.update(targetRef, "followers", FieldValue.arrayRemove(currentUid))
        }

        batch.commit().addOnCompleteListener {
            btnFollow?.isEnabled = true
        }
    }

    private fun showBlockConfirmation(userId: String, userName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Block User")
            .setMessage("Block $userName? This will also unfollow them.")
            .setPositiveButton("Block") { _, _ -> performBlockUserSafe() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performBlockUserSafe() {
        if (!isAdded || currentUid == null || targetUserId == null) return

        // Hủy listener trước khi Block để tránh xung đột
        currentUserListener?.remove()
        targetUserListener?.remove()
        postsListener?.remove()

        val batch = db.batch()
        val myRef = db.collection("users").document(currentUid)
        val targetRef = db.collection("users").document(targetUserId!!)

        batch.update(myRef, "following", FieldValue.arrayRemove(targetUserId))
        batch.update(targetRef, "followers", FieldValue.arrayRemove(currentUid))
        batch.update(myRef, "blockedUsers", FieldValue.arrayUnion(targetUserId))
        batch.update(targetRef, "blockedBy", FieldValue.arrayUnion(currentUid))

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(context, "User blocked", Toast.LENGTH_SHORT).show()
                if (isAdded) parentFragmentManager.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleLike(post: Post) {
        if (currentUid == null) return
        val ref = db.collection("posts").document(post.postId)
        if (post.likedBy.contains(currentUid)) {
            ref.update("likeCount", FieldValue.increment(-1), "likedBy", FieldValue.arrayRemove(currentUid))
        } else {
            ref.update("likeCount", FieldValue.increment(1), "likedBy", FieldValue.arrayUnion(currentUid))
        }
    }

    private fun showCommentsDialog(post: Post) {
        val dialog = CommentsDialogFragment()
        val bundle = Bundle()
        bundle.putString("postId", post.postId)
        dialog.arguments = bundle
        dialog.show(parentFragmentManager, "CommentsDialog")
    }

    private fun sharePost(post: Post) {
        pendingSharePostId = post.postId
        val deepLink = "https://fit-life-app-dl.vercel.app/post/${post.postId}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, "${post.caption}\n$deepLink")
            type = "text/plain"
        }
        shareLauncher.launch(Intent.createChooser(intent, "Share"))
    }
}