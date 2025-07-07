package com.example.recordwithme.ui

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.example.recordwithme.R
import java.util.Calendar
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.drawable.Drawable
import com.google.firebase.firestore.FirebaseFirestore
import android.graphics.drawable.ColorDrawable
import android.util.Base64
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity as AndroidGravity
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import android.widget.FrameLayout
import android.util.TypedValue
import android.widget.ImageView

class GroupCalendarActivity : AppCompatActivity() {
    private var year = 2024 
    private var month = 7 // 1~12
    private var groupId: String = ""
    private var groupName: String = ""
    private lateinit var dayDetailLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_calendar)

        // Intent에서 그룹 정보 가져오기
        groupId = intent.getStringExtra("groupId") ?: ""
        groupName = intent.getStringExtra("groupName") ?: ""

        val textMonth = findViewById<TextView>(R.id.textMonth)
        val layoutYearMonth = findViewById<LinearLayout>(R.id.layoutYearMonth)
        val grid = findViewById<GridLayout>(R.id.gridCalendar)

        // 연/월 변경 버튼 + 연/월 표시 (상단 오른쪽)
        layoutYearMonth.removeAllViews()
        layoutYearMonth.orientation = LinearLayout.HORIZONTAL
        layoutYearMonth.gravity = Gravity.CENTER_VERTICAL

        val tvPrev = TextView(this).apply {
            text = "◀"
            textSize = 28f
            setTextColor(Color.BLACK)
            setPadding(16, 0, 16, 0)
            gravity = Gravity.CENTER
        }
        val tvYearMonth = TextView(this).apply {
            textSize = 32f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(8, 0, 8, 0)
        }
        val tvNext = TextView(this).apply {
            text = "▶"
            textSize = 28f
            setTextColor(Color.BLACK)
            setPadding(16, 0, 16, 0)
            gravity = Gravity.CENTER
        }
        layoutYearMonth.addView(tvPrev)
        layoutYearMonth.addView(tvYearMonth)
        layoutYearMonth.addView(tvNext)

        fun updateCalendar() {
            // 상단 표시
            textMonth.text = month.toString()
            tvYearMonth.text = "$year / $month"

            // 날짜 그리드 생성
            grid.removeAllViews()
            grid.rowCount = 6
            grid.columnCount = 7

            // 달력 계산
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, 1)
            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1:일~7:토
            val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val startOffset = (firstDayOfWeek + 5) % 7 // 월요일 시작
            val days = mutableListOf<Int?>()
            repeat(startOffset) { days.add(null) }
            for (i in 1..lastDay) days.add(i)
            while (days.size < 42) days.add(null)

            val firestore = FirebaseFirestore.getInstance()

            // 날짜 셀 추가
            for ((index, d) in days.withIndex()) {
                val row = index / 7
                val col = index % 7
                val cell = FrameLayout(this)
                val imageView = ImageView(this).apply {
                    val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80f, resources.displayMetrics).toInt()
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        px,
                        Gravity.CENTER
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundColor(if (d == null) Color.TRANSPARENT else Color.WHITE)
                    alpha = 0.8f
                }
                val textView = TextView(this).apply {
                    text = d?.toString() ?: ""
                    gravity = Gravity.CENTER
                    textSize = 20f
                    setTextColor(Color.BLACK)
                    setPadding(0, 0, 0, 0)
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
                cell.addView(imageView)
                cell.addView(textView)
                val params = GridLayout.LayoutParams()
                params.width = 0
                params.height = 0
                params.columnSpec = GridLayout.spec(col, 1f)
                params.rowSpec = GridLayout.spec(row, 0.5f)
                params.setMargins(4, 4, 4, 4)
                cell.layoutParams = params
                
                // 날짜 셀 클릭 시 Shared Element Transition으로 DayDetailActivity 전환
                if (d != null) {
                    // transitionName 설정
                    textView.transitionName = "calendar_cell_${year}_${month}_$d"

                    // Firestore에서 해당 날짜의 최신 사진 1장만 쿼리 (DayDetailActivity와 동일)
                    val dateString = "${year}년 ${month}월 ${d}일"
                    firestore.collection("groups").document(groupId)
                        .collection("photos")
                        .whereEqualTo("date", dateString)
                        .orderBy("uploadedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            val doc = querySnapshot.documents.firstOrNull()
                            if (doc != null) {
                                val isBase64 = doc.getBoolean("isBase64") ?: false
                                val imageUrl = doc.getString("url")
                                if (!imageUrl.isNullOrEmpty()) {
                                    if (isBase64) {
                                        try {
                                            val imageBytes = Base64.decode(imageUrl, Base64.DEFAULT)
                                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                            imageView.setImageBitmap(bitmap)
                                        } catch (e: Exception) {
                                            imageView.setImageDrawable(null)
                                            imageView.setBackgroundColor(Color.WHITE)
                                        }
                                    } else {
                                        Glide.with(this)
                                            .load(imageUrl)
                                            .centerCrop()
                                            .into(imageView)
                                    }
                                } else {
                                    imageView.setImageDrawable(null)
                                    imageView.setBackgroundColor(Color.WHITE)
                                }
                            } else {
                                imageView.setImageDrawable(null)
                                imageView.setBackgroundColor(Color.WHITE)
                            }
                        }
                        .addOnFailureListener {
                            imageView.setImageDrawable(null)
                            imageView.setBackgroundColor(Color.WHITE)
                        }

                    cell.setOnClickListener {
                        val intent = Intent(this, DayDetailActivity::class.java)
                        intent.putExtra("day", d)
                        intent.putExtra("year", year)
                        intent.putExtra("month", month)
                        intent.putExtra("groupId", groupId)
                        intent.putExtra("groupName", groupName)
                        intent.putExtra("transitionName", "calendar_cell_${year}_${month}_$d")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                this,
                                textView,
                                "calendar_cell_${year}_${month}_$d"
                            )
                            dayDetailLauncher.launch(intent, options)
                            window.allowEnterTransitionOverlap = false
                            window.allowReturnTransitionOverlap = false
                        } else {
                            dayDetailLauncher.launch(intent)
                        }
                    }
                }
                grid.addView(cell)
            }
        }

        // 이전/다음 버튼 동작
        tvPrev.setOnClickListener {
            if (month == 1) {
                if (year > 2024) {
                    year--
                    month = 12
                }
            } else {
                month--
            }
            updateCalendar()
        }
        tvNext.setOnClickListener {
            if (month == 12) {
                if (year < 2027) {
                    year++
                    month = 1
                }
            } else {
                month++
            }
            updateCalendar()
        }

        // 뒤로가기 버튼 클릭 리스너 연결
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        dayDetailLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                updateCalendar()
            }
        }

        updateCalendar()
    }
} 