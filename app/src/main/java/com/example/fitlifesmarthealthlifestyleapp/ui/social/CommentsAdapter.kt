package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.data.repository.UserRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Comment
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class CommentsAdapter(
    private val onReplyClick: (Comment) -> Unit,
    private val onCommentLongClick: (Comment) -> Unit,
    private val onUserClick: (String) -> Unit // [MỚI] Callback khi bấm vào user
) : ListAdapter<Comment, CommentsAdapter.CommentViewHolder>(CommentDiffCallback()) {
    private var currentUserName: String = ""
    private var currentUserAvatar: String = ""

    fun setCurrentUserInfo(name: String, avatar: String) {
        this.currentUserName = name
        this.currentUserAvatar = avatar
    }
    private val userRepository = UserRepository()
    private val userCache = mutableMapOf<String, User>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = getItem(position)
        holder.bind(comment)

        holder.itemView.setOnLongClickListener {
            onCommentLongClick(comment)
            true
        }
    }

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // View cơ bản
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivCommentAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvCommentUser)
        private val tvContent: TextView = itemView.findViewById(R.id.tvCommentContent)
        private val tvTime: TextView = itemView.findViewById(R.id.tvCommentTime)
        private val btnLike: ImageView = itemView.findViewById(R.id.btnLikeComment)
        private val tvLikes: TextView = itemView.findViewById(R.id.tvCommentLikes)
        private val tvReply: TextView = itemView.findViewById(R.id.tvReply)

        // View Media
        private val ivMedia: ImageView = itemView.findViewById(R.id.ivCommentMedia)
        private val layoutAudio: View = itemView.findViewById(R.id.layoutCommentAudio)

        private val db = FirebaseFirestore.getInstance()
        private val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        fun bind(comment: Comment) {
            tvContent.text = comment.content

            // [MỚI] Sự kiện Click Avatar & Tên -> Mở Profile
            val openProfile = View.OnClickListener { onUserClick(comment.userId) }
            ivAvatar.setOnClickListener(openProfile)
            tvName.setOnClickListener(openProfile)

            // Format time
            val timestamp = comment.timestamp
            if (timestamp != null) {
                val sdf = SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault())
                tvTime.text = sdf.format(timestamp.toDate())
            } else {
                tvTime.text = ""
            }

            // Hiển thị số like
            tvLikes.text = if (comment.likedBy.isNotEmpty()) comment.likedBy.size.toString() else ""

            // Xử lý hiển thị Media (Ảnh)
            ivMedia.visibility = View.GONE
            layoutAudio.visibility = View.GONE

            if (comment.mediaUrl != null && comment.mediaType != null) {
                when (comment.mediaType) {
                    "IMAGE" -> {
                        ivMedia.visibility = View.VISIBLE
                        Glide.with(itemView.context)
                            .load(comment.mediaUrl)
                            .placeholder(R.drawable.bg_search_rounded)
                            .into(ivMedia)

                        // Sự kiện Click xem ảnh Full Screen
                        ivMedia.setOnClickListener {
                            if (comment.mediaUrl == null) return@setOnClickListener
                            var context = itemView.context
                            while (context is android.content.ContextWrapper) {
                                if (context is androidx.appcompat.app.AppCompatActivity) {
                                    FullScreenImageDialogFragment.show(context.supportFragmentManager, comment.mediaUrl)
                                    return@setOnClickListener
                                }
                                context = context.baseContext
                            }
                        }
                    }
                }
            }

            // Logic Avatar Realtime
            val cachedUser = userCache[comment.userId]
            if (cachedUser != null) {
                applyUser(cachedUser)
            } else {
                tvName.text = comment.userName
                loadAvatar(comment.userAvatar)

                CoroutineScope(Dispatchers.Main).launch {
                    val result = withContext(Dispatchers.IO) {
                        userRepository.getUserDetails(comment.userId)
                    }
                    if (result.isSuccess) {
                        result.getOrNull()?.let { user ->
                            userCache[comment.userId] = user
                            applyUser(user)
                        }
                    }
                }
            }

            // Sự kiện Reply
            tvReply.setOnClickListener {
                onReplyClick(comment)
            }

            // Logic Like Comment
            val isLiked = comment.likedBy.contains(currentUid)
            if (isLiked) {
                btnLike.setImageResource(R.drawable.ic_heart_filled)
                btnLike.setColorFilter(ContextCompat.getColor(itemView.context, R.color.orange_primary))
            } else {
                btnLike.setImageResource(R.drawable.ic_heart_outline)
                btnLike.setColorFilter(ContextCompat.getColor(itemView.context, R.color.gray_text))
            }

            btnLike.setOnClickListener {
                if (currentUid == null) return@setOnClickListener
                val commentRef = db.collection("posts")
                    .document(comment.postId)
                    .collection("comments")
                    .document(comment.id)

                if (isLiked) {
                    commentRef.update("likedBy", FieldValue.arrayRemove(currentUid))
                } else {
                    commentRef.update("likedBy", FieldValue.arrayUnion(currentUid))

                    // [MỚI] Gửi thông báo "Tim bình luận"
                    if (comment.userId != currentUid) {
                        NotificationHelper.sendNotification(
                            context = itemView.context,
                            recipientId = comment.userId,
                            senderId = currentUid,
                            senderName = currentUserName,
                            senderAvatar = currentUserAvatar,
                            postId = comment.postId,
                            type = "LIKE_COMMENT",
                            content = comment.content // Truyền nội dung bình luận để hiển thị trong thông báo
                        )
                    }
                }
            }
        }

        private fun applyUser(user: User) {
            tvName.text = user.displayName
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
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean = oldItem == newItem
    }
}