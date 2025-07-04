package com.example.recordwithme.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SignUpScreen(navController: NavController) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) }
    var name by remember { mutableStateOf("") }
    var id by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordCheck by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, start = 32.dp, end = 32.dp, bottom = 32.dp)
        ) {
            // 상단 뒤로가기 버튼과 타이틀
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = {
                    when (step) {
                        1 -> navController.popBackStack() // 로그인 화면으로 이동
                        2 -> step = 1
                        3 -> step = 2
                    }
                }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기")
                }
                Spacer(modifier = Modifier.width(8.dp))
                val title = when (step) {
                    1 -> "사용자 이름 만들기"
                    2 -> "아이디 만들기"
                    3 -> "비밀번호 만들기"
                    else -> "회원가입"
                }
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            when (step) {
                1 -> {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("사용자 이름") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) step = 2
                            else Toast.makeText(context, "이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFC0CB)),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Text("다음", fontWeight = FontWeight.Bold)
                    }
                }
                2 -> {
                    OutlinedTextField(
                        value = id,
                        onValueChange = { id = it },
                        label = { Text("아이디") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            if (id.isNotBlank()) step = 3
                            else Toast.makeText(context, "아이디를 입력하세요.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFC0CB)),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Text("다음", fontWeight = FontWeight.Bold)
                    }
                }
                3 -> {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("비밀번호") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = passwordCheck,
                        onValueChange = { passwordCheck = it },
                        label = { Text("비밀번호 확인") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            if (password.isBlank() || passwordCheck.isBlank()) {
                                Toast.makeText(context, "비밀번호를 모두 입력하세요.", Toast.LENGTH_SHORT).show()
                            } else if (password != passwordCheck) {
                                Toast.makeText(context, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                // 1. Firestore에서 아이디(문서ID) 중복 체크
                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(id)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            Toast.makeText(context, "이미 사용 중인 아이디입니다.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // 2. 중복이 아니면 회원가입 진행
                                            val userInfo = hashMapOf(
                                                "name" to name,
                                                "id" to id,
                                                "password" to password
                                            )
                                            FirebaseFirestore.getInstance()
                                                .collection("users")
                                                .document(id)
                                                .set(userInfo)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "회원가입 완료!", Toast.LENGTH_SHORT).show()
                                                    navController.popBackStack("login", inclusive = false)
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(context, "회원정보 저장 실패", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "네트워크 오류: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFC0CB)),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Text("회원가입", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}