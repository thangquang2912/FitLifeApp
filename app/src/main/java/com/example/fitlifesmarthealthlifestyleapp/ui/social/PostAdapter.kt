package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.data.repository.UserRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Post
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class PostAdapter(
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onLikeCountClick: (List<String>) -> Unit,
    private val onUserClick: (String) -> Unit,
    private val onShareClick: (Post) -> Unit,
    private val onBlockClick: (String, String) -> Unit // Callback chặn người dùng
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    // Cache để tránh load lại thông tin user liên tục
    private val userCache = mutableMapOf<String, User>()
    private val userRepository = UserRepository()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // --- ÁNH XẠ VIEW ---
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvTime: TextView = itemView.findViewById(R.id.tvPostTime)
        private val ivPostImage: ImageView = itemView.findViewById(R.id.ivPostImage)
        private val tvCaption: TextView = itemView.findViewById(R.id.tvCaption)
        private val tvStats: TextView = itemView.findViewById(R.id.tvPostStats)

        // Like
        private val tvLikes: TextView = itemView.findViewById(R.id.tvLikeCount)
        private val btnLike: ImageView = itemView.findViewById(R.id.btnLike)

        // Comment
        private val btnComment: ImageView = itemView.findViewById(R.id.btnComment)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)

        // Share
        private val btnShare: ImageView = itemView.findViewById(R.id.btnShare)
        private val tvShareCount: TextView = itemView.findViewById(R.id.tvShareCount)

        // Menu More
        private val ivMore: ImageView = itemView.findViewById(R.id.ivMore)

        // Firebase & Logic
        private val db = FirebaseFirestore.getInstance()
        private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        private var lastClickTime: Long = 0

        fun bind(post: Post) {
            // 1. TEXT DATA
            tvCaption.text = post.caption
            tvLikes.text = if (post.likeCount > 0) "${post.likeCount}" else ""
            tvCommentCount.text = if (post.commentCount > 0) "${post.commentCount}" else ""

            // 2. STATS (Calo & Time)
            if (post.duration.isNotEmpty() && post.duration != "0 mins") {
                tvStats.visibility = View.VISIBLE
                val calStr = if (post.calories.isNotEmpty() && post.calories != "0 kcal") " • 🔥 ${post.calories}" else ""
                tvStats.text = "⏱️ ${post.duration}$calStr"
            } else if (post.calories.isNotEmpty() && post.calories != "0 kcal") {
                tvStats.visibility = View.VISIBLE
                tvStats.text = "🔥 ${post.calories}"
            } else {
                tvStats.visibility = View.GONE
            }

            // 3. THỜI GIAN ĐĂNG
            // Kiểm tra null safety cho timestamp
            if (post.createdAt != null) {
                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                tvTime.text = sdf.format(post.createdAt.toDate())
            } else {
                tvTime.text = "Just now"
            }

            // 4. ẢNH BÀI VIẾT & FULLSCREEN
            Glide.with(itemView.context)
                .load(post.postImageUrl)
                .centerCrop()
                .placeholder(R.drawable.bg_search_rounded)
                .into(ivPostImage)

            ivPostImage.setOnClickListener {
                // Mở ảnh Fullscreen
                val context = itemView.context
                if (context is AppCompatActivity) {
                    FullScreenImageDialogFragment.show(context.supportFragmentManager, post.postImageUrl)
                }
            }

            // 5. USER INFO (REALTIME)
            loadUserRealtime(post)

            // Click Avatar/Tên -> Vào trang cá nhân
            val openProfile = View.OnClickListener { onUserClick(post.userId) }
            ivAvatar.setOnClickListener(openProfile)
            tvUserName.setOnClickListener(openProfile)

            // 6. XỬ LÝ LIKE
            val isLiked = post.likedBy.contains(currentUid)
            updateLikeUI(isLiked)

            btnLike.setOnClickListener { onLikeClick(post) }

            // Click vào số Like -> Xem danh sách người like
            tvLikes.setOnClickListener {
                if (post.likedBy.isNotEmpty()) {
                    onLikeCountClick(post.likedBy)
                }
            }

            // 7. XỬ LÝ COMMENT
            btnComment.setOnClickListener { onCommentClick(post) }

            // 8. XỬ LÝ SHARE (Có Debounce chống spam click)
            if (post.shareCount > 0) {
                tvShareCount.visibility = View.VISIBLE
                tvShareCount.text = "${post.shareCount}"
            } else {
                tvShareCount.visibility = View.GONE
            }

            btnShare.setOnClickListener {
                if (android.os.SystemClock.elapsedRealtime() - lastClickTime < 1000) {
                    return@setOnClickListener
                }
                lastClickTime = android.os.SystemClock.elapsedRealtime()
                onShareClick(post)
            }

            // 9. MENU MORE (Edit/Delete/Block)
            ivMore.visibility = View.VISIBLE
            ivMore.setOnClickListener { showOptionsMenu(it, post) }
        }

        private fun loadUserRealtime(post: Post) {
            val cachedUser = userCache[post.userId]
            if (cachedUser != null) {
                applyUserToUI(cachedUser)
            } else {
                // Hiển thị tạm thông tin cũ trong Post object
                tvUserName.text = post.userName
                loadAvatar(post.userAvatar)

                // Fetch mới nhất từ Firestore
                CoroutineScope(Dispatchers.Main).launch {
                    val result = withContext(Dispatchers.IO) { userRepository.getUserDetails(post.userId) }
                    result.getOrNull()?.let { user ->
                        userCache[post.userId] = user
                        applyUserToUI(user)
                    }
                }
            }
        }

        private fun applyUserToUI(user: User) {
            tvUserName.text = user.displayName
            loadAvatar(user.photoUrl)
        }

        private fun loadAvatar(url: String?) {
            Glide.with(itemView.context)
                .load(url)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .circleCrop()
                .into(ivAvatar)
        }

        private fun updateLikeUI(isLiked: Boolean) {
            if (isLiked) {
                btnLike.setImageResource(R.drawable.ic_heart_filled)
                btnLike.setColorFilter(ContextCompat.getColor(itemView.context, R.color.orange_primary))
            } else {
                btnLike.setImageResource(R.drawable.ic_heart_outline)
                btnLike.setColorFilter(ContextCompat.getColor(itemView.context, R.color.gray_text))
            }
        }

        private fun showOptionsMenu(view: View, post: Post) {
            val popup = PopupMenu(view.context, view)

            if (post.userId == currentUid) {
                // --- Bài của mình: Edit & Delete ---
                popup.menu.add(0, 1, 0, "Edit Post")
                popup.menu.add(0, 2, 0, "Delete Post")
            } else {
                // --- Bài người khác: Block ---
                popup.menu.add(0, 3, 0, "Block ${post.userName}")
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> { // EDIT
                        val context = itemView.context
                        if (context is AppCompatActivity) {
                            val editDialog = EditPostDialogFragment()
                            val bundle = Bundle().apply {
                                putString("postId", post.postId)
                                putString("caption", post.caption)
                                putString("duration", post.duration)
                                putString("calories", post.calories)
                                putString("imageUrl", post.postImageUrl)
                            }
                            editDialog.arguments = bundle
                            editDialog.show(context.supportFragmentManager, "EditPostDialog")
                        }
                        true
                    }
                    2 -> { // DELETE
                        db.collection("posts").document(post.postId).delete()
                            .addOnSuccessListener {
                                Toast.makeText(view.context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(view.context, "Failed to delete post", Toast.LENGTH_SHORT).show()
                            }
                        true
                    }
                    3 -> { // BLOCK
                        onBlockClick(post.userId, post.userName)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem.postId == newItem.postId
        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem
    }
}