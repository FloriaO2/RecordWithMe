package com.example.recordwithme.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.recordwithme.BuildConfig
import com.example.recordwithme.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

// 사진 데이터 클래스
data class Comment(
    val userId: String = "",
    val text: String = ""
)

data class PhotoData(
    val url: String,
    val isBase64: Boolean,
    val description: String = "",
    val comments: List<Comment> = emptyList()
)

// PhotoAdapter 수정: 다중 뷰타입 지원
class PhotoAdapter(
    private val dateString: String,
    private val photoCount: Int,
    private val photos: List<PhotoData>,
    private val groupId: String,
    private val photoDocIds: List<String>,
    private val onRefresh: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val VIEW_TYPE_DATE = 0
        private const val VIEW_TYPE_COUNT = 1
        private const val VIEW_TYPE_PHOTO = 2
    }

    // 현재 재생 중인 MediaPlayer
    private var currentMediaPlayer: MediaPlayer? = null
    private var currentPlayingPosition: Int = -1

    // 날짜 뷰홀더
    class DateViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    // 사진 개수 뷰홀더
    class CountViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    // 사진 뷰홀더(기존)
    class PhotoViewHolder(
        val imageView: ImageView,
        val descView: TextView,
        val commentsView: LinearLayout,
        val commentInput: EditText,
        val commentButton: Button,
        val deleteButton: Button,
        val labelButton: Button,
        val musicOverlay: LinearLayout,
        val musicText: TextView,
        val playButton: ImageButton,
        itemView: View
    ) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_PHOTO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        // 기존 PhotoViewHolder 생성 코드
        val container = FrameLayout(context).apply {
            setBackgroundColor(Color.WHITE)
            val padding = 60 // 좌/우/아래 여백
            val topPadding = (4 * context.resources.displayMetrics.density).toInt() // 위쪽만 24dp
            setPadding(padding, topPadding, padding, padding)
            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 60 //여백2
            layoutParams = params
            background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.post_border)
        }

        // 내부 컨테이너 (기존 LinearLayout)
        val innerContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val imageView = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }

        // 음악 정보 오버레이 (처음에는 숨김)
        val musicText = TextView(context).apply {
            text = "🎵 이 순간과 어울리는 음악은 무엇일까요?"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 16, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) // weight 1f로 변경
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setShadowLayer(8f, 0f, 0f, Color.BLACK)
            maxWidth = (1000 * context.resources.displayMetrics.density).toInt() // 충분히 넓게 유지
        }
        val playButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_media_play)
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) // WRAP_CONTENT로 변경
            visibility = View.GONE // 처음엔 안 보이게
        }
        val musicOverlay = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.TRANSPARENT)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(24, 16, 24, 16)
            weightSum = 1f
            visibility = View.VISIBLE // 항상 보이게
            addView(musicText)
            addView(playButton)
        }

        val descView = TextView(context).apply {
            setTextColor(Color.DKGRAY)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }
        val divider = View(context).apply {
            setBackgroundColor(Color.DKGRAY) // 더 진한 회색
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                3 // 기존 2에서 3으로 두껍게
            )
        }
        val commentsView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
        }
        // 댓글 입력란과 등록 버튼을 가로로 배치
        val commentInputLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val commentInput = EditText(context).apply {
            hint = "댓글을 입력하세요"
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(24, 24, 24, 24)
        }
        val commentButton = Button(context).apply {
            text = "등록"
            textSize = 16f
            setBackgroundColor(Color.BLACK)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT // 입력란과 높이 맞춤
            ).apply {
                leftMargin = 8
            }
            background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.btn_comment_rounded)
            setPadding(32, 16, 32, 16)
        }
        commentInputLayout.addView(commentInput)
        commentInputLayout.addView(commentButton)
        val deleteButton = Button(context).apply {
            text = "게시물 삭제"
            textSize = 15f
            setTextColor(Color.WHITE)
            background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.btn_delete_rounded)
        }
        val buttonSpacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                12 // 등록-삭제 버튼과 동일하게 12px 높이로 변경
            )
        }
        val labelButton = Button(context).apply {
            text = "어울리는 음악 재생"
            textSize = 15f
            setTextColor(Color.WHITE)
            background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.btn_music_rounded)
        }
        val deleteParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        deleteParams.topMargin = 12
        deleteButton.layoutParams = deleteParams
        innerContainer.addView(musicOverlay)
        innerContainer.addView(imageView)
        innerContainer.addView(descView)
        innerContainer.addView(divider)
        // 구분선과 댓글 사이 16dp 여백 추가
        val dividerBottomSpace = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (12 * context.resources.displayMetrics.density).toInt() // 16dp
            )
        }
        innerContainer.addView(dividerBottomSpace)
        innerContainer.addView(commentsView)
        innerContainer.addView(commentInputLayout) // 수정: 입력란+버튼 가로 배치
        innerContainer.addView(deleteButton)
        innerContainer.addView(buttonSpacer)
        innerContainer.addView(labelButton)

        container.addView(innerContainer)

        return PhotoViewHolder(imageView, descView, commentsView, commentInput, commentButton, deleteButton, labelButton, musicOverlay, musicText, playButton, container)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val photoIdx = position
        val photo = photos[photoIdx]
        val photoHolder = holder as PhotoViewHolder
        // 항상 초기화
        photoHolder.musicText.text = "🎵 이 순간과 어울리는 음악은 무엇일까요?"
        photoHolder.playButton.visibility = View.GONE
        // 이하 기존 PhotoAdapter의 onBindViewHolder 내용에서 position -> photoIdx로 변경
        if (photo.isBase64) {
            try {
                val bytes = Base64.decode(photo.url, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                photoHolder.imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                photoHolder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } else if (photo.url.startsWith("https://")) {
            photoHolder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        photoHolder.descView.text = if (photo.description.isBlank()) "+ 설명을 추가해주세요." else photo.description
        photoHolder.descView.setTextColor(
            if (photo.description.isBlank()) Color.parseColor("#1976D2") else Color.DKGRAY
        )
        photoHolder.descView.setOnClickListener {
            val editText = EditText(photoHolder.descView.context).apply {
                setText(photo.description)
                hint = "설명을 입력하세요"
            }
            AlertDialog.Builder(photoHolder.descView.context)
                .setTitle(if (photo.description.isBlank()) "설명 추가" else "설명 수정")
                .setView(editText)
                .setPositiveButton("저장") { _, _ ->
                    val newDesc = editText.text.toString().trim()
                    if (newDesc.isNotEmpty() && newDesc != photo.description) {
                        val firestore = FirebaseFirestore.getInstance()
                        val photoId = photoDocIds[photoIdx]
                        val photoDocRef = firestore.collection("groups")
                            .document(groupId)
                            .collection("photos")
                            .document(photoId)
                        photoDocRef.update("description", newDesc)
                            .addOnSuccessListener { onRefresh() }
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        }
        photoHolder.commentsView.removeAllViews()
        val leftPadding = (11 * photoHolder.commentsView.context.resources.displayMetrics.density).toInt()
        if (photo.comments.isEmpty()) {
            val emptyView = TextView(photoHolder.commentsView.context).apply {
                text = "댓글이 없습니다"
                setTextColor(Color.LTGRAY)
                textSize = 18f
                setPadding(leftPadding, 10, 0, 8) // 왼쪽만 16dp 패딩
            }
            photoHolder.commentsView.addView(emptyView)
        } else {
            for (comment in photo.comments) {
                val commentView = TextView(photoHolder.commentsView.context).apply {
                    text = "${comment.userId} : ${comment.text}"
                    setTextColor(Color.BLACK)
                    textSize = 15f
                    setPadding(leftPadding, 10, 0, 8) // 왼쪽만 16dp 패딩
                }
                photoHolder.commentsView.addView(commentView)
            }
        }
        photoHolder.commentButton.setOnClickListener {
            val newCommentText = photoHolder.commentInput.text.toString().trim()
            if (newCommentText.isNotEmpty()) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val usersRef = FirebaseFirestore.getInstance().collection("users")
                    usersRef.document(uid).get().addOnSuccessListener { document ->
                        val userName = document.getString("name") ?: "익명"
                        val firestore = FirebaseFirestore.getInstance()
                        val photoId = photoDocIds[photoIdx]
                        val photoDocRef = firestore.collection("groups")
                            .document(groupId)
                            .collection("photos")
                            .document(photoId)
                        val commentMap = mapOf("userId" to userName, "text" to newCommentText)
                        photoDocRef.update("comments", com.google.firebase.firestore.FieldValue.arrayUnion(commentMap))
                            .addOnSuccessListener {
                                photoHolder.commentInput.setText("")
                                onRefresh()
                            }
                    }
                }
            }
        }
        val context = photoHolder.itemView.context
        val photoId = photoDocIds[photoIdx]
        photoHolder.deleteButton.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("사진 삭제")
                .setMessage("정말 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("groups")
                        .document(groupId)
                        .collection("photos")
                        .document(photoId)
                        .delete()
                        .addOnSuccessListener { onRefresh() }
                }
                .setNegativeButton("취소", null)
                .show()
        }
        // 라벨 추출 버튼 클릭 리스너
        photoHolder.labelButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                        photoHolder.musicOverlay.visibility = View.VISIBLE
                    }
                    val visionApiKey = BuildConfig.VISION_API_KEY
                    val labels = com.example.recordwithme.util.VisionApiHelper.getLabelsFromVisionApi(
                        photo.url, // Base64 데이터
                        visionApiKey
                    )
                    Log.d("SpotifyDebug", "Vision 라벨: $labels")
                    if (labels.isNotEmpty()) {
                        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                        val prefs = EncryptedSharedPreferences.create(
                            "spotify_prefs",
                            masterKeyAlias,
                            photoHolder.itemView.context,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        )
                        val accessToken = prefs.getString("access_token", null)
                        if (accessToken != null) {
                            val musicKeywords = listOf("k-pop", "Korean", "노래", "music")
                            val searchQuery = (labels.take(3) + musicKeywords).joinToString(" ")
                            val tracks = searchSpotifyTracks(searchQuery, accessToken, labels)
                            Log.d("SpotifyDebug", "검색 쿼리: $searchQuery, 트랙 수: ${tracks.size}")
                            if (tracks.isNotEmpty()) {
                                val playableTrack = tracks.firstOrNull { it.previewUrl != null }
                                if (playableTrack != null) {
                                    Log.d("SpotifyDebug", "첫 미리듣기 곡: ${playableTrack.name}, previewUrl: ${playableTrack.previewUrl}")
                                    (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                        val musicText = photoHolder.musicText
                                        musicText.text = "🎵 ${playableTrack.name} - ${playableTrack.artist}"
                                        val playButton = photoHolder.playButton
                                        playButton.setImageResource(android.R.drawable.ic_media_pause)
                                        playButton.visibility = View.VISIBLE
                                        playButton.setOnClickListener {
                                            togglePlayPause(playableTrack.previewUrl, photoHolder, photoIdx)
                                        }
                                        photoHolder.itemView.tag = playableTrack
                                        // 자동 재생
                                        playPreviewUrl(playableTrack.previewUrl, photoHolder, photoIdx, autoPlay = true)
                                    }
                                } else {
                                    // iTunes에서 미리듣기 URL 시도
                                    val mostPopularTrack = tracks.maxByOrNull { it.popularity }
                                    val secondPopularTrack = tracks.sortedByDescending { it.popularity }.getOrNull(1)

                                    if (mostPopularTrack != null) {
                                        Log.d("SpotifyDebug", "iTunes 검색용 곡 정보(인기순): name=${mostPopularTrack.name}, artist=${mostPopularTrack.artist}, popularity=${mostPopularTrack.popularity}")
                                        val itunesPreviewUrl = getItunesPreviewUrl(mostPopularTrack.name, mostPopularTrack.artist)
                                        if (itunesPreviewUrl != null) {
                                            Log.d("SpotifyDebug", "iTunes 미리듣기 URL: $itunesPreviewUrl")
                                            (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                                val musicText = photoHolder.musicText
                                                musicText.text = "🎵 ${mostPopularTrack.name} - ${mostPopularTrack.artist} (iTunes)"
                                                val playButton = photoHolder.playButton
                                                playButton.setImageResource(android.R.drawable.ic_media_pause)
                                                playButton.visibility = View.VISIBLE
                                                playButton.setOnClickListener {
                                                    togglePlayPause(itunesPreviewUrl, photoHolder, photoIdx)
                                                }
                                                photoHolder.itemView.tag = mostPopularTrack
                                                // 자동 재생
                                                playPreviewUrl(itunesPreviewUrl, photoHolder, photoIdx, autoPlay = true)
                                            }
                                        } else if (secondPopularTrack != null) {
                                            // 첫 번째 곡이 실패하면 두 번째 곡 시도
                                            Log.d("SpotifyDebug", "첫 번째 곡 실패, 두 번째 곡 시도: name=${secondPopularTrack.name}, artist=${secondPopularTrack.artist}, popularity=${secondPopularTrack.popularity}")
                                            val secondItunesPreviewUrl = getItunesPreviewUrl(secondPopularTrack.name, secondPopularTrack.artist)
                                            if (secondItunesPreviewUrl != null) {
                                                Log.d("SpotifyDebug", "두 번째 곡 iTunes 미리듣기 URL: $secondItunesPreviewUrl")
                                                (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                                    val musicText = photoHolder.musicText
                                                    musicText.text = "🎵 ${secondPopularTrack.name} - ${secondPopularTrack.artist} (iTunes)"
                                                    val playButton = photoHolder.playButton
                                                    playButton.setImageResource(android.R.drawable.ic_media_pause)
                                                    playButton.visibility = View.VISIBLE
                                                    playButton.setOnClickListener {
                                                        togglePlayPause(secondItunesPreviewUrl, photoHolder, photoIdx)
                                                    }
                                                    photoHolder.itemView.tag = secondPopularTrack
                                                    // 자동 재생
                                                    playPreviewUrl(secondItunesPreviewUrl, photoHolder, photoIdx, autoPlay = true)
                                                }
                                            } else {
                                                (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                                    photoHolder.musicOverlay.visibility = View.GONE
                                                    android.widget.Toast.makeText(
                                                        photoHolder.itemView.context,
                                                        "미리듣기 가능한 곡이 없습니다.",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        } else {
                                            (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                                photoHolder.musicOverlay.visibility = View.GONE
                                                android.widget.Toast.makeText(
                                                    photoHolder.itemView.context,
                                                    "미리듣기 가능한 곡이 없습니다.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    } else {
                                        (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                            photoHolder.musicOverlay.visibility = View.GONE
                                            android.widget.Toast.makeText(
                                                photoHolder.itemView.context,
                                                "미리듣기 가능한 곡이 없습니다.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            } else {
                                (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                    photoHolder.musicOverlay.visibility = View.GONE
                                    android.widget.Toast.makeText(
                                        photoHolder.itemView.context,
                                        "검색 결과가 없습니다.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else {
                            (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                photoHolder.musicOverlay.visibility = View.GONE
                                android.widget.Toast.makeText(
                                    photoHolder.itemView.context,
                                    "Spotify 로그인이 필요합니다.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                            photoHolder.musicOverlay.visibility = View.GONE
                            android.widget.Toast.makeText(
                                photoHolder.itemView.context,
                                "라벨을 추출할 수 없습니다.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                        photoHolder.musicOverlay.visibility = View.GONE
                        android.widget.Toast.makeText(
                            photoHolder.itemView.context,
                            "오류: ${e.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = photos.size

    // Preview URL 재생 함수
    private fun playPreviewUrl(previewUrl: String?, photoHolder: PhotoViewHolder, photoIdx: Int, autoPlay: Boolean = false) {
        Log.d("SpotifyDebug", "playPreviewUrl 진입, previewUrl: $previewUrl, autoPlay: $autoPlay")
        if (previewUrl == null) {
            // Preview URL이 없으면 Spotify 앱으로 이동
            val track = photoHolder.itemView.tag as? Track
            if (track != null) {
                val spotifyUri = "spotify:track:${track.id}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUri))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    photoHolder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        photoHolder.itemView.context,
                        "Spotify 앱이 설치되지 않았습니다.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
            return
        }

        // 이전 재생 중인 음악 정지
        stopCurrentMusic()

        try {
            currentMediaPlayer = MediaPlayer().apply {
                // 오디오 스트림 타입 설정 (미디어 볼륨)
                setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)

                // 볼륨 설정 (최대 볼륨의 80%)
                val audioManager = photoHolder.itemView.context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                val targetVolume = (maxVolume * 0.8).toInt()
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetVolume, 0)

                setDataSource(previewUrl)
                prepareAsync()
                setOnPreparedListener { player ->
                    Log.d("SpotifyDebug", "MediaPlayer 준비 완료, 재생 시작")
                    if (autoPlay) {
                        player.start()
                        currentPlayingPosition = photoIdx
                        val playButton = photoHolder.playButton
                        playButton.setImageResource(android.R.drawable.ic_media_pause)
                    }
                }
                setOnCompletionListener { player ->
                    // 재생 완료 시 재생 버튼으로 변경
                    val playButton = photoHolder.playButton
                    playButton.setImageResource(android.R.drawable.ic_media_play)
                    currentPlayingPosition = -1
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("SpotifyDebug", "MediaPlayer 에러 발생 what=$what, extra=$extra")
                    android.widget.Toast.makeText(
                        photoHolder.itemView.context,
                        "재생 중 오류가 발생했습니다. (코드: $what, $extra)",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    val playButton = photoHolder.playButton
                    playButton.setImageResource(android.R.drawable.ic_media_play)
                    currentPlayingPosition = -1
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("SpotifyDebug", "MediaPlayer 예외: ${e.message}")
            android.widget.Toast.makeText(
                photoHolder.itemView.context,
                "재생 준비 중 오류가 발생했습니다: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        Log.d("SpotifyPreview", "미리듣기 URL: $previewUrl")
    }

    // 현재 재생 중인 음악 정지
    private fun stopCurrentMusic() {
        currentMediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        currentMediaPlayer = null
        currentPlayingPosition = -1
    }

    // Adapter 소멸 시 MediaPlayer 정리
    fun cleanup() {
        stopCurrentMusic()
    }

    // 일시정지/이어듣기 토글 함수 (반드시 클래스 내부에 위치)
    private fun togglePlayPause(previewUrl: String?, photoHolder: PhotoViewHolder, photoIdx: Int) {
        if (currentMediaPlayer != null && currentMediaPlayer!!.isPlaying) {
            currentMediaPlayer?.pause()
            val playButton = photoHolder.playButton
            playButton.setImageResource(android.R.drawable.ic_media_play)
        } else if (currentMediaPlayer != null && !currentMediaPlayer!!.isPlaying) {
            currentMediaPlayer?.start()
            val playButton = photoHolder.playButton
            playButton.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            // 처음 재생
            playPreviewUrl(previewUrl, photoHolder, photoIdx, autoPlay = true)
        }
    }
}

class DayDetailActivity : AppCompatActivity() {
    private lateinit var galleryLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null
    private var groupId: String = ""
    private var year: Int = -1
    private var month: Int = -1
    private var day: Int = -1
    private var groupName: String = ""
    private lateinit var loadPhotosFunc: () -> Unit
    private var photoAdapter: PhotoAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 시스템 UI(상단바, 하단바) 숨기기
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        day = intent.getIntExtra("day", -1)
        year = intent.getIntExtra("year", -1)
        month = intent.getIntExtra("month", -1)
        groupId = intent.getStringExtra("groupId") ?: ""
        groupName = intent.getStringExtra("groupName") ?: ""
        val receivedTransitionName = intent.getStringExtra("transitionName") ?: ""

        // 디버깅을 위한 로그
        Log.d("DayDetailActivity", "받은 groupId: $groupId")
        Log.d("DayDetailActivity", "받은 groupName: $groupName")

        // 메인 레이아웃 생성
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(32, 64, 32, 64)

            // Shared Element Transition을 위한 transitionName 설정
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                transitionName = receivedTransitionName
            }
        }

        val groupNameText = TextView(this).apply {
            text = groupName
            textSize = 18f
            setTextColor(Color.parseColor("#1A237E")) // 남색
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 50, 0, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(groupNameText)

        // 날짜 텍스트
        val dateText = TextView(this).apply {
            text = "${year}년 ${month}월 ${day}일"
            textSize = 20f
            setTextColor(Color.parseColor("#212121")) // 남색
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(dateText)


        // 사진 개수 텍스트를 날짜 아래에 배치
        val photoCountText = TextView(this).apply {
            text = "이 날의 사진: 0장"
            textSize = 16f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(photoCountText)

        // 버튼들을 그 아래에 배치
        val buttonBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 16, 0, 32)
        }

        val backButton = Button(this).apply {
            text = "◀"
            textSize = 30f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 0, 0, 0)
            minHeight = 0
            minWidth = 0
            setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }

        val space = android.widget.Space(this)
        space.layoutParams = LinearLayout.LayoutParams(0, 0, 1f)

        val addButton = Button(this).apply {
            text = "+"
            textSize = 30f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 0, 0, 0)
            minHeight = 0
            minWidth = 0
            setOnClickListener { addPhoto() }
        }

        buttonBar.addView(backButton)
        buttonBar.addView(space)
        buttonBar.addView(addButton)
        layout.addView(buttonBar)

        // RecyclerView 생성
        val recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@DayDetailActivity)
            setBackgroundColor(Color.WHITE)
        }
        layout.addView(recyclerView)

        // ActivityResultLauncher 등록 (갤러리에서 사진 선택)
        galleryLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageUri = result.data!!.data
                if (imageUri != null) {
                    selectedImageUri = imageUri
                    uploadImageToFirebase(imageUri)
                }
            }
        }

        // 데이터 새로고침 함수 정의
        fun loadPhotos() {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val dateString = "${year}년 ${month}월 ${day}일"
                    val snapshot = firestore.collection("groups")
                        .document(groupId)
                        .collection("photos")
                        .whereEqualTo("date", dateString)
                        .orderBy("uploadedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .get()
                        .await()
                    val photoCount = snapshot.size()
                    // photoCountText 업데이트
                    photoCountText.text = "이 날의 사진: ${photoCount}장"

                    val photoList = mutableListOf<PhotoData>()
                    val photoDocIds = mutableListOf<String>()
                    for (document in snapshot.documents) {
                        val imageUrl = document.getString("url") ?: continue
                        val isBase64 = document.getBoolean("isBase64") ?: false
                        val description = document.getString("description") ?: ""
                        val commentsRaw = document.get("comments")
                        val comments: List<Comment> = when (commentsRaw) {
                            is List<*> -> {
                                commentsRaw.map {
                                    when (it) {
                                        is Map<*, *> -> {
                                            val userId = it["userId"]?.toString() ?: "익명"
                                            val text = it["text"]?.toString() ?: ""
                                            Comment(userId, text)
                                        }
                                        is String -> Comment("익명", it)
                                        else -> null
                                    }
                                }.filterNotNull()
                            }
                            else -> emptyList()
                        }
                        photoList.add(PhotoData(imageUrl, isBase64, description, comments))
                        photoDocIds.add(document.id)
                    }
                    recyclerView.adapter = PhotoAdapter(
                        dateString = "${groupName}\n${year}년 ${month}월 ${day}일",
                        photoCount = photoCount,
                        photos = photoList,
                        groupId = groupId,
                        photoDocIds = photoDocIds
                    ) {
                        loadPhotos()
                    }
                    photoAdapter = recyclerView.adapter as PhotoAdapter
                } catch (e: Exception) {
                    // photoCountText 업데이트 (에러 시)
                    photoCountText.text = "이 날의 사진: 0장"

                    recyclerView.adapter = PhotoAdapter(
                        dateString = "${groupName}\n${year}년 ${month}월 ${day}일",
                        photoCount = 0,
                        photos = emptyList(),
                        groupId = groupId,
                        photoDocIds = emptyList()
                    ) {
                        loadPhotos()
                    }
                    photoAdapter = recyclerView.adapter as PhotoAdapter
                    e.printStackTrace()
                }
            }
        }

        // 최초 데이터 로드
        if (groupId.isNotEmpty()) {
            loadPhotos()
        }

        loadPhotosFunc = { loadPhotos() }

        // 그룹 이름이 없으면 Firestore에서 조회
        if (groupName.isEmpty() && groupId.isNotEmpty()) {
            Log.d("DayDetailActivity", "Firestore에서 그룹 이름 조회 시작")
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("groups").document(groupId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        groupName = document.getString("name") ?: ""
                        Log.d("DayDetailActivity", "Firestore에서 조회한 groupName: $groupName")
                        // UI 업데이트
                        dateText.text = "${groupName}\n${year}년 ${month}월 ${day}일"
                        backButton.text = "◀ ${groupName} 화면으로 돌아가기"
                    } else {
                        Log.d("DayDetailActivity", "Firestore 문서가 존재하지 않음")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("DayDetailActivity", "Firestore 조회 실패: ${e.message}")
                }
        } else {
            Log.d("DayDetailActivity", "groupName이 이미 있음: $groupName")
        }

        setContentView(layout)

        // Shared Element Transition 설정 - 속도 조절 및 배경 유지
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val transition = android.transition.TransitionInflater.from(this)
                .inflateTransition(android.R.transition.move)

            // 애니메이션 지속 시간을 늘려서 더 부드럽게
            transition.duration = 800

            window.sharedElementEnterTransition = transition

            // 배경이 사라지지 않도록 설정
            window.allowEnterTransitionOverlap = false
            window.allowReturnTransitionOverlap = false
        }
    }

    // 사진 추가 함수 (갤러리에서 선택)
    fun addPhoto() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        galleryLauncher.launch(intent)
    }

    // Storage에 업로드 후 Firestore에 저장 → Base64로 Firestore에 직접 저장
    private fun uploadImageToFirebase(imageUri: Uri) {
        val context = this
        val dateString = "${year}년 ${month}월 ${day}일"
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                android.widget.Toast.makeText(context, "이미지 파일을 열 수 없습니다", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            val bytes = inputStream.readBytes()
            val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
            val firestore = FirebaseFirestore.getInstance()
            val photoData = hashMapOf(
                "url" to base64String,
                "isBase64" to true,
                "description" to "",
                "comments" to emptyList<String>(),
                "date" to dateString,
                "uploadedAt" to Timestamp.now()
            )
            firestore.collection("groups")
                .document(groupId)
                .collection("photos")
                .add(photoData)
                .addOnSuccessListener {
                    android.widget.Toast.makeText(context, "사진이 추가되었습니다", android.widget.Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    loadPhotosFunc()
                }
                .addOnFailureListener { e ->
                    android.widget.Toast.makeText(context, "Firestore 저장 실패: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "이미지 처리 실패: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        photoAdapter?.cleanup()
    }
}

// Spotify Track 데이터 클래스
data class Track(
    val id: String,
    val name: String,
    val artist: String,
    val album: String,
    val previewUrl: String?,
    val popularity: Int = 0
)

// Spotify API로 음악 검색하는 함수 (한국 음악 우선, 유명한 곡들)
private fun searchSpotifyTracks(query: String, accessToken: String, labels: List<String> = emptyList()): List<Track> {
    return try {
        // 한국 음악으로 검색 범위 제한 (장르: k-pop, korean, 한국어)
        val koreanQuery = "$query korean k-pop"
        val encodedQuery = java.net.URLEncoder.encode(koreanQuery, "UTF-8")
        val url = URL("https://api.spotify.com/v1/search?q=$encodedQuery&type=track&limit=10&market=US")

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json")

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d("SpotifyDebug", "Spotify API 응답: $response")
            val jsonObject = JSONObject(response)
            val tracksObject = jsonObject.getJSONObject("tracks")
            val itemsArray = tracksObject.getJSONArray("items")

            val tracks = mutableListOf<Track>()
            for (i in 0 until itemsArray.length()) {
                val trackObject = itemsArray.getJSONObject(i)
                val id = trackObject.getString("id")
                val name = trackObject.getString("name")
                val previewUrl = if (trackObject.has("preview_url") && !trackObject.isNull("preview_url")) {
                    trackObject.getString("preview_url")
                } else null

                val artist = try {
                    val artistsArray = trackObject.getJSONArray("artists")
                    if (artistsArray.length() > 0) {
                        artistsArray.getJSONObject(0).getString("name")
                    } else {
                        "Unknown Artist"
                    }
                } catch (e: Exception) {
                    "Unknown Artist"
                }

                val albumObject = trackObject.getJSONObject("album")
                val album = albumObject.getString("name")
                val popularity = trackObject.optInt("popularity", 0)

                tracks.add(Track(id, name, artist, album, previewUrl, popularity))
            }

            // 임영웅(영문/한글) 아티스트의 곡 제외
            val filteredTracks = tracks.filterNot {
                it.artist.contains("Lim Young Woong", ignoreCase = true) ||
                it.artist.contains("임영웅")
            }

            // 라벨 가중치 1.5배 적용
            filteredTracks.sortedByDescending { track ->
                val popularKeywords = listOf("방탄소년단", "BTS", "블랙핑크", "BLACKPINK", "아이유", "IU",
                    "세븐틴", "SEVENTEEN", "트와이스", "TWICE", "레드벨벳", "Red Velvet", "엑소", "EXO",
                    "뉴진스", "NewJeans", "르세라핌", "LE SSERAFIM", "아이브", "IVE", "스테이씨", "STAYC")
                val labelScore = labels.count { label ->
                    track.name.contains(label, ignoreCase = true) ||
                    track.artist.contains(label, ignoreCase = true)
                }
                val keywordScore = popularKeywords.count { keyword ->
                    track.name.contains(keyword, ignoreCase = true) ||
                    track.artist.contains(keyword, ignoreCase = true)
                }
                (labelScore * 1.5) + keywordScore + track.popularity
            }
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

// iTunes Search API로 미리듣기 URL을 받아오는 함수
private fun getItunesPreviewUrl(trackName: String, artist: String): String? {
    return try {
        val query = java.net.URLEncoder.encode("$trackName $artist", "UTF-8")
        val url = URL("https://itunes.apple.com/search?term=$query&entity=song&limit=1")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val results = json.getJSONArray("results")
            if (results.length() > 0) {
                val first = results.getJSONObject(0)
                return first.getString("previewUrl")
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}
